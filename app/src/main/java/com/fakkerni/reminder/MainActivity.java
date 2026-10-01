package com.fakkerni.reminder;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 41;
    private LinearLayout reminderList;
    private LinearLayout permissionBanner;
    private TextView permissionMessage;
    private Button permissionButton;
    private TextView filterToday;
    private TextView filterWeek;
    private TextView filterAll;
    private int activeFilter = 1;
    private String appliedMode;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.theme(this);super.onCreate(savedInstanceState);
        Design.main(this);
        appliedMode=Appearance.mode(this);
        ReminderReceiver.createChannel(this);

        reminderList = findViewById(R.id.reminderList);
        permissionBanner = findViewById(R.id.permissionBanner);
        permissionMessage = findViewById(R.id.permissionMessage);
        permissionButton = findViewById(R.id.permissionButton);
        filterToday = findViewById(R.id.filterToday);
        filterWeek = findViewById(R.id.filterWeek);
        filterAll = findViewById(R.id.filterAll);

        ((TextView) findViewById(R.id.todayDate)).setText(
                Ui.date("EEEE, d MMMM", System.currentTimeMillis())
        );

        View.OnClickListener openAdd = v ->
                startActivity(new Intent(this, AssistantActivity.class));
        findViewById(R.id.addButton).setOnClickListener(openAdd);
        findViewById(R.id.topAddButton).setOnClickListener(openAdd);
        findViewById(R.id.aiSettingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, AiSettingsActivity.class)));
        findViewById(R.id.languageButton).setOnClickListener(v -> {
            LocaleHelper.toggle(this);
            recreate();
        });
        filterToday.setOnClickListener(v -> selectFilter(0));
        filterWeek.setOnClickListener(v -> selectFilter(1));
        filterAll.setOnClickListener(v -> selectFilter(2));
        styleFilters();
        if(savedInstanceState==null&&Intent.ACTION_MAIN.equals(getIntent().getAction()))
            startActivity(new Intent(this,AssistantActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!Appearance.mode(this).equals(appliedMode)){recreate();return;}
        renderReminders();
        renderPermissionBanner();
    }

    private void selectFilter(int filter) {
        activeFilter = filter;
        styleFilters();
        renderReminders();
    }

    private void styleFilters() {
        TextView[] filters = {filterToday, filterWeek, filterAll};
        for (int i = 0; i < filters.length; i++) {
            filters[i].setSelected(i == activeFilter);
            filters[i].setTextColor(i == activeFilter ? Ui.BG : Ui.WHITE);
            filters[i].setBackground(i == activeFilter ? Ui.rounded(Ui.YELLOW,28,this)
                    : Ui.rounded(Ui.SURFACE,28,this));
        }
    }

    private void renderReminders() {
        reminderList.removeAllViews();
        long now = System.currentTimeMillis();
        List<Reminder> all = ReminderStore.getAll(this);
        all.removeIf(item -> item.eventTimeMillis < now);

        long weekEnd = now + 7L * 24L * 60L * 60L * 1000L;
        int weekCount = 0;
        for (Reminder reminder : all) {
            if (reminder.eventTimeMillis <= weekEnd) weekCount++;
        }
        ((TextView) findViewById(R.id.summaryCount)).setText(
                Digits.latin(getString(R.string.scheduled_count, weekCount)));

        List<Reminder> visible = new ArrayList<>();
        long today = startOfDay(now);
        long tomorrow = nextDay(today);
        for (Reminder reminder : all) {
            boolean include = activeFilter == 2
                    || (activeFilter == 1 && reminder.eventTimeMillis <= weekEnd)
                    || (activeFilter == 0 && reminder.eventTimeMillis >= today
                    && reminder.eventTimeMillis < tomorrow);
            if (include) visible.add(reminder);
        }

        if (visible.isEmpty()) {
            LinearLayout empty=Ui.card(this,Ui.INK);
            Ui.EmptyClock clock=new Ui.EmptyClock(this);empty.addView(clock,new LinearLayout.LayoutParams(-1,dp(160)));
            TextView title=Ui.text(this,getString(R.string.nothing_today),20,Ui.WHITE);title.setGravity(Gravity.CENTER);
            Ui.add(empty,title,8);
            TextView hint=Ui.text(this,getString(R.string.nothing_subtitle),12,Ui.SECONDARY);hint.setGravity(Gravity.CENTER);
            Ui.add(empty,hint,8);addWithTopMargin(empty,12);
            return;
        }

        long lastDay = Long.MIN_VALUE;
        int cardIndex = 0;
        for (Reminder reminder : visible) {
            long day = startOfDay(reminder.eventTimeMillis);
            if (day != lastDay) {
                TextView heading = Ui.text(this,dayLabel(day),15,Ui.WHITE);
                heading.setTypeface(getResources().getFont(R.font.cairo), Typeface.BOLD);
                addWithTopMargin(heading, lastDay == Long.MIN_VALUE ? 4 : 20);
                lastDay = day;
            }
            addWithTopMargin(reminderCard(reminder, cardIndex++), 8);
        }
    }

    private View reminderCard(Reminder reminder, int index) {
        LinearLayout card = Ui.card(this,index % 2 == 0 ? Ui.INK : Ui.SURFACE);
        card.setOnClickListener(v -> startActivity(
                new Intent(this, AddReminderActivity.class).putExtra("id", reminder.id)));
        LinearLayout head=Ui.row(this);
        MorphIconView mark=Ui.icon(this,reminder.phoneNumber.isEmpty()?"bell":"phone",Ui.SURFACE,
                Ui.t(this,"تذكير","Reminder"),48);mark.setClickable(false);mark.setFocusable(false);head.addView(mark);
        LinearLayout labels=Ui.column(this);labels.setPadding(dp(12),0,dp(8),0);
        TextView title=Ui.text(this,reminder.title,17,Ui.WHITE);title.setMaxLines(3);
        labels.addView(title);
        Ui.add(labels,Ui.text(this,reminder.contactName.isEmpty()?Ui.t(this,"تذكير شخصي","Personal reminder"):reminder.contactName,12,Ui.SECONDARY),3);
        head.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=Ui.text(this,Ui.t(this,"● قادم","● Upcoming"),10,Ui.BG);
        badge.setBackground(Ui.rounded(Ui.YELLOW,22,this));badge.setPadding(dp(10),dp(5),dp(10),dp(5));head.addView(badge);
        card.addView(head);
        LinearLayout details=Ui.row(this);
        String[] captions={Ui.t(this,"اليوم","Date"),Ui.t(this,"الساعة","Time"),Ui.t(this,"التنبيه","Alert")};
        String[] values={Ui.date("d MMM",reminder.eventTimeMillis),Ui.date("h:mm a",reminder.eventTimeMillis),Ui.t(this,"قبل 1 ساعة","1 hour before")};
        for(int i=0;i<3;i++){
            LinearLayout cell=Ui.column(this);cell.addView(Ui.text(this,captions[i],11,Ui.SECONDARY));
            Ui.add(cell,Ui.text(this,values[i],13,Ui.WHITE),3);details.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
        }
        Ui.add(card,details,20);Ui.add(card,Ui.divider(this),12);
        LinearLayout footer=Ui.row(this);
        TextView phone=Ui.text(this,reminder.phoneNumber.isEmpty()?Ui.t(this,"اضغط لتعديل التذكير","Tap to edit reminder"):reminder.phoneNumber,11,Ui.SECONDARY);
        phone.setPadding(dp(8),0,dp(8),0);
        phone.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        if(!reminder.phoneNumber.isEmpty())phone.setTextDirection(View.TEXT_DIRECTION_LTR);
        footer.addView(phone,new LinearLayout.LayoutParams(0,-2,1));
        if(!reminder.phoneNumber.isEmpty()){
            MorphIconView call=Ui.icon(this,"phone",Ui.YELLOW,getString(R.string.call_description,reminder.phoneNumber),48);
            call.setOnClickListener(v->{call.setIcon("check",true);openDialer(reminder.phoneNumber);});footer.addView(call);
        }
        MorphIconView delete=Ui.icon(this,"trash",android.graphics.Color.TRANSPARENT,getString(R.string.delete),48);
        delete.setOnClickListener(v -> confirmDelete(reminder));
        footer.addView(delete);Ui.add(card,footer,2);
        return card;
    }

    private void openDialer(String phoneNumber) {
        Intent dial = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + Uri.encode(phoneNumber)));
        startActivity(dial);
    }

    private void confirmDelete(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(reminder.title)
                .setNegativeButton(R.string.keep_it, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    ReminderScheduler.cancel(this, reminder.id);
                    ReminderStore.delete(this, reminder.id);
                    renderReminders();
                }).show();
    }

    private void renderPermissionBanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionBanner.setVisibility(View.VISIBLE);
            permissionMessage.setText(R.string.enable_notifications);
            permissionButton.setOnClickListener(v -> requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST));
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!manager.canScheduleExactAlarms()) {
                permissionBanner.setVisibility(View.VISIBLE);
                permissionMessage.setText(R.string.enable_exact_alarm);
                permissionButton.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName())));
                    } catch (Exception ignored) {}
                });
                return;
            }
        }
        android.app.NotificationManager notifications=getSystemService(android.app.NotificationManager.class);
        android.app.NotificationChannel channel=notifications.getNotificationChannel(ReminderReceiver.CHANNEL_ID);
        if(!notifications.areNotificationsEnabled()||channel==null||channel.getImportance()<android.app.NotificationManager.IMPORTANCE_DEFAULT||channel.getSound()==null){
            permissionBanner.setVisibility(View.VISIBLE);
            permissionMessage.setText(Ui.t(this,"راجع صوت الإشعارات باش تسمع تذكيراتك","Check notification sound so you can hear reminders"));
            permissionButton.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,ReminderReceiver.CHANNEL_ID)));
            return;
        }
        permissionBanner.setVisibility(View.GONE);
    }

    private void addWithTopMargin(View view, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topDp);
        reminderList.addView(view, params);
    }

    private String dayLabel(long day) {
        long today = startOfDay(System.currentTimeMillis());
        if (day == today) return getString(R.string.today);
        if (day == nextDay(today)) return getString(R.string.tomorrow);
        return new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date(day));
    }

    private static long startOfDay(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static long nextDay(long day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(day);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return calendar.getTimeInMillis();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
