package com.fakkerni.reminder;

import android.content.Context;

enum AiProvider {
    OPENAI("OpenAI","https://api.openai.com/v1","sk-","https://platform.openai.com/api-keys"),
    GROQ("Groq","https://api.groq.com/openai/v1","gsk_","https://console.groq.com/keys");
    final String label,baseUrl,keyPrefix,keysUrl;
    AiProvider(String label,String baseUrl,String keyPrefix,String keysUrl){this.label=label;this.baseUrl=baseUrl;this.keyPrefix=keyPrefix;this.keysUrl=keysUrl;}
    boolean accepts(String key){return key!=null&&key.startsWith(keyPrefix)&&key.length()>=25&&key.matches("[A-Za-z0-9_\\-]+");}
    static AiProvider selected(Context c){
        String fallback=AiKeyStore.hasKey(c,OPENAI)?"OPENAI":"GROQ";
        try{return valueOf(c.getSharedPreferences("ai_provider",Context.MODE_PRIVATE).getString("provider",fallback));}
        catch(IllegalArgumentException e){return GROQ;}
    }
    static void select(Context c,AiProvider p){c.getSharedPreferences("ai_provider",Context.MODE_PRIVATE).edit().putString("provider",p.name()).apply();}
}
