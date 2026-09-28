package com.fakkerni.reminder;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.os.*;

/** Local delivery metadata only; never stores reminder text, phone numbers or credentials. */
final class AlarmDiagnostics {
    private static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("alarm_diagnostics",Context.MODE_PRIVATE);
    }
    static void scheduled(Context c,long at,long id) {
        prefs(c).edit().putLong("scheduled",at).putLong("id",id)
                .remove("received").remove("posted").remove("blocked").commit();
    }
    static void received(Context c,long id) {
        if(prefs(c).getLong("id",Long.MIN_VALUE)!=id)return;
        prefs(c).edit().putLong("received",System.currentTimeMillis()).commit();
    }
    static void outcome(Context c,long id,boolean posted) {
        if(prefs(c).getLong("id",Long.MIN_VALUE)!=id)return;
        prefs(c).edit().putLong(posted?"posted":"blocked",System.currentTimeMillis()).commit();
    }
    static String report(Context c) {
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        NotificationChannel ch=nm.getNotificationChannel(ReminderReceiver.CHANNEL_ID);
        AlarmManager am=c.getSystemService(AlarmManager.class);
        AudioManager audio=c.getSystemService(AudioManager.class);
        boolean exact=Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms();
        boolean enabled=nm.areNotificationsEnabled()&&ch!=null&&ch.getImportance()>0;
        boolean restricted=Build.VERSION.SDK_INT>=28&&c.getSystemService(ActivityManager.class).isBackgroundRestricted();
        String s=Ui.t(c,"الهاتف: ","Phone: ")+Build.MANUFACTURER+" "+Build.MODEL+" · Android "+Build.VERSION.RELEASE;
        s+="\n"+Ui.t(c,"الإشعارات: ","Notifications: ")+yes(c,enabled);
        s+="\n"+Ui.t(c,"المنبّهات الدقيقة: ","Exact alarms: ")+yes(c,exact);
        s+="\n"+Ui.t(c,"صوت القناة: ","Channel sound: ")+yes(c,ch!=null&&ch.getSound()!=null&&ch.getImportance()>=3);
        s+="\n"+Ui.t(c,"مستوى صوت المنبّه: ","Alarm volume: ")+audio.getStreamVolume(AudioManager.STREAM_ALARM)+"/"+audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        s+="\n"+Ui.t(c,"تقييد الخلفية: ","Background restricted: ")+yes(c,restricted);
        s+="\n"+Ui.t(c,"عدم الإزعاج: ","Do Not Disturb: ")+yes(c,nm.getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_ALL);
        long scheduled=prefs(c).getLong("scheduled",0),received=prefs(c).getLong("received",0),posted=prefs(c).getLong("posted",0),blocked=prefs(c).getLong("blocked",0);
        if(scheduled>0)s+="\n\n"+Ui.t(c,"آخر تنبيه جُدول: ","Last scheduled alert: ")+Ui.date("d MMM HH:mm:ss",scheduled);
        if(received>0)s+="\n"+Ui.t(c,"وصل للتطبيق: ","Receiver ran: ")+Ui.date("HH:mm:ss",received);
        if(posted>0)s+="\n"+Ui.t(c,"نُشر الإشعار. ظهوره والصوت يخضعان لإعدادات الهاتف.","Notification posted. Visibility and sound depend on phone settings.");
        else if(blocked>0)s+="\n"+Ui.t(c,"نشر الإشعار ممنوع. راجع إذن الإشعارات والقناة.","Notification blocked. Check app and channel permission.");
        else if(scheduled>0&&received==0)s+="\n"+(System.currentTimeMillis()>scheduled+5000
                ?Ui.t(c,"لم يصل التنبيه للتطبيق بعد الموعد. راجع المنبّهات الدقيقة والبطارية والتشغيل التلقائي.","Alarm has not reached the app after its time. Check exact alarms, battery and auto-start settings.")
                :Ui.t(c,"في انتظار موعد التنبيه.","Waiting for the alarm time."));
        return Digits.latin(s);
    }
    private static String yes(Context c,boolean value){return value?Ui.t(c,"نعم","Yes"):Ui.t(c,"لا","No");}
}
