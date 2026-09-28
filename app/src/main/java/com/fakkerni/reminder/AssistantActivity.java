package com.fakkerni.reminder;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** The launch screen has one primary action: speak, then review the draft. */
public class AssistantActivity extends Activity {
    private VoiceInput voice;
    private boolean autoStart;
    private boolean foreground;
    private VoiceDraft pendingDraft;
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleHelper.wrap(base));}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout root=Design.root(this),nav=Ui.row(this);
        MorphIconView back=Ui.icon(this,"back",Ui.SURFACE,Ui.t(this,"تذكيراتي","My reminders"),48);
        back.setOnClickListener(v->finish());nav.addView(back);Ui.space(nav);
        TextView brand=Ui.text(this,getString(R.string.app_name),17,Ui.WHITE);nav.addView(brand);Ui.space(nav);
        MorphIconView settings=Ui.icon(this,"settings",Ui.SURFACE,Ui.t(this,"الإعدادات","Settings"),48);
        settings.setOnClickListener(v->startActivity(new Intent(this,AiSettingsActivity.class)));nav.addView(settings);root.addView(nav);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout center=Ui.column(this);center.setGravity(Gravity.CENTER);scroll.addView(center);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView heading=Ui.text(this,Ui.t(this,"شن نذكّرك بيه؟","What’s on your mind?"),29,Ui.WHITE);heading.setGravity(Gravity.CENTER);Ui.add(center,heading,24);
        TextView hint=Ui.text(this,Ui.t(this,"قول شن عندك وإمتى. الباقي عليّ.","Say what and when. I’ll fill it in."),14,Ui.SECONDARY);hint.setGravity(Gravity.CENTER);Ui.add(center,hint,8);
        LinearLayout panel=Ui.column(this);panel.setId(R.id.voicePanel);Ui.add(center,panel,0);
        TextView example=Ui.text(this,Ui.t(this,"مثلاً: ذكّرني غدوة 5 مساء نتصل بأحمد","Try: remind me tomorrow at 5 pm to call Ahmed"),12,Ui.SECONDARY);example.setGravity(Gravity.CENTER);Ui.add(center,example,16);
        Button manual=Ui.button(this,Ui.t(this,"نكتب بيدي","Type instead"),Ui.SURFACE);manual.setTextColor(Ui.WHITE);
        manual.setOnClickListener(v->{voice.cancel();openEditor(null);});Ui.add(root,manual,16);
        TextView privacy=Ui.text(this,Ui.t(this,"ما يتحفظ حتى تذكير إلا بموافقتك","Nothing is saved until you approve"),11,Ui.SECONDARY);privacy.setGravity(Gravity.CENTER);Ui.add(root,privacy,10);
        Ui.setup(this,root);
        voice=new VoiceInput(this,panel,draft->{if(foreground)openEditor(draft);else pendingDraft=draft;},true);
        autoStart=state==null;
    }
    private void openEditor(VoiceDraft draft){
        Intent intent=new Intent(this,AddReminderActivity.class);
        if(draft!=null)intent.putExtra("voiceDraft",draft.toJson());
        startActivity(intent);finish();
    }
    @Override protected void onResume(){super.onResume();foreground=true;if(pendingDraft!=null){VoiceDraft draft=pendingDraft;pendingDraft=null;openEditor(draft);return;}if(autoStart){autoStart=false;voice.autoStart();}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==VoiceInput.MICROPHONE_REQUEST)voice.permissionResult(results);}
    @Override protected void onPause(){foreground=false;if(voice!=null)voice.pause();super.onPause();}
    @Override protected void onDestroy(){if(voice!=null)voice.destroy();super.onDestroy();}
}
