package com.fakkerni.reminder;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Two small cards. Credentials and diagnostics appear only when requested. */
public class AiSettingsActivity extends Activity {
    private static final int PHONE_TONE_REQUEST=341;
    private TextView aiStatus;
    private Button toneButton;
    private MediaPlayer tonePreview;
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleHelper.wrap(base));}
    @Override protected void onCreate(Bundle state){
        Ui.theme(this);super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout root=Design.root(this),page=Ui.column(this);
        ScrollView scroll=new ScrollView(this);scroll.addView(page);root.addView(scroll);
        LinearLayout header=Ui.row(this);
        MorphIconView back=Ui.icon(this,"back",Ui.PAPER,Ui.t(this,"رجوع","Back"),48);
        back.setOnClickListener(v->finish());header.addView(back);Ui.space(header);
        header.addView(Ui.text(this,Ui.t(this,"الإعدادات","Settings"),28,Ui.WHITE));page.addView(header);
        LinearLayout personal=Ui.card(this,Ui.SURFACE);
        Ui.add(personal,Ui.text(this,Ui.t(this,"المساعد والمظهر","Assistant & appearance"),19,Ui.WHITE),0);
        Button assistantName=Ui.button(this,Ui.t(this,"اسم المساعد: ","Assistant name: ")+Appearance.name(this),Ui.INK);
        assistantName.setTextColor(Ui.WHITE);assistantName.setOnClickListener(v->editAssistantName());Ui.add(personal,assistantName,12);
        LinearLayout modes=Ui.row(this);
        for(String mode:new String[]{"dark","light"}){
            Button choice=Ui.button(this,mode.equals("dark")?Ui.t(this,"داكن","Dark"):Ui.t(this,"فاتح","Light"),
                    Appearance.mode(this).equals(mode)?Ui.YELLOW:Ui.INK);
            if(!Appearance.mode(this).equals(mode))choice.setTextColor(Ui.WHITE);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,54),1);
            if(mode.equals("light"))lp.setMarginStart(Ui.dp(this,8));modes.addView(choice,lp);
            choice.setOnClickListener(v->{Appearance.mode(this,mode);recreate();});
        }
        Ui.add(personal,modes,8);Ui.add(page,personal,16);
        LinearLayout ai=Ui.card(this,Ui.SURFACE);
        Ui.add(ai,Ui.text(this,Ui.t(this,"ربط الذكاء الاصطناعي","AI connections"),21,Ui.WHITE),0);
        aiStatus=Ui.text(this,"",13,Ui.SECONDARY);Ui.add(ai,aiStatus,8);refreshAiStatus();
        Button key=Ui.button(this,Ui.t(this,"ادخل واختار الخدمة","Connect or switch service"),Ui.YELLOW);
        key.setOnClickListener(v->startActivity(new Intent(this,AiConnectionsActivity.class)));Ui.add(ai,key,12);
        Ui.add(page,ai,20);
        LinearLayout alerts=Ui.card(this,Ui.INK);
        Ui.add(alerts,Ui.text(this,Ui.t(this,"التنبيهات","Alerts"),21,Ui.WHITE),0);
        Ui.add(alerts,Ui.text(this,Ui.t(this,"قبل الموعد بـ1 ساعة · صوت واهتزاز","1 hour before · sound & vibration"),13,Ui.SECONDARY),6);
        toneButton=Ui.button(this,Ui.t(this,"النغمة: ","Sound: ")+AlertTone.selected(this).label(this),Ui.PAPER);
        toneButton.setTextColor(Ui.WHITE);toneButton.setOnClickListener(v->chooseTone());Ui.add(alerts,toneButton,12);
        Button test=Ui.button(this,Ui.t(this,"جرّب بعد 10 ثواني","Test in 10 seconds"),Ui.YELLOW);Ui.add(alerts,test,14);
        test.setOnClickListener(v->testAlarm());
        Button permissions=Ui.button(this,Ui.t(this,"الصوت وإذن الإشعارات","Sound & notification permission"),Ui.PAPER);Ui.add(alerts,permissions,8);
        permissions.setOnClickListener(v->openNotificationSettings());Ui.add(page,alerts,14);
        Button help=Ui.button(this,Ui.t(this,"مساعدة","Help"),Ui.PAPER);Ui.add(page,help,14);help.setOnClickListener(v->showHelp());
        Ui.setup(this,root);
    }
    private void editAssistantName(){
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(Appearance.name(this));
        input.setSelectAllOnFocus(true);input.setTypeface(getResources().getFont(R.font.cairo));
        int p=Ui.dp(this,20);input.setPadding(p,p,p,p);
        new AlertDialog.Builder(this).setTitle(Ui.t(this,"اسم المساعد","Assistant name")).setView(input)
                .setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(Ui.t(this,"حفظ","Save"),(d,w)->{
                    String value=input.getText().toString().trim();if(!value.isEmpty()){Appearance.name(this,value);recreate();}
                }).show();
    }
    @Override protected void onResume(){super.onResume();refreshAiStatus();}
    private void refreshAiStatus(){if(aiStatus!=null){AiProvider active=AiProvider.active(this);
        aiStatus.setText(active==null?Ui.t(this,"ما فيش خدمة نشطة","No active service"):
                Ui.t(this,"الخدمة النشطة: ","Active service: ")+active.label);}}
    @Override protected void onStop(){stopTone();super.onStop();}
    private void chooseTone(){
        AlertTone[] tones=AlertTone.values();String[] labels=new String[tones.length];
        for(int i=0;i<tones.length;i++)labels[i]=tones[i]==AlertTone.PHONE?
                Ui.t(this,"اختار من نغمات الهاتف…","Choose from phone sounds…"):tones[i].label(this);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(Ui.t(this,"اختار نغمة التنبيه","Choose an alert sound"))
                .setSingleChoiceItems(labels,AlertTone.selected(this).ordinal(),(d,which)->{
                    AlertTone tone=tones[which];
                    if(tone==AlertTone.PHONE){d.dismiss();openPhoneTonePicker();return;}
                    AlertTone.select(this,tone);ReminderReceiver.createChannel(this);
                    toneButton.setText(Ui.t(this,"النغمة: ","Sound: ")+tone.label(this));previewTone(tone);
                }).setPositiveButton(Ui.t(this,"تم","Done"),null).create();
        dialog.setOnDismissListener(d->stopTone());dialog.show();
    }
    private void openPhoneTonePicker(){
        Intent picker=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        picker.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_NOTIFICATION);
        picker.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true);
        picker.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);
        picker.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                Ui.t(this,"اختار نغمة من الهاتف","Choose a phone notification sound"));
        picker.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                AlertTone.selected(this)==AlertTone.PHONE?AlertTone.sound(this,AlertTone.PHONE):
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        try{startActivityForResult(picker,PHONE_TONE_REQUEST);}
        catch(ActivityNotFoundException e){Toast.makeText(this,Ui.t(this,"قائمة نغمات الهاتف غير متاحة","Phone sound picker unavailable"),Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PHONE_TONE_REQUEST||result!=RESULT_OK||data==null)return;
        Uri selected=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if(selected==null){Toast.makeText(this,Ui.t(this,"اختار نغمة مسموعة","Choose an audible sound"),Toast.LENGTH_SHORT).show();return;}
        AlertTone.selectPhone(this,selected);ReminderReceiver.createChannel(this);
        toneButton.setText(Ui.t(this,"النغمة: ","Sound: ")+AlertTone.PHONE.label(this));
        previewTone(AlertTone.PHONE);
    }
    private void previewTone(AlertTone tone){
        stopTone();try{MediaPlayer player=new MediaPlayer();tonePreview=player;
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setDataSource(this,AlertTone.sound(this,tone));player.setOnCompletionListener(p->stopTone());
            player.prepare();player.start();
        }catch(Exception ignored){stopTone();Toast.makeText(this,Ui.t(this,"التجربة الصوتية غير متاحة. جرّب تنبيه 10 ثواني.",
                "Preview unavailable. Try the 10-second alert."),Toast.LENGTH_SHORT).show();}
    }
    private void stopTone(){if(tonePreview!=null){try{tonePreview.stop();}catch(Exception ignored){}
        tonePreview.release();tonePreview=null;}}
    private void testAlarm(){
        ReminderReceiver.createChannel(this);NotificationManager manager=getSystemService(NotificationManager.class);
        android.app.NotificationChannel channel=manager.getNotificationChannel(ReminderReceiver.channelId(this));
        if(!manager.areNotificationsEnabled()||channel==null||channel.getImportance()==0){openNotificationSettings();return;}
        if(!ReminderScheduler.scheduleLockScreenTest(this)){
            Toast.makeText(this,Ui.t(this,"فعّل المواعيد الدقيقة ثم أعد التجربة","Enable exact alarms, then try again"),Toast.LENGTH_LONG).show();
            if(android.os.Build.VERSION.SDK_INT>=31)open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));return;
        }
        Toast.makeText(this,Ui.t(this,"اقفل الشاشة توا. الـ10 ثواني تبدأ من الضغط.","Lock now. The 10 seconds start from this tap."),Toast.LENGTH_LONG).show();
    }
    private void showHelp(){
        String[] options={Ui.t(this,"حالة آخر تنبيه","Last alarm status"),Ui.t(this,"البطارية والخلفية","Battery & background"),Ui.t(this,"الخصوصية والصوت","Privacy & sound")};
        new AlertDialog.Builder(this).setTitle(Ui.t(this,"مساعدة","Help")).setItems(options,(d,i)->{
            if(i==0)new AlertDialog.Builder(this).setMessage(AlarmDiagnostics.report(this)).setPositiveButton(android.R.string.ok,null)
                    .setNeutralButton(Ui.t(this,"نسخ","Copy"),(a,b)->getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Alarm status",AlarmDiagnostics.report(this)))).show();
            if(i==1)new AlertDialog.Builder(this).setMessage(Ui.t(this,"realme: البطارية ← إدارة بطارية التطبيقات ← فكّرني. فعّل النشاط في الخلفية والتشغيل التلقائي. لا تغيّرها إن كانت التنبيهات تعمل.",
                            "realme: Battery → App battery management → فكّرني. Enable background activity and auto launch. Leave these unchanged if alerts work."))
                    .setNegativeButton(android.R.string.cancel,null).setPositiveButton(Ui.t(this,"فتح الإعدادات","Open settings"),(a,b)->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).show();
            if(i==2)new AlertDialog.Builder(this).setMessage(Ui.t(this,"محتوى شاشة القفل مقروء للآخرين. الصوت وعدم الإزعاج وإضاءة الشاشة تخضع للهاتف. التسجيل والنص يُرسلان للمزوّد المختار فقط. مطابقة جهات الاتصال محلية بإذنك؛ لا نرفع دفتر الأرقام.",
                            "Lock-screen content can be read by others. Sound, DND and screen wake depend on your phone. Audio and text go only to your selected provider. Contact matching is local with your permission; the address book is never uploaded."))
                    .setPositiveButton(android.R.string.ok,null).show();
        }).show();
    }
    private void openNotificationSettings(){open(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,ReminderReceiver.channelId(this)));}
    private void open(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException e){Toast.makeText(this,Ui.t(this,"افتح إعدادات الهاتف يدوياً","Open phone settings manually"),Toast.LENGTH_LONG).show();}}
}
