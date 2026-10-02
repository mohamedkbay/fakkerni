package com.fakkerni.reminder;

import org.json.*;
import org.junit.Test;
import java.time.ZonedDateTime;
import static org.junit.Assert.*;

public class GroqClientTest {
    @Test public void audioRequestExplicitlyUsesArabicForBothProviders() throws Exception {
        for(AiProvider provider:new AiProvider[]{AiProvider.GROQ,AiProvider.OPENAI}){
            String body=new String(new OpenAiClient(provider).transcriptionBody(new byte[]{1,2,3},"test-boundary"),java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(body.contains("name=\"language\"\r\n\r\nar\r\n"));
            assertTrue(body.contains("باللهجة الليبية بالحروف العربية"));
            assertFalse(body.contains("name=\"language\"\r\n\r\nen"));
            assertTrue(body.endsWith("--test-boundary--\r\n"));
        }
    }
    @Test public void extractedReminderMustStayArabic() throws Exception {
        JSONObject request=OpenAiClient.groqRequestBody("غدوة خمسة العشية نتصل بأحمد",ZonedDateTime.now());
        String instruction=request.getJSONArray("messages").getJSONObject(0).getString("content");
        assertTrue(instruction.contains("Always write the reminder title in Arabic"));
        assertTrue(instruction.contains("do not translate or romanize"));
        assertTrue(instruction.contains("Use ASCII digits"));
    }
    private String envelope(String reason,Object refusal) throws Exception {
        JSONObject draft=new JSONObject().put("title","اتصال بأحمد").put("date","2026-09-25")
                .put("time","17:00").put("phone","").put("needs_review",false);
        return new JSONObject().put("choices",new JSONArray().put(new JSONObject().put("finish_reason",reason)
                .put("message",new JSONObject().put("content",draft.toString()).put("refusal",refusal)))).toString();
    }
    @Test public void groqUsesStrictChatSchemaAndSeparateInput() throws Exception {
        String text="غدوة الساعة خمسة العشية اتصل بأحمد";
        JSONObject b=OpenAiClient.groqRequestBody(text,ZonedDateTime.parse("2026-09-24T10:00:00+02:00[Africa/Tripoli]"));
        assertEquals("openai/gpt-oss-20b",b.getString("model"));
        assertEquals(text,b.getJSONArray("messages").getJSONObject(1).getString("content"));
        assertTrue(b.getJSONArray("messages").getJSONObject(0).getString("content").contains("Africa/Tripoli"));
        JSONObject schema=b.getJSONObject("response_format").getJSONObject("json_schema");
        assertTrue(schema.getBoolean("strict"));
        assertFalse(schema.getJSONObject("schema").getBoolean("additionalProperties"));
        assertEquals(6,schema.getJSONObject("schema").getJSONArray("required").length());
        assertFalse(b.has("input"));
    }
    @Test public void completedGroqDraftPreservesOriginalTranscript() throws Exception {
        VoiceDraft d=OpenAiClient.parseGroqResponse(envelope("stop",JSONObject.NULL),"غدوة اتصل بأحمد");
        assertEquals("اتصال بأحمد",d.title);assertEquals("2026-09-25",d.date);
        assertEquals("غدوة اتصل بأحمد",d.transcript);assertFalse(d.needsReview);
    }
    @Test(expected=OpenAiClient.ApiFailure.class) public void truncatedGroqOutputRejected() throws Exception {
        OpenAiClient.parseGroqResponse(envelope("length",JSONObject.NULL),"recording");
    }
    @Test(expected=OpenAiClient.ApiFailure.class) public void groqRefusalRejected() throws Exception {
        OpenAiClient.parseGroqResponse(envelope("stop","refused"),"recording");
    }
    @Test(expected=JSONException.class) public void malformedGroqOutputRejected() throws Exception {
        OpenAiClient.parseGroqResponse("{\"choices\":[]}","recording");
    }
    @Test public void providerKeysCannotBeMixed() {
        String groq="gsk_test_not_a_real_key_123456789";
        String openai="sk-test-not-a-real-key-123456789";
        assertTrue(AiProvider.GROQ.accepts(groq));assertFalse(AiProvider.OPENAI.accepts(groq));
        assertTrue(AiProvider.OPENAI.accepts(openai));assertFalse(AiProvider.GROQ.accepts(openai));
        assertFalse(AiProvider.GROQ.accepts(null));assertFalse(AiProvider.GROQ.accepts(groq+"\n"));
        assertEquals("https://api.groq.com/openai/v1",AiProvider.GROQ.baseUrl);
        assertTrue(AiProvider.GEMINI.accepts("AQ.Ab123456789012345678901234567890"));
        assertTrue(AiProvider.GEMINI.accepts("AIza123456789012345678901234567890"));
    }
    @Test public void geminiSendsInlineAudioAndRequiresArabicTranscript() throws Exception {
        JSONObject body=OpenAiClient.geminiRequestBody(new byte[]{1,2,3},
                ZonedDateTime.parse("2026-10-02T10:00:00+02:00[Africa/Tripoli]"));
        JSONArray parts=body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts");
        assertTrue(parts.getJSONObject(0).getString("text").contains("Arabic script"));
        assertEquals("audio/mp4",parts.getJSONObject(1).getJSONObject("inline_data").getString("mime_type"));
        assertEquals("AQID",parts.getJSONObject(1).getJSONObject("inline_data").getString("data"));
        JSONObject config=body.getJSONObject("generationConfig");
        assertEquals("application/json",config.getString("responseMimeType"));
        assertEquals(7,config.getJSONObject("responseSchema").getJSONArray("required").length());
    }
    @Test public void geminiDraftUsesItsArabicTranscript() throws Exception {
        JSONObject draft=new JSONObject().put("transcript","غدوة اتصل بأحمد").put("title","اتصال بأحمد")
                .put("date","2026-10-03").put("time","17:00").put("phone","")
                .put("contact_name","أحمد").put("needs_review",false);
        String envelope=new JSONObject().put("candidates",new JSONArray().put(new JSONObject()
                .put("finishReason","STOP").put("content",new JSONObject().put("parts",new JSONArray()
                        .put(new JSONObject().put("text",draft.toString())))))).toString();
        VoiceDraft parsed=OpenAiClient.parseGeminiResponse(envelope);
        assertEquals("غدوة اتصل بأحمد",parsed.transcript);assertEquals("أحمد",parsed.contactName);
    }
    @Test(expected=OpenAiClient.ApiFailure.class) public void wrongProviderKeyRejectedBeforeUpload() throws Exception {
        new OpenAiClient(AiProvider.GROQ).transcribe(new byte[]{1},"sk-test-not-a-real-key-123456789",ZonedDateTime.now());
    }
}
