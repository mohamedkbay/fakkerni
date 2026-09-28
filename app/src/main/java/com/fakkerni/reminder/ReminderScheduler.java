package com.fakkerni.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class ReminderScheduler {
    static final long ONE_HOUR = 60L * 60L * 1000L;

    private ReminderScheduler() {}

    static boolean scheduleLockScreenTest(Context context) {
        AlarmManager alarms=context.getSystemService(AlarmManager.class);
        if(Build.VERSION.SDK_INT>=31&&!alarms.canScheduleExactAlarms())return false;
        long trigger=System.currentTimeMillis()+10_000L;
        Intent intent=new Intent(context,ReminderReceiver.class)
                .setData(android.net.Uri.parse("fakkerni://lock-screen-test"))
                .putExtra("id",-100L).putExtra("test",true)
                .putExtra("title",Ui.t(context,"اتصال بالعميل — تذكير تجريبي","Call the client — test reminder"))
                .putExtra("time",trigger+ONE_HOUR);
        PendingIntent pending=PendingIntent.getBroadcast(context,-100,intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try{setClock(context,alarms,trigger,pending,-100L);AlarmDiagnostics.scheduled(context,trigger,-100L);return true;}
        catch(SecurityException e){return false;}
    }

    static void schedule(Context context, Reminder reminder) {
        long now = System.currentTimeMillis();
        if (reminder.eventTimeMillis <= now) return;

        long alertTime = reminder.eventTimeMillis - ONE_HOUR;
        if (alertTime <= now) alertTime = now + 3_000L;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = pendingIntent(context, reminder);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alertTime, pendingIntent);
            } else {
                setClock(context,alarmManager,alertTime,pendingIntent,reminder.id);
            }
        } catch(SecurityException revoked) {
            // Permission can change after the check. Keep a best-effort alert instead of crashing.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,alertTime,pendingIntent);
        }
        AlarmDiagnostics.scheduled(context,alertTime,reminder.id);
    }

    private static void setClock(Context context,AlarmManager alarms,long trigger,PendingIntent operation,long id) {
        // User-created, audible reminders: expose the next alarm to Android and avoid idle-alarm quotas.
        Intent show=new Intent(context,id>0?AddReminderActivity.class:AiSettingsActivity.class)
                .putExtra("id",id).setData(android.net.Uri.parse("fakkerni://scheduled/"+id));
        PendingIntent showIntent=PendingIntent.getActivity(context,0,show,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        alarms.setAlarmClock(new AlarmManager.AlarmClockInfo(trigger,showIntent),operation);
    }

    static void cancel(Context context, long id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(id),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    static void rescheduleAll(Context context) {
        for (Reminder reminder : ReminderStore.getAll(context)) {
            schedule(context, reminder);
        }
    }

    private static PendingIntent pendingIntent(Context context, Reminder reminder) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .putExtra("id", reminder.id)
                .putExtra("title", reminder.title)
                .putExtra("time", reminder.eventTimeMillis)
                .putExtra("phone", reminder.phoneNumber);
        return PendingIntent.getBroadcast(
                context,
                requestCode(reminder.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int requestCode(long id) {
        return (int) (id ^ (id >>> 32));
    }
}
