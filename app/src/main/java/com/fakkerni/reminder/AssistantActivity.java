package com.fakkerni.reminder;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** AI-first entry: one-time name/theme choice, then a hold-to-speak assistant. */
public class AssistantActivity extends Activity {
    private VoiceInput voice;
    private boolean foreground;
    private VoiceDraft pendingDraft;
    private String appliedMode;
    private TextView providerChip;
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleHelper.wrap(base));}
    @Override protected void onCreate(Bundle state){
        Ui.theme(this);super.onCreate(state);appliedMode=Appearance.mode(this);
        if(!Appearance.ready(this)){showIntro();return;}
        showAssistant();
    }
    private void showAssistant(){
        LinearLayout root=Design.root(this),nav=Ui.row(this);
        MorphIconView back=Ui.icon(this,"back",android.graphics.Color.TRANSPARENT,Ui.t(this,"تذكيراتي","My reminders"),44);
        back.setOnClickListener(v->finish());nav.addView(back);Ui.space(nav);
        TextView brand=Ui.text(this,Appearance.name(this),15,Ui.WHITE);brand.setGravity(Gravity.CENTER);nav.addView(brand);Ui.space(nav);
        providerChip=Ui.text(this,"",12,Ui.WHITE);providerChip.setGravity(Gravity.CENTER);
        providerChip.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);
        providerChip.setBackground(Ui.rounded(Ui.SURFACE,24,this));
        providerChip.setOnClickListener(v->startActivity(new Intent(this,AiConnectionsActivity.class)));
        nav.addView(providerChip,new LinearLayout.LayoutParams(-2,Ui.dp(this,42)));
        MorphIconView settings=Ui.icon(this,"settings",android.graphics.Color.TRANSPARENT,Ui.t(this,"الإعدادات","Settings"),44);
        settings.setOnClickListener(v->startActivity(new Intent(this,AiSettingsActivity.class)));nav.addView(settings);root.addView(nav);
        refreshProviderChip();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout center=Ui.column(this);center.setGravity(Gravity.CENTER);scroll.addView(center);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout panel=Ui.column(this);panel.setId(R.id.voicePanel);Ui.add(center,panel,0);
        TextView heading=Ui.text(this,Ui.t(this,"قولها، ونرتّبها لك.","Say it. I'll make it a reminder."),26,Ui.WHITE);
        heading.setGravity(Gravity.CENTER);Ui.add(center,heading,28);
        TextView hint=Ui.text(this,Ui.t(this,"شن عندك؟ إمتى؟ ولو فيه شخص، قول اسمه.","What, when, and who to call—just say it."),14,Ui.SECONDARY);
        hint.setGravity(Gravity.CENTER);Ui.add(center,hint,10);
        LinearLayout footer=Ui.row(this);footer.setGravity(Gravity.CENTER);
        Button manual=Ui.button(this,Ui.t(this,"نكتب بيدي","Type instead"),Ui.SURFACE);
        manual.setTextColor(Ui.WHITE);footer.addView(manual,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));
        manual.setOnClickListener(v->{if(voice!=null)voice.cancel();openEditor(null);});
        Button reminders=Ui.button(this,Ui.t(this,"تذكيراتي","My reminders"),Ui.INK);
        reminders.setTextColor(Ui.WHITE);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,Ui.dp(this,56),1);rp.setMarginStart(Ui.dp(this,8));footer.addView(reminders,rp);
        reminders.setOnClickListener(v->finish());Ui.add(root,footer,12);
        TextView privacy=Ui.text(this,Ui.t(this,"ما ينحفظ شي إلا لما توافق","Nothing saves before you approve"),11,Ui.SECONDARY);
        privacy.setGravity(Gravity.CENTER);Ui.add(root,privacy,10);
        Ui.setup(this,root);
        voice=new VoiceInput(this,panel,draft->{if(foreground)openEditor(draft);else pendingDraft=draft;},true);
        voice.moveActions(center);
    }
    private void refreshProviderChip(){if(providerChip!=null){AiProvider active=AiProvider.active(this);
        providerChip.setText(active==null?Ui.t(this,"ربط AI","Connect AI"):active.label);}}
    private void showIntro(){
        LinearLayout root=Design.root(this),page=Ui.column(this);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView overline=Ui.text(this,Ui.t(this,"مساعدك الشخصي","YOUR ASSISTANT"),12,Ui.SECONDARY);
        Ui.add(page,overline,16);
        AssistantOrb preview=new AssistantOrb(this);page.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(this,260)));
        TextView title=Ui.text(this,Ui.t(this,"خلّينا نبدأ بطريقتك.","Let's make it yours."),29,Ui.WHITE);Ui.add(page,title,4);
        Ui.add(page,Ui.text(this,Ui.t(this,"اختار اسم للمساعد والمظهر. تقدر تغيّرهم بعدين من الإعدادات.",
                "Choose the assistant's name and theme. You can change both later."),14,Ui.SECONDARY),8);
        Ui.add(page,Ui.text(this,Ui.t(this,"اسم المساعد","ASSISTANT NAME"),13,Ui.SECONDARY),30);
        EditText name=new EditText(this);name.setSingleLine(true);name.setText(Appearance.name(this));
        name.setSelectAllOnFocus(true);name.setTextColor(Ui.WHITE);name.setHintTextColor(Ui.SECONDARY);
        name.setTypeface(getResources().getFont(R.font.cairo));name.setBackground(Ui.rounded(Ui.SURFACE,22,this));
        int p=Ui.dp(this,16);name.setPadding(p,Ui.dp(this,10),p,Ui.dp(this,10));
        Ui.add(page,name,8);
        Ui.add(page,Ui.text(this,Ui.t(this,"المظهر","APPEARANCE"),13,Ui.SECONDARY),24);
        LinearLayout modes=Ui.row(this);
        String[] values={"dark","light"};
        String[] labels={Ui.t(this,"داكن","Dark"),Ui.t(this,"فاتح","Light")};
        for(int i=0;i<2;i++){
            final String mode=values[i];
            Button choice=Ui.button(this,labels[i],Appearance.mode(this).equals(mode)?Ui.YELLOW:Ui.SURFACE);
            if(!Appearance.mode(this).equals(mode))choice.setTextColor(Ui.WHITE);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,Ui.dp(this,60),1);
            if(i>0)cp.setMarginStart(Ui.dp(this,8));modes.addView(choice,cp);
            choice.setOnClickListener(v->{Appearance.name(this,name.getText().toString().trim().isEmpty()?Appearance.name(this):name.getText().toString());Appearance.mode(this,mode);recreate();});
        }
        Ui.add(page,modes,8);
        Button start=Ui.button(this,Ui.t(this,"ابدأ","Get started"),Ui.YELLOW);
        start.setOnClickListener(v->{String chosen=name.getText().toString().trim();
            if(chosen.isEmpty()){name.setError(Ui.t(this,"اكتب اسم","Enter a name"));return;}
            Appearance.name(this,chosen);Appearance.completeIntro(this);recreate();
        });Ui.add(root,start,16);
        Ui.setup(this,root);
    }
    private void openEditor(VoiceDraft draft){
        Intent intent=new Intent(this,AddReminderActivity.class);
        if(draft!=null)intent.putExtra("voiceDraft",draft.toJson());
        startActivity(intent);finish();
    }
    @Override protected void onResume(){super.onResume();foreground=true;
        if(!Appearance.mode(this).equals(appliedMode)){recreate();return;}
        refreshProviderChip();
        if(pendingDraft!=null){VoiceDraft draft=pendingDraft;pendingDraft=null;openEditor(draft);}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);
        if(request==VoiceInput.MICROPHONE_REQUEST&&voice!=null)voice.permissionResult(results);
    }
    @Override protected void onPause(){foreground=false;if(voice!=null)voice.pause();super.onPause();}
    @Override protected void onDestroy(){if(voice!=null)voice.destroy();super.onDestroy();}
}
