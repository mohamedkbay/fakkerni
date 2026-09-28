package com.fakkerni.reminder;

import org.junit.Test;
import org.json.JSONObject;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.Assert.*;

public class VoiceDraftTest {
    @Test public void assistantHandoffPreservesFieldsAndFailureReason() throws Exception {
        JSONObject json=new JSONObject(draft("2026-09-25","17:00",""));
        json.put("contact_name","أحمد");
        VoiceDraft first=VoiceDraft.parse(json.toString());first.extractionIssue="model_blocked";
        VoiceDraft restored=VoiceDraft.parse(first.toJson());
        assertEquals(first.title,restored.title);assertEquals("أحمد",restored.contactName);
        assertEquals("17:00",restored.time);assertEquals("model_blocked",restored.extractionIssue);
    }
    private String draft(String date, String time, String phone) throws Exception {
        return new JSONObject().put("transcript", "غدوة نتصل بأحمد الساعة خمسة العشية")
                .put("title", "نتصل بأحمد").put("date", date).put("time", time)
                .put("phone", phone).put("needs_review", false).toString();
    }
    @Test public void resolvesTripoliTimeWithoutSubtractingTheAlertHour() throws Exception {
        VoiceDraft d = VoiceDraft.parse(draft("2026-09-24", "17:00", "+218912345678"));
        assertEquals(Instant.parse("2026-09-24T15:00:00Z").toEpochMilli(),
                d.futureTime(ZoneId.of("Africa/Tripoli"), 0));
        assertEquals("+218912345678", d.phone);
        assertFalse(d.needsReview);
    }
    @Test public void missingTimeRequiresManualChoice() throws Exception {
        VoiceDraft d = VoiceDraft.parse(draft("2026-09-24", "", ""));
        assertTrue(d.needsReview);
        assertEquals(-1, d.futureTime(ZoneId.of("UTC"), 0));
    }
    @Test public void impossibleDateAndTimeAreNotNormalized() throws Exception {
        VoiceDraft d = VoiceDraft.parse(draft("2026-02-30", "25:00", ""));
        assertTrue(d.needsReview);
        assertEquals("", d.date);
        assertEquals("", d.time);
    }
    @Test public void rejectsPastEvent() throws Exception {
        VoiceDraft d = VoiceDraft.parse(draft("2026-01-01", "12:00", ""));
        assertEquals(-1, d.futureTime(ZoneId.of("UTC"), Instant.parse("2026-09-23T00:00:00Z").toEpochMilli()));
    }
    @Test public void refusesDstGapAndOverlap() throws Exception {
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        assertEquals(-1, VoiceDraft.parse(draft("2026-03-29", "02:30", "")).futureTime(berlin, 0));
        assertEquals(-1, VoiceDraft.parse(draft("2026-10-25", "02:30", "")).futureTime(berlin, 0));
    }
    @Test public void removesPhoneCommands() throws Exception {
        assertEquals("", VoiceDraft.parse(draft("2026-09-24", "17:00", "*123#")).phone);
    }
    @Test(expected = IllegalArgumentException.class) public void silenceCreatesNoTask() throws Exception {
        VoiceDraft.parse("{\"transcript\":\"\"}");
    }
    @Test public void missingReviewFlagDefaultsToReview() throws Exception {
        JSONObject obj = new JSONObject(draft("2026-09-24", "17:00", ""));
        obj.remove("needs_review");
        assertTrue(VoiceDraft.parse(obj.toString()).needsReview);
    }
    @Test public void parsesCompletedModelEnvelope() throws Exception {
        JSONObject part = new JSONObject().put("type","output_text").put("text", draft("2026-09-24", "17:00", ""));
        String envelope = new JSONObject().put("status","completed").put("output", new org.json.JSONArray().put(
                new JSONObject().put("type", "message").put("content", new org.json.JSONArray().put(part)))).toString();
        VoiceDraft result=OpenAiClient.parseResponse(envelope,"original transcript 123");
        assertEquals("نتصل بأحمد",result.title);
        assertEquals("original transcript 123",result.transcript);
    }
    @Test(expected = OpenAiClient.ApiFailure.class) public void blockedOutputIsNotADraft() throws Exception {
        OpenAiClient.parseResponse("{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\"}]}]}","text");
    }
    @Test(expected = OpenAiClient.ApiFailure.class) public void truncatedOutputIsNotADraft() throws Exception {
        OpenAiClient.parseResponse("{\"status\":\"incomplete\",\"output\":[]}","text");
    }
    @Test public void normalizesArabicAndPersianDigits() throws Exception {
        assertEquals("0123456789 0123456789",Digits.latin("٠١٢٣٤٥٦٧٨٩ ۰۱۲۳۴۵۶۷۸۹"));
        VoiceDraft d=VoiceDraft.parse(draft("٢٠٢٦-٠٩-٢٤","۱۷:۳۰","+٢١٨٩١٢٣٤٥٦٧٨"));
        assertEquals("2026-09-24",d.date);assertEquals("17:30",d.time);
        assertEquals("+218912345678",d.phone);
    }
    @Test public void fallbackKeepsTranscriptAndRequiresDateReview() throws Exception {
        VoiceDraft d=OpenAiClient.fallback("غدوة عندي اجتماع");
        assertEquals("غدوة عندي اجتماع",d.transcript);assertTrue(d.needsReview);
        assertEquals(-1,d.futureTime(ZoneId.of("Africa/Tripoli"),0));
    }
    @Test public void requestDoesNotStoreOutputAndUsesStrictSchema() throws Exception {
        JSONObject body=OpenAiClient.requestBody("tomorrow 5pm call Ahmed",java.time.ZonedDateTime.parse("2026-09-24T10:00:00+02:00[Africa/Tripoli]"));
        assertFalse(body.getBoolean("store"));
        JSONObject format=body.getJSONObject("text").getJSONObject("format");
        assertTrue(format.getBoolean("strict"));assertEquals("json_schema",format.getString("type"));
        assertFalse(format.getJSONObject("schema").getBoolean("additionalProperties"));
        assertEquals(6,format.getJSONObject("schema").getJSONArray("required").length());
    }
}
