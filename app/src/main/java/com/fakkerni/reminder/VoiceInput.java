package com.fakkerni.reminder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.*;

/** Hold-to-speak on the assistant screen; drafts are always reviewed before saving. */
final class VoiceInput {
    static final int MICROPHONE_REQUEST = 62;
    interface Listener { void onDraft(VoiceDraft draft); }
    private final Activity activity;
    private final Listener listener;
    private final MorphIconView record, cancel;
    private final TextView status;
    private AssistantOrb orb;
    private LinearLayout immersiveControls;
    private TextView holdHint;
    private boolean holdActive,immersive;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private MediaRecorder recorder;
    private File audio;
    private long started;
    private ZonedDateTime recordedAt;
    private boolean busy, destroyed, completed;
    private int generation;
    private Future<?> pending;
    private OpenAiClient client;
    private AiProvider recordingProvider;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (recorder == null) return;
            int seconds = (int) ((SystemClock.elapsedRealtime() - started) / 1000);
            if(orb!=null){try{orb.amplitude(Math.min(1f,recorder.getMaxAmplitude()/12000f));}catch(RuntimeException ignored){}}
            status.setText(String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60) + " · "
                    + Ui.t(activity,immersive?"سيب الزر للتحويل":"اضغط للإيقاف",immersive?"Release to transcribe":"Tap to stop"));
            if (seconds >= 60) stopRecording(true); else handler.postDelayed(this, 100);
        }
    };
    VoiceInput(Activity activity, LinearLayout panel, Listener listener) {
        this(activity,panel,listener,false);
    }
    VoiceInput(Activity activity, LinearLayout panel, Listener listener, boolean immersive) {
        this.activity = activity; this.listener = listener;
        this.immersive=immersive;
        panel.setOrientation(LinearLayout.HORIZONTAL);panel.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int p=Ui.dp(activity,16);panel.setPadding(p,p,p,p);
        panel.setBackground(new Ui.Pattern(Ui.INK,0x125F6B75,1,activity));
        record=Ui.icon(activity,"mic",Ui.YELLOW,Ui.t(activity,"تسجيل تذكير","Record reminder"),72);
        record.setId(View.generateViewId());panel.addView(record);
        LinearLayout labels=Ui.column(activity);labels.setPadding(p,0,p,0);
        labels.addView(Ui.text(activity,Ui.t(activity,"قولها، ونكتبها لك","Say it. We’ll write it."),16,Ui.WHITE));
        status=Ui.text(activity,Ui.t(activity,"اضغط وسجّل تذكيرك","Tap to record a reminder"),11,Ui.SECONDARY);
        Ui.add(labels,status,4);panel.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        cancel=Ui.icon(activity,"close",Ui.PAPER,Ui.t(activity,"إلغاء التسجيل","Discard recording"),48);
        panel.addView(cancel);cancel.setOnClickListener(v->cancel());
        record.setOnClickListener(v->{
            if(recorder!=null)stopRecording(true);
            else if(audio!=null)upload(); else requestRecording();
        });
        if(immersive){
            panel.removeAllViews();panel.setOrientation(LinearLayout.VERTICAL);panel.setGravity(android.view.Gravity.CENTER);
            panel.setPadding(0,0,0,0);panel.setBackground(null);
            orb=new AssistantOrb(activity);
            panel.addView(orb,new LinearLayout.LayoutParams(-1,Ui.dp(activity,310)));
            ((LinearLayout)status.getParent()).removeView(status);status.setTextSize(15);status.setGravity(android.view.Gravity.CENTER);
            status.setTextColor(Ui.SECONDARY);Ui.add(panel,status,0);
            LinearLayout controls=Ui.row(activity);controls.setGravity(android.view.Gravity.CENTER);immersiveControls=controls;
            record.setBackground(Ui.rounded(Ui.YELLOW,44,activity));record.setInk(Ui.BG);
            controls.addView(record,new LinearLayout.LayoutParams(Ui.dp(activity,78),Ui.dp(activity,78)));
            controls.addView(cancel,new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48)));
            ((LinearLayout.LayoutParams)cancel.getLayoutParams()).setMarginStart(Ui.dp(activity,12));
            Ui.add(panel,controls,20);
            TextView hold=Ui.text(activity,Ui.t(activity,"اضغط مطوّلًا للكلام · سيب الزر للإرسال","Hold to speak · release to send"),12,Ui.SECONDARY);
            hold.setGravity(android.view.Gravity.CENTER);holdHint=hold;Ui.add(panel,hold,10);
            record.setOnClickListener(v->{if(audio!=null)upload();else if(recorder==null)requestRecording();else stopRecording(true);});
            record.setOnTouchListener((v,event)->{
                switch(event.getActionMasked()){
                    case android.view.MotionEvent.ACTION_DOWN:
                        if(busy)return true;
                        holdActive=true;v.setPressed(true);
                        if(audio!=null)upload();else requestRecording();return true;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        holdActive=false;v.setPressed(false);
                        if(recorder!=null)stopRecording(true);return true;
                    default:return true;
                }
            });
            status.setText(Ui.t(activity,"قول شن عندك وإمتى","Tell me what and when"));
        }
        update();
    }
    void moveActions(LinearLayout target){
        if(!immersive||immersiveControls==null)return;
        ((LinearLayout)immersiveControls.getParent()).removeView(immersiveControls);
        ((LinearLayout)holdHint.getParent()).removeView(holdHint);
        Ui.add(target,immersiveControls,28);Ui.add(target,holdHint,8);
    }
    void autoStart(){
        // Explicitly requested AI-first flow. Never launch settings or record while backgrounded.
        if(AiKeyStore.hasKey(activity,AiProvider.selected(activity)))requestRecording();
        else status.setText(Ui.t(activity,"اضغط الكرة لإضافة مفتاح الـAI مرة واحدة","Tap the orb to set up your AI key once"));
    }
    private void requestRecording() {
        if (!AiKeyStore.hasKey(activity,AiProvider.selected(activity))) {
            activity.startActivity(new Intent(activity, AiSettingsActivity.class)); return;
        }
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_REQUEST);return;
        }
        startRecording();
    }
    void permissionResult(int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if(!immersive||holdActive)startRecording();
            else status.setText(Ui.t(activity,"جاهز. اضغط مطوّلًا للتسجيل","Ready. Hold to record"));
        }
        else status.setText(R.string.voice_mic_error);
    }
    private void startRecording() {
        if (destroyed || busy || recorder != null) return;
        deleteAudio();completed=false;
        recordingProvider=AiProvider.selected(activity);
        try {
            audio = File.createTempFile("voice-", ".m4a", activity.getCacheDir());
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(16000);recorder.setAudioChannels(1);
            recorder.setAudioEncodingBitRate(64000);recorder.setMaxDuration(60_000);
            recorder.setMaxFileSize(1_000_000);recorder.setOutputFile(audio.getAbsolutePath());
            recorder.setOnInfoListener((r,what,extra)->{
                if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                        ||what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)stopRecording(true);
            });
            recorder.setOnErrorListener((r,what,extra)->{
                releaseRecorder();deleteAudio();status.setText(R.string.voice_record_error);update();
            });
            recorder.prepare();recorder.start();recordedAt=ZonedDateTime.now();
            started=SystemClock.elapsedRealtime();handler.post(ticker);
        } catch(Exception e){releaseRecorder();deleteAudio();status.setText(R.string.voice_record_error);}
        update();
    }
    private void stopRecording(boolean send) {
        if(recorder==null)return;
        boolean valid=SystemClock.elapsedRealtime()-started>=700;
        try{recorder.stop();}catch(RuntimeException e){valid=false;}
        releaseRecorder();
        if(!valid||audio==null||audio.length()<100){deleteAudio();status.setText(Ui.t(activity,"اضغط مطوّلًا واحكي التذكير","Hold a little longer and speak"));}
        else if(send){upload();return;}
        else status.setText(Ui.t(activity,"اضغط لتحويل التسجيل إلى نص","Tap to transcribe this recording"));
        update();
    }
    private void releaseRecorder(){
        handler.removeCallbacks(ticker);
        if(recorder!=null){recorder.release();recorder=null;}
    }
    private void upload(){
        if(busy||audio==null||recorder!=null)return;
        busy=true;status.setText(R.string.voice_busy);update();
        final int current=++generation;final File file=audio;
        final AiProvider provider=recordingProvider==null?AiProvider.selected(activity):recordingProvider;
        client=new OpenAiClient(provider);final OpenAiClient request=client;
        final ZonedDateTime reference=recordedAt==null?ZonedDateTime.now():recordedAt;
        pending=worker.submit(()->{
            try{
                String key;
                try{key=AiKeyStore.read(activity,provider);}catch(Exception e){throw new OpenAiClient.ApiFailure(401);}
                VoiceDraft draft=request.transcribe(Files.readAllBytes(file.toPath()),key,reference);
                handler.post(()->{
                    if(destroyed||current!=generation)return;
                    busy=false;completed=true;deleteAudio();update();
                    status.setText(Ui.t(activity,"راجع الموعد واحفظ التذكير","Review the time, then save"));listener.onDraft(draft);
                });
            }catch(Exception e){
                int message=R.string.voice_network_error;
                if(e instanceof OpenAiClient.ApiFailure){
                    int code=((OpenAiClient.ApiFailure)e).status;
                    if(code==400)message=R.string.voice_request_error;
                    else if(code==401||code==403)message=R.string.voice_key_error;
                    else if(code==429)message=R.string.voice_quota_error;
                    else if(code==422)message=R.string.voice_parse_error;
                }
                final int error=message;
                handler.post(()->{if(destroyed||current!=generation)return;busy=false;status.setText(error);update();});
            }
        });
    }
    void pause(){if(recorder!=null)stopRecording(false);}
    void cancel(){
        ++generation;
        if(client!=null){OpenAiClient request=client;new Thread(request::cancel,"cancel-openai").start();}
        if(pending!=null)pending.cancel(true);
        releaseRecorder();deleteAudio();busy=false;completed=false;
        status.setText(Ui.t(activity,immersive?"قول شن عندك وإمتى":"اضغط وسجّل تذكيرك",immersive?"Tell me what and when":"Tap to record a reminder"));update();
    }
    void destroy(){destroyed=true;cancel();worker.shutdownNow();handler.removeCallbacksAndMessages(null);}
    private void deleteAudio(){if(audio!=null){audio.delete();audio=null;}}
    private void update(){
        record.spinning(false);
        record.setIcon(busy?"loader":recorder!=null?"stop":audio!=null?"retry":completed?"check":"mic",true);
        record.spinning(busy);record.setEnabled(!busy);
        if(orb!=null)orb.state(recorder!=null,busy);
        record.setContentDescription(Ui.t(activity,busy?"جاري تحويل الصوت":recorder!=null?"إيقاف وتحويل التسجيل":audio!=null?"إعادة محاولة التحويل":"تسجيل تذكير",
                busy?"Transcribing":recorder!=null?"Stop and transcribe":audio!=null?"Retry transcription":"Record reminder"));
        cancel.setVisibility(audio!=null||busy?View.VISIBLE:View.GONE);
        View save=activity.findViewById(R.id.saveButton);if(save!=null)save.setEnabled(!busy&&recorder==null);
    }
}
