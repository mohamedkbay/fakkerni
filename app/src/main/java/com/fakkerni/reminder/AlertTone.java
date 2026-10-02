package com.fakkerni.reminder;

import android.content.Context;
import android.net.Uri;

enum AlertTone {
    CHIME("loud_reminders_chime_v1","reminder_chime"),
    PULSE("loud_reminders_pulse_v1","reminder_pulse");
    final String channelId,resource;
    AlertTone(String channelId,String resource){this.channelId=channelId;this.resource=resource;}
    static AlertTone selected(Context context){
        String saved=context.getSharedPreferences("alert_tone",Context.MODE_PRIVATE).getString("tone","CHIME");
        try{return valueOf(saved);}catch(IllegalArgumentException e){return CHIME;}
    }
    static void select(Context context,AlertTone tone){context.getSharedPreferences("alert_tone",Context.MODE_PRIVATE)
            .edit().putString("tone",tone.name()).apply();}
    static Uri sound(Context context,AlertTone tone){
        return Uri.parse("android.resource://"+context.getPackageName()+"/raw/"+tone.resource);
    }
    String label(Context context){
        switch(this){
            case CHIME:return Ui.t(context,"رنين هادئ","Chime");
            case PULSE:return Ui.t(context,"نبض واضح","Pulse");
            default:return Ui.t(context,"رنين هادئ","Chime");
        }
    }
}
