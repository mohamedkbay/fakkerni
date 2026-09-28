package com.fakkerni.reminder;

import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Pattern;

/** Treat model output as an untrusted draft; never schedule directly from it. */
final class VoiceDraft {
    final String transcript, title, phone, date, time, contactName;
    final boolean needsReview;
    String extractionIssue="";
    private VoiceDraft(String transcript, String title, String phone, String date, String time, String contactName, boolean review) {
        this.transcript = transcript; this.title = title; this.phone = phone;
        this.date = date; this.time = time; this.needsReview = review;
        this.contactName=contactName;
    }
    static VoiceDraft parse(String json) throws Exception {
        JSONObject object = new JSONObject(json);
        String transcript = string(object, "transcript", 6000);
        if (transcript.isEmpty()) throw new IllegalArgumentException("No speech");
        String title = string(object, "title", 250);
        String phone = string(object, "phone", 40);
        if (!phone.isEmpty() && !Pattern.matches("\\+?[0-9 ()-]{3,40}", phone)) phone = "";
        String date = string(object, "date", 10);
        String time = string(object, "time", 5);
        try { if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new Exception(); LocalDate.parse(date); }
        catch (Exception e) { date = ""; }
        try { if (!time.matches("[0-9]{2}:[0-9]{2}")) throw new Exception(); LocalTime.parse(time); }
        catch (Exception e) { time = ""; }
        VoiceDraft draft=new VoiceDraft(transcript, title, phone, date, time, string(object,"contact_name",120),
                object.optBoolean("needs_review", true) || date.isEmpty() || time.isEmpty() || title.isEmpty());
        String issue=object.optString("extraction_issue","");
        if(issue.equals("model_blocked")||issue.equals("extraction_failed"))draft.extractionIssue=issue;
        return draft;
    }
    private static String string(JSONObject obj, String key, int max) throws Exception {
        Object value = obj.opt(key);
        if (!(value instanceof String)) return "";
        String text = Digits.latin(((String) value).trim());
        if (text.length() > max) throw new IllegalArgumentException("Oversized field");
        return text;
    }
    String toJson(){
        try{return new JSONObject().put("transcript",transcript).put("title",title).put("phone",phone)
                .put("date",date).put("time",time).put("contact_name",contactName).put("needs_review",needsReview)
                .put("extraction_issue",extractionIssue).toString();}
        catch(org.json.JSONException e){throw new IllegalStateException(e);}
    }
    long futureTime(ZoneId zone, long now) {
        if (date.isEmpty() || time.isEmpty()) return -1;
        LocalDateTime local = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        // Both nonexistent and ambiguous wall-clock times need explicit user selection.
        if (zone.getRules().getValidOffsets(local).size() != 1) return -1;
        long millis = local.atZone(zone).toInstant().toEpochMilli();
        return millis > now ? millis : -1;
    }
}
