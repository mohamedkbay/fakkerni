package com.fakkerni.reminder;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ReminderStore {
    private static final String PREFS = "fakkerni_reminders";
    private static final String KEY = "items";

    private ReminderStore() {}

    static synchronized List<Reminder> getAll(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "[]");
        List<Reminder> reminders = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                reminders.add(new Reminder(
                        item.getLong("id"),
                        item.getString("title"),
                        item.getLong("time"),
                        item.optString("phone", ""),
                        item.optString("contact", "")
                ));
            }
        } catch (JSONException ignored) {
            // A broken entry should never make the app unusable.
        }
        reminders.sort(Comparator.comparingLong(r -> r.eventTimeMillis));
        return reminders;
    }

    static synchronized Reminder find(Context context, long id) {
        for (Reminder reminder : getAll(context)) {
            if (reminder.id == id) return reminder;
        }
        return null;
    }

    static synchronized void save(Context context, Reminder reminder) {
        List<Reminder> items = getAll(context);
        items.removeIf(item -> item.id == reminder.id);
        items.add(reminder);
        write(context, items);
    }

    static synchronized void delete(Context context, long id) {
        List<Reminder> items = getAll(context);
        items.removeIf(item -> item.id == id);
        write(context, items);
    }

    private static void write(Context context, List<Reminder> items) {
        JSONArray array = new JSONArray();
        for (Reminder item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", item.id);
                object.put("title", item.title);
                object.put("time", item.eventTimeMillis);
                object.put("phone", item.phoneNumber);
                object.put("contact", item.contactName);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY, array.toString()).apply();
    }
}
