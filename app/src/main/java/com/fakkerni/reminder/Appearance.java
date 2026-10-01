package com.fakkerni.reminder;

import android.content.Context;

/** Small, local-only preferences for the assistant's identity and appearance. */
final class Appearance {
    private static final String PREFS="fakkerni_settings";
    private static final String MODE="appearance_mode";
    private static final String NAME="assistant_name";
    private static final String READY="assistant_intro_done";
    private Appearance(){}
    static boolean light(Context c){return c.getSharedPreferences(PREFS,0).getString(MODE,"dark").equals("light");}
    static String mode(Context c){return light(c)?"light":"dark";}
    static void mode(Context c,String value){c.getSharedPreferences(PREFS,0).edit().putString(MODE,value).apply();}
    static String name(Context c){return c.getSharedPreferences(PREFS,0).getString(NAME,Ui.t(c,"فكّرني","Fakkerni"));}
    static void name(Context c,String value){c.getSharedPreferences(PREFS,0).edit().putString(NAME,value.trim()).apply();}
    static boolean ready(Context c){return c.getSharedPreferences(PREFS,0).getBoolean(READY,false);}
    static void completeIntro(Context c){c.getSharedPreferences(PREFS,0).edit().putBoolean(READY,true).apply();}
}
