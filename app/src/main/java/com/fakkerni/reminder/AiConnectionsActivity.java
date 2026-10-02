package com.fakkerni.reminder;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

/** One active voice provider; other encrypted keys remain available for one-tap switching. */
public class AiConnectionsActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout list;
    private boolean destroyed;

    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleHelper.wrap(base));}
    @Override protected void onCreate(Bundle state){
        Ui.theme(this);super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout root=Design.root(this),page=Ui.column(this);
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout header=Ui.row(this);
        MorphIconView back=Ui.icon(this,"back",Ui.PAPER,Ui.t(this,"رجوع","Back"),48);
        back.setOnClickListener(v->finish());header.addView(back);Ui.space(header);
        header.addView(Ui.text(this,Ui.t(this,"ربط الذكاء الاصطناعي","Connect AI"),25,Ui.WHITE));
        Ui.add(page,header,0);
        Ui.add(page,Ui.text(this,Ui.t(this,"اربط أكثر من خدمة، واختار واحدة فقط للتسجيل. تبديل الخدمة المربوطة بلمسة واحدة.",
                "Connect multiple services, but use only one for recording. Switch a connected service with one tap."),14,Ui.SECONDARY),18);
        list=Ui.column(this);Ui.add(page,list,22);
        Ui.add(page,Ui.text(this,Ui.t(this,"مفتاحك محفوظ مشفّر على هذا الهاتف. التسجيل يروح للخدمة النشطة فقط، ولا نرفع دفتر أرقامك.",
                "Keys stay encrypted on this phone. Only the active service receives your recording; your address book stays local."),12,Ui.SECONDARY),18);
        Ui.setup(this,root);render();
    }
    @Override protected void onResume(){super.onResume();if(list!=null)render();}
    @Override protected void onDestroy(){destroyed=true;worker.shutdownNow();super.onDestroy();}

    private void render(){
        list.removeAllViews();AiProvider active=AiProvider.active(this);
        for(AiProvider provider:new AiProvider[]{AiProvider.GROQ,AiProvider.OPENAI,AiProvider.GEMINI}){
            boolean connected=AiKeyStore.hasKey(this,provider),selected=active==provider;
            LinearLayout card=Ui.card(this,selected?Ui.SURFACE:Ui.INK);
            LinearLayout title=Ui.row(this);
            title.addView(Ui.text(this,provider.label,21,Ui.WHITE));Ui.space(title);
            TextView badge=Ui.text(this,selected?Ui.t(this,"نشط","Active"):
                    connected?Ui.t(this,"مربوط","Connected"):Ui.t(this,"غير مربوط","Not connected"),12,selected?Ui.YELLOW:Ui.SECONDARY);
            title.addView(badge);Ui.add(card,title,0);
            String detail=provider==AiProvider.GROQ?Ui.t(this,"تفريغ عربي سريع وفهم التذكير","Fast Arabic transcription and reminder extraction"):
                    provider==AiProvider.OPENAI?Ui.t(this,"تحويل الصوت وفهم الموعد","Voice transcription and time extraction"):
                            Ui.t(this,"الصوت والموعد في طلب واحد","Audio and reminder in one request");
            Ui.add(card,Ui.text(this,detail,13,Ui.SECONDARY),6);
            Button action=Ui.button(this,selected?Ui.t(this,"الخدمة النشطة","Active service"):
                    connected?Ui.t(this,"فعّل بضغطة","Activate in one tap"):Ui.t(this,"ادخل واربط","Connect"),selected?Ui.PAPER:Ui.YELLOW);
            if(selected)action.setTextColor(Ui.WHITE);
            action.setEnabled(!selected);
            action.setOnClickListener(v->{if(connected){AiProvider.select(this,provider);render();}
                else editKey(provider);});Ui.add(card,action,16);
            if(connected){
                Button manage=Ui.button(this,Ui.t(this,"تغيير أو حذف المفتاح","Change or remove key"),Ui.PAPER);
                manage.setTextColor(Ui.WHITE);manage.setOnClickListener(v->editKey(provider));Ui.add(card,manage,8);
            }
            Ui.add(list,card,12);
        }
    }
    private void editKey(AiProvider provider){
        LinearLayout content=Ui.column(this);int pad=Ui.dp(this,18);content.setPadding(pad,pad,pad,pad);
        Ui.add(content,Ui.text(this,Ui.t(this,"ألصق مفتاح ","Paste your ")+provider.label+
                Ui.t(this," مرة واحدة. حنتأكد منه قبل ما نفعّله."," key once. We'll check it before activating."),13,Ui.SECONDARY),0);
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setTypeface(getResources().getFont(R.font.cairo));input.setTextColor(Ui.WHITE);input.setHintTextColor(Ui.MUTED);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setSaveEnabled(false);input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        input.setHint(provider==AiProvider.GEMINI?"AIza… / AQ.…":provider.keyPrefix+"…");
        input.setContentDescription(provider.label+" API key");Ui.add(content,input,12);
        Button paste=Ui.button(this,Ui.t(this,"لصق من الحافظة","Paste from clipboard"),Ui.PAPER);
        paste.setTextColor(Ui.WHITE);paste.setOnClickListener(v->{ClipboardManager clipboard=getSystemService(ClipboardManager.class);
            if(clipboard.hasPrimaryClip()&&clipboard.getPrimaryClip()!=null&&clipboard.getPrimaryClip().getItemCount()>0){
                CharSequence value=clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
                if(value!=null)input.setText(value.toString().trim());
            }});Ui.add(content,paste,10);
        Button link=Ui.button(this,Ui.t(this,"احصل على مفتاح ","Get a ")+provider.label+Ui.t(this,""," key"),Ui.PAPER);
        link.setTextColor(Ui.WHITE);link.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(provider.keysUrl)));}
            catch(ActivityNotFoundException e){Toast.makeText(this,Ui.t(this,"تعذّر فتح الرابط","Could not open link"),Toast.LENGTH_SHORT).show();}});
        Ui.add(content,link,8);
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(provider.label).setView(content)
                .setNegativeButton(android.R.string.cancel,null).setPositiveButton(Ui.t(this,"تحقق واربط","Verify & connect"),null);
        if(AiKeyStore.hasKey(this,provider))builder.setNeutralButton(Ui.t(this,"حذف المفتاح","Remove key"),(d,w)->removeKey(provider));
        AlertDialog dialog=builder.create();dialog.setOnShowListener(d->{
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            Button confirm=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirm.setOnClickListener(v->{String key=input.getText().toString().trim();
                if(!provider.accepts(key)){input.setError(Ui.t(this,"تحقق من شكل المفتاح","Check the key format"));return;}
                confirm.setEnabled(false);confirm.setText(Ui.t(this,"جاري التحقق…","Checking…"));
                worker.submit(()->{
                    int status=-1;try{status=AiConnectionVerifier.verify(provider,key);}catch(Exception ignored){}
                    final int result=status;runOnUiThread(()->{
                        if(destroyed||!dialog.isShowing())return;
                        confirm.setEnabled(true);confirm.setText(Ui.t(this,"تحقق واربط","Verify & connect"));
                        if(result==200){try{AiKeyStore.save(this,provider,key);AiProvider.select(this,provider);
                            input.setText("");dialog.dismiss();render();Toast.makeText(this,Ui.t(this,"تم الربط والتفعيل","Connected and active"),Toast.LENGTH_SHORT).show();}
                            catch(Exception e){input.setError(Ui.t(this,"تعذّر حفظ المفتاح","Could not save key"));}}
                        else if(result==401||result==403)input.setError(Ui.t(this,"المفتاح مرفوض. تأكد منه ومن صلاحيات المشروع.","Key rejected. Check it and project permissions."));
                        else if(result==429)input.setError(Ui.t(this,"وصلت حد الاستخدام. جرّب لاحقاً.","Rate limit reached. Try later."));
                        else input.setError(Ui.t(this,"تعذّر التحقق من الاتصال. جرّب مرة أخرى.","Could not verify connection. Try again."));
                    });
                });
            });
        });dialog.show();
    }
    private void removeKey(AiProvider provider){
        new AlertDialog.Builder(this).setMessage(Ui.t(this,"تحذف مفتاح ","Remove the ")+provider.label+Ui.t(this,"؟"," key?"))
                .setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok,(d,w)->{
                    AiKeyStore.remove(this,provider);
                    render();
                }).show();
    }
}
