package com.fakkerni.reminder;

import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.Arrays;

/** Optional developer smoke test: console-only key input, never saved or printed. */
public final class LiveAiCheck {
    public static void main(String[] args) throws Exception {
        if(System.console()==null)throw new IllegalStateException("Run in an interactive terminal");
        AiProvider provider=args.length>1&&args[1].equals("groq")?AiProvider.GROQ:AiProvider.OPENAI;
        char[] secret=System.console().readPassword(provider.label+" key (hidden, not saved): ");
        try {
            OpenAiClient client=new OpenAiClient(provider);
            ZonedDateTime now=ZonedDateTime.now(java.time.ZoneId.of("Africa/Tripoli"));
            VoiceDraft draft=args[0].equals("--text")?client.extract("ذكرني غدا الساعة الخامسة مساء أن أتصل بأحمد.",new String(secret),now)
                    :client.transcribe(Files.readAllBytes(Path.of(args[0])),new String(secret),now);
            System.out.println(draft.date.isEmpty()?"Transcript available; extraction needs review":"Draft extraction succeeded");
            System.out.println("Transcript: "+draft.transcript);
            System.out.println("Draft: "+draft.title+" | "+draft.date+" | "+draft.time+" | contact="+draft.contactName+" | review="+draft.needsReview);
        } catch(OpenAiClient.ApiFailure e) {
            System.out.println(provider.label+" HTTP status: "+e.status);System.exit(1);
        } finally { Arrays.fill(secret,'\0'); }
    }
}
