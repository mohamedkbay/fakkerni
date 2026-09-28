package com.fakkerni.reminder;

/** Normalize Arabic-Indic and Persian numerals in all user-visible data. */
final class Digits {
    static String latin(String text) {
        if(text==null) return "";
        StringBuilder out=new StringBuilder(text.length());
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c>='٠'&&c<='٩') c=(char)('0'+c-'٠');
            else if(c>='۰'&&c<='۹') c=(char)('0'+c-'۰');
            out.append(c);
        }
        return out.toString();
    }
}
