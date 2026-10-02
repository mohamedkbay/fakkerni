package com.fakkerni.reminder;

import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/** Checks a key without sending voice, contacts, or reminders. */
final class AiConnectionVerifier {
    private AiConnectionVerifier() {}
    static int verify(AiProvider provider, String key) throws Exception {
        if (!provider.accepts(key)) return 401;
        HttpsURLConnection connection=(HttpsURLConnection)new URL(provider.baseUrl+"/models").openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            if(provider==AiProvider.GEMINI)connection.setRequestProperty("x-goog-api-key",key);
            else connection.setRequestProperty("Authorization","Bearer "+key);
            return connection.getResponseCode();
        } finally { connection.disconnect(); }
    }
}
