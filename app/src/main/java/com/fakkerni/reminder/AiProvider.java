package com.fakkerni.reminder;

import android.content.Context;

enum AiProvider {
    OPENAI("OpenAI","https://api.openai.com/v1","sk-","https://platform.openai.com/api-keys","openai-key.enc"),
    GROQ("Groq","https://api.groq.com/openai/v1","gsk_","https://console.groq.com/keys","groq-key.enc"),
    GEMINI("Gemini","https://generativelanguage.googleapis.com/v1beta","","https://aistudio.google.com/app/apikey","gemini-key.enc");
    final String label,baseUrl,keyPrefix,keysUrl,keyFile;
    AiProvider(String label,String baseUrl,String keyPrefix,String keysUrl,String keyFile){this.label=label;this.baseUrl=baseUrl;this.keyPrefix=keyPrefix;this.keysUrl=keysUrl;this.keyFile=keyFile;}
    boolean accepts(String key){return key!=null&&key.startsWith(keyPrefix)&&key.length()>=20&&key.length()<=512&&key.matches("[A-Za-z0-9_.\\-]+");}
    static AiProvider selected(Context c){
        String fallback=AiKeyStore.hasKey(c,OPENAI)?"OPENAI":AiKeyStore.hasKey(c,GROQ)?"GROQ":AiKeyStore.hasKey(c,GEMINI)?"GEMINI":"GROQ";
        try{return valueOf(c.getSharedPreferences("ai_provider",Context.MODE_PRIVATE).getString("provider",fallback));}
        catch(IllegalArgumentException e){return GROQ;}
    }
    static AiProvider active(Context c){AiProvider p=selected(c);return AiKeyStore.hasKey(c,p)?p:null;}
    static void select(Context c,AiProvider p){c.getSharedPreferences("ai_provider",Context.MODE_PRIVATE).edit().putString("provider",p.name()).apply();}
}
