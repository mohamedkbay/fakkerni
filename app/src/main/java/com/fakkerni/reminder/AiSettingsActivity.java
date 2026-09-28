package com.fakkerni.reminder;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Two small cards. Credentials and diagnostics appear only when requested. */
public class AiSettingsActivity extends Activity {
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleHelper.wrap(base));}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout root=Design.root(this),page=Ui.column(this);
        ScrollView scroll=new ScrollView(this);scroll.addView(page);root.addView(scroll);
        LinearLayout header=Ui.row(this);
        MorphIconView back=Ui.icon(this,"back",Ui.PAPER,Ui.t(this,"رجوع","Back"),48);
        back.setOnClickListener(v->finish());header.addView(back);Ui.space(header);
        header.addView(Ui.text(this,Ui.t(this,"الإعدادات","Settings"),28,Ui.WHITE));page.addView(header);
        AiProvider provider=AiProvider.selected(this);AiProvider.select(this,provider);
        LinearLayout ai=Ui.card(this,Ui.SURFACE);
        Ui.add(ai,Ui.text(this,Ui.t(this,"التسجيل الذكي","Voice AI"),21,Ui.WHITE),0);
        LinearLayout choices=Ui.row(this);
        for(AiProvider option:new AiProvider[]{AiProvider.GROQ,AiProvider.OPENAI}){
            Button choose=Ui.button(this,option.label,provider==option?Ui.YELLOW:Ui.WHITE);
            choose.setSelected(provider==option);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMarginEnd(Ui.dp(this,6));choices.addView(choose,lp);
            choose.setOnClickListener(v->{if(provider!=option){AiProvider.select(this,option);recreate();}});
        }
        Ui.add(ai,choices,12);
        Ui.add(ai,Ui.text(this,AiKeyStore.hasKey(this,provider)?Ui.t(this,"المفتاح محفوظ · جاهز للتجربة","Key saved · ready to try")
                :Ui.t(this,"أضف المفتاح مرة واحدة","Add your key once"),13,Ui.SECONDARY),10);
        Button key=Ui.button(this,AiKeyStore.hasKey(this,provider)?Ui.t(this,"تغيير المفتاح","Change key"):Ui.t(this,"إضافة المفتاح","Add key"),Ui.WHITE);
        key.setOnClickListener(v->editKey(provider));Ui.add(ai,key,12);
        Ui.add(ai,Ui.text(this,Ui.t(this,"الصوت والنص يُرسلان إلى ","Audio and text go to ")+provider.label
                +Ui.t(this,". الأسماء تُطابق محلياً. راجع الموعد قبل الحفظ.",". Contacts match locally. Review before saving."),12,Ui.SECONDARY),12);
        Ui.add(page,ai,20);
        LinearLayout alerts=Ui.card(this,Ui.INK);
        Ui.add(alerts,Ui.text(this,Ui.t(this,"التنبيهات","Alerts"),21,Ui.WHITE),0);
        Ui.add(alerts,Ui.text(this,Ui.t(this,"قبل الموعد بـ1 ساعة · صوت واهتزاز","1 hour before · sound & vibration"),13,Ui.PAPER),6);
        Button test=Ui.button(this,Ui.t(this,"جرّب بعد 10 ثواني","Test in 10 seconds"),Ui.YELLOW);Ui.add(alerts,test,14);
        test.setOnClickListener(v->testAlarm());
        Button permissions=Ui.button(this,Ui.t(this,"الصوت وإذن الإشعارات","Sound & notification permission"),Ui.PAPER);Ui.add(alerts,permissions,8);
        permissions.setOnClickListener(v->openNotificationSettings());Ui.add(page,alerts,14);
        Button help=Ui.button(this,Ui.t(this,"مساعدة","Help"),Ui.PAPER);Ui.add(page,help,14);help.setOnClickListener(v->showHelp());
        Ui.setup(this,root);
    }
    private void editKey(AiProvider provider){
        LinearLayout content=Ui.column(this);int pad=Ui.dp(this,20);content.setPadding(pad,pad,pad,pad);
        Ui.add(content,Ui.text(this,provider==AiProvider.GROQ
                ?Ui.t(this,"خطة Groq المجانية بحدود استخدام. المفتاح مشفّر على هاتفك.","Groq’s free plan has usage limits. Your key is encrypted on your phone.")
                :Ui.t(this,"رصيد OpenAI API منفصل عن ChatGPT. المفتاح مشفّر على هاتفك.","OpenAI API billing is separate from ChatGPT. Your key is encrypted on your phone."),13,Ui.INK),0);
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setTypeface(getResources().getFont(R.font.cairo));input.setTextColor(Ui.INK);input.setHintTextColor(Ui.MUTED);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setSaveEnabled(false);input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        input.setHint(provider.keyPrefix+"…");input.setContentDescription(provider.label+" API key");Ui.add(content,input,12);
        Button link=Ui.button(this,Ui.t(this,"احصل على مفتاح ","Get a ")+provider.label+Ui.t(this,""," key"),Ui.PAPER);Ui.add(content,link,12);
        link.setOnClickListener(v->open(new Intent(Intent.ACTION_VIEW,Uri.parse(provider.keysUrl))));
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(provider.label).setView(content)
                .setNegativeButton(android.R.string.cancel,null).setPositiveButton(Ui.t(this,"حفظ","Save"),null);
        if(AiKeyStore.hasKey(this,provider))builder.setNeutralButton(Ui.t(this,"حذف المفتاح","Remove key"),(d,w)->
                new AlertDialog.Builder(this).setMessage(Ui.t(this,"تحذف مفتاح ","Remove key for ")+provider.label+"؟")
                        .setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok,(a,b)->{AiKeyStore.remove(this,provider);recreate();}).show());
        AlertDialog dialog=builder.create();dialog.setOnShowListener(d->{
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                String key=input.getText().toString().trim();
                if(!provider.accepts(key)){input.setError(Ui.t(this,"يلزم مفتاح يبدأ بـ ","Key must start with ")+provider.keyPrefix);return;}
                try{AiKeyStore.save(this,provider,key);input.setText("");dialog.dismiss();recreate();
                    Toast.makeText(this,Ui.t(this,"تم الحفظ. جرّب الميكروفون في تذكير جديد.","Saved. Try the microphone in a new reminder."),Toast.LENGTH_LONG).show();}
                catch(Exception e){input.setError(Ui.t(this,"تعذّر الحفظ","Could not save key"));}
            });
        });dialog.show();
    }
    private void testAlarm(){
        ReminderReceiver.createChannel(this);NotificationManager manager=getSystemService(NotificationManager.class);
        if(!manager.areNotificationsEnabled()||manager.getNotificationChannel(ReminderReceiver.CHANNEL_ID).getImportance()==0){openNotificationSettings();return;}
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
    private void openNotificationSettings(){open(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,ReminderReceiver.CHANNEL_ID));}
    private void open(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException e){Toast.makeText(this,Ui.t(this,"افتح إعدادات الهاتف يدوياً","Open phone settings manually"),Toast.LENGTH_LONG).show();}}
}
