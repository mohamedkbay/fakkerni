package com.fakkerni.reminder;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddReminderActivity extends Activity {
    private final Calendar selected = Calendar.getInstance();
    private EditText reminderText;
    private EditText phoneText;
    private TextView todayChip;
    private TextView tomorrowChip;
    private TextView dateChip;
    private TextView timeButton;
    private long editingId = -1L;
    private VoiceInput voice;
    private boolean voiceDraft, dateNeedsChoice, timeNeedsChoice;
    private String contactName = "", contactNumber = "";
    private static final int PICK_CONTACT = 63;
    private static final int READ_CONTACTS = 64;
    private String pendingContact="";
    private int contactGeneration;
    private final java.util.concurrent.ExecutorService contactsWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private String appliedMode;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.theme(this);super.onCreate(savedInstanceState);
        Design.editor(this);
        appliedMode=Appearance.mode(this);

        reminderText = findViewById(R.id.reminderText);
        phoneText = findViewById(R.id.phoneText);
        todayChip = findViewById(R.id.todayChip);
        tomorrowChip = findViewById(R.id.tomorrowChip);
        dateChip = findViewById(R.id.dateChip);
        timeButton = findViewById(R.id.timeButton);

        selected.add(Calendar.HOUR_OF_DAY, 2);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);

        editingId = getIntent().getLongExtra("id", -1L);
        if (editingId != -1L) {
            Reminder existing = ReminderStore.find(this, editingId);
            if (existing != null) {
                reminderText.setText(existing.title);
                phoneText.setText(existing.phoneNumber);
                contactName = existing.contactName;
                contactNumber = existing.phoneNumber;
                selected.setTimeInMillis(existing.eventTimeMillis);
                ((TextView) findViewById(R.id.screenTitle)).setText(R.string.edit_reminder);
            }
        }

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        todayChip.setOnClickListener(v -> chooseRelativeDay(0));
        tomorrowChip.setOnClickListener(v -> chooseRelativeDay(1));
        dateChip.setOnClickListener(v -> openDatePicker());
        timeButton.setOnClickListener(v -> openTimePicker());
        findViewById(R.id.saveButton).setOnClickListener(v -> saveReminder());
        findViewById(R.id.aiSettingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, AiSettingsActivity.class)));
        findViewById(R.id.contactButton).setOnClickListener(v -> openContactSearch());
        phoneText.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (!Digits.latin(s.toString()).equals(contactNumber)) { contactName=""; contactNumber=""; }
                refreshContact();
            }
            public void afterTextChanged(android.text.Editable s) {}
        });

        android.widget.LinearLayout voicePanel = findViewById(R.id.voicePanel);
        voice = new VoiceInput(this, voicePanel, this::applyVoiceDraft);
        if (savedInstanceState != null) {
            selected.setTimeInMillis(savedInstanceState.getLong("selected", selected.getTimeInMillis()));
            voiceDraft = savedInstanceState.getBoolean("voiceDraft");
            dateNeedsChoice = savedInstanceState.getBoolean("dateNeedsChoice");
            timeNeedsChoice = savedInstanceState.getBoolean("timeNeedsChoice");
            String transcript = savedInstanceState.getString("transcript", "");
            TextView view = findViewById(R.id.transcriptView);
            view.setText(transcript);
            view.setVisibility(transcript.isEmpty() ? View.GONE : View.VISIBLE);
            contactName = savedInstanceState.getString("contactName", "");
            contactNumber = savedInstanceState.getString("contactNumber", "");
            pendingContact = savedInstanceState.getString("pendingContact", "");
        }

        refreshSelection();
        refreshContact();
        if(voiceDraft)markReview();
        if(savedInstanceState==null&&getIntent().hasExtra("voiceDraft")){
            try{applyVoiceDraft(VoiceDraft.parse(getIntent().getStringExtra("voiceDraft")));}
            catch(Exception e){Toast.makeText(this,R.string.voice_parse_error,Toast.LENGTH_LONG).show();}
        }
    }

    private void refreshContact() {
        ((TextView)findViewById(R.id.contactName)).setText(contactName.isEmpty()
                ? Ui.t(this,"ابحث بالاسم العربي أو الإنجليزي أو الرقم","Search Arabic / English names or numbers") : contactName);
    }

    private void openContactSearch(){
        EditText query=new EditText(this);query.setSingleLine(true);
        query.setHint(Ui.t(this,"اكتب اسم أو رقم","Name or number"));
        query.setTypeface(getResources().getFont(R.font.cairo));
        int p=Ui.dp(this,20);query.setPadding(p,p,p,p);
        new android.app.AlertDialog.Builder(this)
                .setTitle(Ui.t(this,"ابحث في جهات اتصالك","Search your contacts"))
                .setView(query)
                .setNegativeButton(Ui.t(this,"قائمة الهاتف","Phone contacts"),(d,w)->openSystemContactPicker())
                .setPositiveButton(Ui.t(this,"بحث","Search"),(d,w)->{
                    String search=query.getText().toString().trim();
                    if(search.isEmpty()){openSystemContactPicker();return;}
                    contactName="";contactNumber="";phoneText.setText("");refreshContact();
                    matchSpokenContact(search);
                }).show();
    }
    private void openSystemContactPicker(){
        try{startActivityForResult(new Intent(Intent.ACTION_PICK,
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI),PICK_CONTACT);}
        catch(android.content.ActivityNotFoundException e){
            Toast.makeText(this,Ui.t(this,"ما فيش تطبيق جهات اتصال؛ اكتب الرقم هنا","No contacts app. Enter a number instead."),Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_CONTACT || result != RESULT_OK || data == null || data.getData() == null) return;
        // The system grants access to this selected row only; no address-book upload or broad permission.
        try (android.database.Cursor cursor = getContentResolver().query(data.getData(),
                new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                contactNumber = Digits.latin(cursor.getString(0));
                contactName = Digits.latin(cursor.getString(1));
                phoneText.setText(contactNumber); refreshContact();
            }
        } catch (RuntimeException e) {
            Toast.makeText(this,Ui.t(this,"تعذّر قراءة الرقم. تقدر تكتبه يدوياً","Could not read this number. You can enter it manually."),Toast.LENGTH_LONG).show();
        }
    }

    private void chooseRelativeDay(int daysAhead) {
        dateNeedsChoice = false;
        Calendar day = Calendar.getInstance();
        day.add(Calendar.DAY_OF_YEAR, daysAhead);
        selected.set(Calendar.YEAR, day.get(Calendar.YEAR));
        selected.set(Calendar.MONTH, day.get(Calendar.MONTH));
        selected.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH));
        refreshSelection();
    }

    private void matchSpokenContact(String name){
        pendingContact=name;
        if(checkSelfPermission(android.Manifest.permission.READ_CONTACTS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
            new android.app.AlertDialog.Builder(this)
                    .setMessage(Ui.t(this,"نبحث عن «","Look up “")+name+Ui.t(this,"» في جهات اتصالك؟ البحث محلي وما نرفعوش دفتر الأرقام.","” in your contacts? Matching stays on this phone; your address book is not uploaded."))
                    .setPositiveButton(Ui.t(this,"السماح بالبحث","Allow lookup"),(d,w)->requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},READ_CONTACTS))
                    .setNegativeButton(Ui.t(this,"مش توا","Not now"),(d,w)->pendingContact="").show();
            return;
        }
        final int generation=++contactGeneration;
        final String task=reminderText.getText().toString();
        contactsWorker.submit(()->{
            try{
                java.util.List<LocalContacts.Match> matches=LocalContacts.find(getApplicationContext(),name);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed()||generation!=contactGeneration||!phoneText.getText().toString().isEmpty()||!task.equals(reminderText.getText().toString()))return;
                    pendingContact="";
                    if(matches.isEmpty()){
                        Toast.makeText(this,Ui.t(this,"ما لقيناش الاسم. اضغط زر الهاتف واختاره يدوياً.","No matching name. Tap the phone button to choose manually."),Toast.LENGTH_LONG).show();return;
                    }
                    // A unique match fills only the draft; final Save remains the user's approval.
                    if(matches.size()==1){
                        LocalContacts.Match match=matches.get(0);contactNumber=match.phone;contactName=match.name;
                        phoneText.setText(contactNumber);refreshContact();return;
                    }
                    String[] labels=new String[matches.size()];
                    for(int i=0;i<labels.length;i++)labels[i]=matches.get(i).name+"\n"+matches.get(i).phone;
                    new android.app.AlertDialog.Builder(this).setTitle(Ui.t(this,"اختَر رقم ","Choose a number for ")+name)
                            .setItems(labels,(d,which)->{
                                if(generation!=contactGeneration||!task.equals(reminderText.getText().toString()))return;
                                LocalContacts.Match match=matches.get(which);
                                contactNumber=match.phone;contactName=match.name;
                                phoneText.setText(contactNumber);refreshContact();
                            }).setNegativeButton(android.R.string.cancel,null).show();
                });
            }catch(RuntimeException e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())Toast.makeText(this,Ui.t(this,"تعذّر البحث؛ اختَر الرقم بزر الهاتف.","Lookup unavailable; choose with the phone button."),Toast.LENGTH_LONG).show();});}
        });
    }

    private void openDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    dateNeedsChoice = false;
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, day);
                    refreshSelection();
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(startOfToday());
        dialog.show();
    }

    private void openTimePicker() {
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    timeNeedsChoice = false;
                    selected.set(Calendar.HOUR_OF_DAY, hour);
                    selected.set(Calendar.MINUTE, minute);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    refreshSelection();
                },
                selected.get(Calendar.HOUR_OF_DAY),
                selected.get(Calendar.MINUTE),
                false
        ).show();
    }

    private void refreshSelection() {
        long selectedDay = startOfDay(selected.getTimeInMillis());
        long today = startOfToday();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTimeInMillis(today);
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        long tomorrowStart = tomorrow.getTimeInMillis();
        todayChip.setSelected(selectedDay == today);
        tomorrowChip.setSelected(selectedDay == tomorrowStart);
        dateChip.setSelected(selectedDay != today && selectedDay != tomorrowStart);
        dateChip.setText(Ui.date("d MMM", selected.getTimeInMillis()));
        timeButton.setText(Ui.date("h:mm a", selected.getTimeInMillis()));
        if (dateNeedsChoice) {
            todayChip.setSelected(false); tomorrowChip.setSelected(false); dateChip.setSelected(false);
            dateChip.setText(R.string.choose_date);
        }
        if (timeNeedsChoice) timeButton.setText(R.string.choose_time);
        TextView[] choices={todayChip,tomorrowChip,dateChip};
        for(TextView chip:choices){
            chip.setBackground(Ui.rounded(chip.isSelected()?Ui.YELLOW:Ui.INK,18,this));
            chip.setTextColor(chip.isSelected()?Ui.BG:Ui.WHITE);
        }
    }

    @Override protected void onResume(){super.onResume();
        if(appliedMode!=null&&!Appearance.mode(this).equals(appliedMode))recreate();
    }

    private void saveReminder() {
        if (dateNeedsChoice) {
            Toast.makeText(this, R.string.voice_need_date, Toast.LENGTH_LONG).show(); openDatePicker(); return;
        }
        if (timeNeedsChoice) {
            Toast.makeText(this, R.string.voice_need_time, Toast.LENGTH_LONG).show(); openTimePicker(); return;
        }
        String title = reminderText.getText().toString().trim();
        String phone = phoneText.getText().toString().trim();
        if (title.isEmpty()) {
            reminderText.setError(getString(R.string.write_error));
            reminderText.requestFocus();
            return;
        }
        if (selected.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, R.string.past_error, Toast.LENGTH_LONG).show();
            return;
        }

        long id = editingId == -1L ? System.currentTimeMillis() : editingId;
        Reminder reminder = new Reminder(id, title, selected.getTimeInMillis(), phone, contactName);
        if (editingId != -1L) ReminderScheduler.cancel(this, editingId);
        ReminderStore.save(this, reminder);
        ReminderScheduler.schedule(this, reminder);

        boolean closeSoon = reminder.eventTimeMillis - System.currentTimeMillis() < ReminderScheduler.ONE_HOUR;
        Toast.makeText(
                this,
                closeSoon ? getString(R.string.saved_close) : getString(R.string.saved),
                Toast.LENGTH_LONG
        ).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!manager.canScheduleExactAlarms()) {
                try {
                    Intent settings = new Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())
                    );
                    startActivity(settings);
                } catch (Exception ignored) {
                }
            }
        }
        finish();
    }

    private static long startOfToday() {
        return startOfDay(System.currentTimeMillis());
    }

    private void applyVoiceDraft(VoiceDraft draft) {
        voiceDraft = true;
        markReview();
        reminderText.setText(draft.title.isEmpty() ? draft.transcript : draft.title);
        phoneText.setText(draft.phone);
        long proposed = draft.futureTime(java.time.ZoneId.systemDefault(), System.currentTimeMillis());
        dateNeedsChoice = timeNeedsChoice = proposed < 0 || draft.needsReview;
        if (!dateNeedsChoice) selected.setTimeInMillis(proposed);
        else if (!draft.date.isEmpty()) {
            java.time.LocalDate date = java.time.LocalDate.parse(draft.date);
            if (!date.isBefore(java.time.LocalDate.now()) && !draft.needsReview) {
                selected.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
                dateNeedsChoice = false;
            }
        }
        TextView transcript = findViewById(R.id.transcriptView);
        transcript.setText(getString(R.string.voice_result, draft.transcript,
                getString(dateNeedsChoice || timeNeedsChoice ? R.string.voice_missing : R.string.voice_review)));
        if(!draft.extractionIssue.isEmpty())transcript.append("\n"+Ui.t(this,
                draft.extractionIssue.equals("model_blocked")?"الصوت تحوّل لنص. فعّل openai/gpt-oss-20b في إعدادات مشروع Groq لفهم الموعد؛ تقدر تختاره يدوياً توا.":"الصوت تحوّل لنص، لكن فهم الموعد تعذّر. اختار الموعد يدوياً.",
                draft.extractionIssue.equals("model_blocked")?"Audio transcribed. Enable openai/gpt-oss-20b in your Groq project to extract the time. You can select it manually now.":"Audio transcribed, but time extraction failed. Select the time manually."));
        transcript.setVisibility(View.VISIBLE);
        refreshSelection();
        ++contactGeneration;
        if(!draft.contactName.isEmpty()&&draft.phone.isEmpty())matchSpokenContact(draft.contactName);
    }

    private void markReview(){
        ((TextView)findViewById(R.id.screenTitle)).setText(Ui.t(this,"راجع ووافق","Review & confirm"));
        ((TextView)findViewById(R.id.saveButton)).setText(Ui.t(this,"موافق، احفظ التذكير","Confirm reminder"));
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == VoiceInput.MICROPHONE_REQUEST) voice.permissionResult(results);
        if(request==READ_CONTACTS&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED&&!pendingContact.isEmpty())matchSpokenContact(pendingContact);
    }
    @Override protected void onPause() {
        if (voice != null) voice.pause();
        super.onPause();
    }
    @Override protected void onDestroy() {
        if (voice != null) voice.destroy();
        contactsWorker.shutdownNow();
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putLong("selected", selected.getTimeInMillis());
        state.putBoolean("voiceDraft", voiceDraft);
        state.putBoolean("dateNeedsChoice", dateNeedsChoice);
        state.putBoolean("timeNeedsChoice", timeNeedsChoice);
        state.putString("transcript", ((TextView) findViewById(R.id.transcriptView)).getText().toString());
        state.putString("contactName", contactName);
        state.putString("contactNumber", contactNumber);
        state.putString("pendingContact",pendingContact);
        super.onSaveInstanceState(state);
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
}
