package com.fakkerni.reminder;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

/** RemoteViews only: safe for System UI to render while our process is not running. */
final class NotificationCard {
    static RemoteViews create(Context c, String title, String when, String label, String phone, boolean expanded) {
        RemoteViews view=new RemoteViews(c.getPackageName(),expanded?R.layout.notification_expanded:R.layout.notification_compact);
        int direction=c.getResources().getConfiguration().getLayoutDirection();
        view.setInt(R.id.notificationCard,"setLayoutDirection",direction);
        view.setTextViewText(R.id.notificationTitle,Digits.latin(title));
        view.setTextViewText(R.id.notificationTime,Digits.latin(when));
        view.setContentDescription(R.id.notificationCard,Digits.latin(title+". "+when));
        if(expanded){
            view.setTextViewText(R.id.notificationLabel,label);
            view.setTextViewText(R.id.notificationPhone,Digits.latin(phone));
            view.setViewVisibility(R.id.notificationPhone,phone.isEmpty()?View.GONE:View.VISIBLE);
        }
        return view;
    }
}
