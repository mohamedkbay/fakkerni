package com.fakkerni.reminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

enum AlertTone {
    BELL("bell",R.raw.tone_bell,"جرس واضح","Bell"),
    FLOW("flow",R.raw.tone_flow,"نغمة ممتدة","Flow"),
    RECEIVED("received",R.raw.tone_received,"استلام","Received"),
    MESSAGE("message",R.raw.tone_message,"رسالة","Message"),
    SPARK("spark",R.raw.tone_spark,"ومضة","Spark"),
    SOFT_MESSAGE("soft_message",R.raw.tone_soft_message,"رسالة هادئة","Soft message"),
    MODERN("modern",R.raw.tone_modern,"حديثة","Modern"),
    QUICK("quick",R.raw.tone_quick,"سريعة","Quick"),
    BRIGHT("bright",R.raw.tone_bright,"مشرقة","Bright"),
    PHONE("phone",0,"نغمة من الهاتف","Phone sound");
    final String id,arabic,english;
    final int rawId;
    AlertTone(String id,int rawId,String arabic,String english){
        this.id=id;this.rawId=rawId;this.arabic=arabic;this.english=english;
    }
    private static SharedPreferences prefs(Context context){
        return context.getSharedPreferences("alert_tone",Context.MODE_PRIVATE);
    }
    static AlertTone selected(Context context){
        String saved=prefs(context).getString("tone","BELL");
        if("CHIME".equals(saved))return BELL;
        if("PULSE".equals(saved))return RECEIVED;
        try{return valueOf(saved);}catch(IllegalArgumentException e){return BELL;}
    }
    static void select(Context context,AlertTone tone){
        if(tone==PHONE)throw new IllegalArgumentException("Use selectPhone with a ringtone URI");
        prefs(context).edit().putString("previous_channel",selected(context).channelId(context))
                .putString("tone",tone.name()).apply();
    }
    static void selectPhone(Context context,Uri uri){
        if(uri==null)throw new IllegalArgumentException("No ringtone selected");
        prefs(context).edit().putString("previous_channel",selected(context).channelId(context))
                .putString("phone_uri",uri.toString()).putString("tone",PHONE.name()).apply();
    }
    static String previousChannelId(Context context){return prefs(context).getString("previous_channel","");}
    String channelId(Context context){
        return this==PHONE?phoneChannelIdFor(sound(context,this).toString()):"loud_reminders_"+id+"_v1";
    }
    static String phoneChannelIdFor(String uri){
        return "loud_reminders_phone_"+UUID.nameUUIDFromBytes(uri.getBytes(StandardCharsets.UTF_8))
                .toString().replace("-","");
    }
    static Uri sound(Context context,AlertTone tone){
        if(tone!=PHONE)return ToneProvider.uri(tone);
        String saved=prefs(context).getString("phone_uri","");
        return saved.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION):Uri.parse(saved);
    }
    String label(Context context){return Ui.t(context,arabic,english);}
    static AlertTone fromAssetId(String id){
        for(AlertTone tone:values())if(tone.rawId!=0&&tone.id.equals(id))return tone;
        return null;
    }
}
