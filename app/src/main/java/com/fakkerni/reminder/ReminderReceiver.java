package com.fakkerni.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL_ID = "loud_reminders_v1";

    @Override
    public void onReceive(Context context, Intent intent) {
        Context localizedContext = LocaleHelper.wrap(context);
        createChannel(localizedContext);
        long id = intent.getLongExtra("id", System.currentTimeMillis());
        AlarmDiagnostics.received(context,id);
        String title = intent.getStringExtra("title");
        long time = intent.getLongExtra("time", 0L);
        String phone = intent.getStringExtra("phone");

        Intent openApp = new Intent(context, id > 0 ? AddReminderActivity.class : MainActivity.class)
                .putExtra("id",id).setData(Uri.parse("fakkerni://reminder/"+id))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String eventTime = Ui.date("EEE d MMM • h:mm a",time);
        title=Digits.latin(title);
        phone=Digits.latin(phone);
        if(title.trim().isEmpty())title=localizedContext.getString(R.string.app_name);
        String label=intent.getBooleanExtra("test",false)?Ui.t(localizedContext,"تجربة شاشة القفل","Lock-screen test")
                :time-System.currentTimeMillis()<59*60_000L?Ui.t(localizedContext,"موعدك قريب","Coming up soon")
                :localizedContext.getString(R.string.hour_left);
        Notification.Builder builder = new Notification.Builder(localizedContext, CHANNEL_ID);

        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(eventTime)
                .setSubText(label)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setCustomContentView(NotificationCard.create(localizedContext,title,eventTime,label,phone,false))
                .setCustomHeadsUpContentView(NotificationCard.create(localizedContext,title,eventTime,label,phone,false))
                .setCustomBigContentView(NotificationCard.create(localizedContext,title,eventTime,label,phone,true))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(Ui.INK)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

        if (phone != null && !phone.trim().isEmpty()) {
            Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone)));
            PendingIntent callIntent = PendingIntent.getActivity(
                    context,
                    ((int) (id ^ (id >>> 32))) + 1,
                    dial,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.addAction(new Notification.Action.Builder(
                    R.drawable.ic_phone,
                    localizedContext.getString(R.string.call_now),
                    callIntent
            ).build());
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel=manager.getNotificationChannel(CHANNEL_ID);
        if (manager.areNotificationsEnabled() && channel!=null && channel.getImportance()>0) {
            try {
                manager.notify((int) (id ^ (id >>> 32)), builder.build());
                AlarmDiagnostics.outcome(context,id,true);
            } catch (SecurityException ignored) { AlarmDiagnostics.outcome(context,id,false); }
        } else AlarmDiagnostics.outcome(context,id,false);
    }

    static void createChannel(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            existing.setName(context.getString(R.string.channel_name));
            existing.setDescription(context.getString(R.string.channel_description));
            manager.createNotificationChannel(existing);
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.channel_description));
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 800});
        channel.enableLights(true);
        channel.setLightColor(Ui.YELLOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setSound(
                alarmSound(),
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
        );
        manager.createNotificationChannel(channel);
    }

    private static Uri alarmSound() {
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        return sound != null ? sound : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }
}
