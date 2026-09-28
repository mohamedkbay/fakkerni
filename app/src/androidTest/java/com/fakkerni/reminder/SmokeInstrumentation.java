package com.fakkerni.reminder;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import java.time.*;

/** Run only on a disposable emulator. No network calls or real credentials. */
public class SmokeInstrumentation extends Instrumentation {
    private int checks;
    private boolean providersOnly;
    private boolean assistantOnly;
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);providersOnly=arguments!=null&&"true".equals(arguments.getString("providers"));assistantOnly=arguments!=null&&"true".equals(arguments.getString("assistant"));start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            Context c=getTargetContext();
            if(!"ranchu".equals(Build.HARDWARE)&&!"goldfish".equals(Build.HARDWARE))
                throw new IllegalStateException("Disposable emulator only");
            if(assistantOnly){
                int count=ReminderStore.getAll(c).size();
                check(!AiKeyStore.hasKey(c,AiProvider.GROQ)&&!AiKeyStore.hasKey(c,AiProvider.OPENAI),"No existing credentials");
                AssistantActivity assistant=(AssistantActivity)startActivitySync(new Intent(c,AssistantActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                runOnMainSync(()->{check(assistant.findViewById(R.id.voicePanel)!=null,"Assistant orb panel");assistant.finish();});
                String json=new org.json.JSONObject().put("transcript","غدوة الساعة خمسة مساء اتصل بأحمد")
                        .put("title","اتصل بأحمد").put("date",LocalDate.now().plusDays(1).toString())
                        .put("time","17:00").put("phone","+218912345678").put("contact_name","").put("needs_review",false).toString();
                AddReminderActivity review=(AddReminderActivity)startActivitySync(new Intent(c,AddReminderActivity.class)
                        .putExtra("voiceDraft",VoiceDraft.parse(json).toJson()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                runOnMainSync(()->{
                    check("اتصل بأحمد".contentEquals(((TextView)review.findViewById(R.id.reminderText)).getText()),"AI title fills review");
                    check("+218912345678".contentEquals(((TextView)review.findViewById(R.id.phoneText)).getText()),"Phone draft fills review");
                    check(review.findViewById(R.id.voicePanel).getVisibility()==android.view.View.GONE,"Review hides recording controls");
                    check(((TextView)review.findViewById(R.id.timeButton)).getText().toString().contains("5:00"),"AI event time retained");
                    check(ReminderStore.getAll(c).size()==count,"AI cannot auto-save a reminder");
                    check(review.findViewById(R.id.saveButton).isEnabled(),"Confirmation available");
                    review.finish();
                });
                check(ReminderStore.getAll(c).size()==count,"Dismissed draft remains unsaved");
                if(c.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)==android.content.pm.PackageManager.PERMISSION_GRANTED){
                    java.util.List<LocalContacts.Match> matches=LocalContacts.find(c,"Ahmed");
                    check(matches.size()==1&&"+218910000001".equals(matches.get(0).phone),"Local synthetic contact matched");
                    org.json.JSONObject contactDraft=new org.json.JSONObject(json).put("phone","").put("contact_name","Ahmed");
                    AddReminderActivity contactReview=(AddReminderActivity)startActivitySync(new Intent(c,AddReminderActivity.class)
                            .putExtra("voiceDraft",contactDraft.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    java.util.concurrent.atomic.AtomicReference<String> number=new java.util.concurrent.atomic.AtomicReference<>("");
                    long deadline=SystemClock.elapsedRealtime()+5000;
                    while(number.get().isEmpty()&&SystemClock.elapsedRealtime()<deadline){
                        runOnMainSync(()->number.set(((TextView)contactReview.findViewById(R.id.phoneText)).getText().toString()));SystemClock.sleep(100);
                    }
                    check("+218910000001".equals(number.get()),"Unique contact fills the review draft");
                    check(ReminderStore.getAll(c).size()==count,"Contact matching cannot auto-save");
                    runOnMainSync(contactReview::finish);
                }
                if(c.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED){
                    try{
                        AiProvider.select(c,AiProvider.GROQ);AiKeyStore.save(c,AiProvider.GROQ,"gsk_dummy_not_a_real_key_123456789");
                        AssistantActivity recording=(AssistantActivity)startActivitySync(new Intent(c,AssistantActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        check(c.getCacheDir().listFiles((dir,name)->name.startsWith("voice-")).length>0,"Launch starts foreground recording");
                        runOnMainSync(recording::finish);waitForIdleSync();
                        check(c.getCacheDir().listFiles((dir,name)->name.startsWith("voice-")).length==0,"Leaving assistant clears local audio");
                    }finally{AiKeyStore.remove(c,AiProvider.GROQ);}
                }
                result.putString("stream","PASS: "+checks+" assistant assertions\n");finish(Activity.RESULT_OK,result);return;
            }
            if(providersOnly){
                check(!AiKeyStore.hasKey(c,AiProvider.OPENAI)&&!AiKeyStore.hasKey(c,AiProvider.GROQ),"No existing credentials may be replaced");
                String openai="sk-dummy-not-a-real-key-123456789",groq="gsk_dummy_not_a_real_key_123456789";
                try{
                    AiKeyStore.save(c,AiProvider.OPENAI,openai);AiKeyStore.save(c,AiProvider.GROQ,groq);
                    check(openai.equals(AiKeyStore.read(c)),"Old OpenAI storage compatibility");
                    check(groq.equals(AiKeyStore.read(c,AiProvider.GROQ)),"Groq encryption round trip");
                    AiProvider.select(c,AiProvider.GROQ);check(AiProvider.selected(c)==AiProvider.GROQ,"Groq selection");
                    AiKeyStore.remove(c,AiProvider.GROQ);check(!AiKeyStore.hasKey(c,AiProvider.GROQ),"Groq removal");
                    check(openai.equals(AiKeyStore.read(c)),"Groq removal leaves OpenAI untouched");
                    AiProvider.select(c,AiProvider.OPENAI);check(AiProvider.selected(c)==AiProvider.OPENAI,"OpenAI selection");
                }finally{AiKeyStore.remove(c,AiProvider.OPENAI);AiKeyStore.remove(c,AiProvider.GROQ);AiProvider.select(c,AiProvider.GROQ);}
                check(!AiKeyStore.hasKey(c,AiProvider.OPENAI)&&!AiKeyStore.hasKey(c,AiProvider.GROQ),"No test credentials retained");
                result.putString("stream","PASS: "+checks+" provider assertions\n");finish(Activity.RESULT_OK,result);return;
            }
            check(ReminderStore.getAll(c).isEmpty(),"Fresh test install is required");
            check(!AiKeyStore.hasKey(c),"Must not replace an existing credential");
            String dummy="not-a-real-secret-test-only";
            AiKeyStore.save(c,dummy);check(dummy.equals(AiKeyStore.read(c)),"Keystore round trip");
            AiKeyStore.remove(c);check(!AiKeyStore.hasKey(c),"Credential removal");
            ReminderReceiver.createChannel(LocaleHelper.wrap(c));
            NotificationChannel channel=c.getSystemService(NotificationManager.class).getNotificationChannel(ReminderReceiver.CHANNEL_ID);
            check(channel.getImportance()==NotificationManager.IMPORTANCE_HIGH,"High importance channel");
            check(channel.getSound()!=null&&channel.shouldVibrate(),"Audible and vibrating channel");
            check(channel.getAudioAttributes().getUsage()==android.media.AudioAttributes.USAGE_ALARM,"Alarm sound stream");
            long tomorrow=LocalDate.now().plusDays(1).atTime(10,30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            Reminder first=new Reminder(91001,"اتصال بالعميل",tomorrow,"+٢١٨٩١٢٣٤٥٦٧٨","أحمد علي");
            ReminderStore.save(c,first);
            check("+218912345678".equals(ReminderStore.find(c,91001).phoneNumber),"Stored Latin phone");
            check("أحمد علي".equals(ReminderStore.find(c,91001).contactName),"Stored contact name");
            ReminderStore.save(c,new Reminder(91002,"تسليم تصميم المشروع",tomorrow+2*3600000,""));
            ReminderStore.save(c,new Reminder(91003,"مراجعة مهام الأسبوع",tomorrow+24*3600000,""));
            MainActivity main=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(()->{
                check(main.findViewById(R.id.aiSettingsButton)!=null,"Settings navigation");
                check(!((TextView)main.findViewById(R.id.todayDate)).getText().toString().matches(".*[٠-٩۰-۹].*"),"Latin date digits");
                check(((LinearLayout)main.findViewById(R.id.reminderList)).getChildCount()>=3,"Rendered reminder cards");
                main.finish();
            });
            AddReminderActivity editor=(AddReminderActivity)startActivitySync(new Intent(c,AddReminderActivity.class).putExtra("id",91001L).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(()->{
                check("أحمد علي".contentEquals(((TextView)editor.findViewById(R.id.contactName)).getText()),"Editor contact name");
                EditText phone=editor.findViewById(R.id.phoneText);phone.setText("٠٩١٢٣٤٥٦٧٨");
                check("0912345678".contentEquals(phone.getText()),"Input digits conversion");
                check(!"أحمد علي".contentEquals(((TextView)editor.findViewById(R.id.contactName)).getText()),"Editing number clears linked name");
                check(editor.findViewById(R.id.voicePanel).getVisibility()==android.view.View.GONE,"Editing is separate from assistant");
                editor.finish();
            });
            Reminder near=new Reminder(91999,"Scheduled notification smoke test",System.currentTimeMillis()+30000,"");
            ReminderStore.save(c,near);ReminderScheduler.schedule(c,near);
            long deadline=SystemClock.elapsedRealtime()+15000;boolean posted=false;
            while(SystemClock.elapsedRealtime()<deadline){
                for(android.service.notification.StatusBarNotification n:c.getSystemService(NotificationManager.class).getActiveNotifications())
                    if(n.getId()==91999)posted=true;
                if(posted)break;SystemClock.sleep(250);
            }
            check(posted,"AlarmManager delivered a notification");
            c.getSystemService(NotificationManager.class).cancel(91999);ReminderStore.delete(c,91999);
            c.getSharedPreferences("fakkerni_reminders",Context.MODE_PRIVATE).edit().putBoolean("qa_complete",true).commit();
            result.putString("stream","PASS: "+checks+" device assertions. Three sample reminders remain for visual QA.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+e.getClass().getSimpleName()+": "+e.getMessage()+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
}
