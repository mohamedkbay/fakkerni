package com.fakkerni.reminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

final class LocaleHelper {
    private static final String PREFS = "fakkerni_settings";
    private static final String LANGUAGE = "language";

    private LocaleHelper() {}

    static Context wrap(Context context) {
        String code = language(context);
        Locale locale = Locale.forLanguageTag(code + "-u-nu-latn");
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    static void toggle(Context context) {
        String next = language(context).equals("ar") ? "en" : "ar";
        preferences(context).edit().putString(LANGUAGE, next).apply();
    }

    private static String language(Context context) {
        return preferences(context).getString(LANGUAGE, "ar");
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
