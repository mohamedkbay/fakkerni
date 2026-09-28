package com.fakkerni.reminder;

import java.text.Normalizer;
import java.util.Locale;

/** Conservative local matching: whole names/tokens, never fuzzy guesses about phone numbers. */
final class ContactNames {
    static String normalize(String value){
        if(value==null)return "";
        return Normalizer.normalize(value,Normalizer.Form.NFKD).replaceAll("\\p{M}","")
                .replace('ى','ي').replace("ـ","").toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+"," ").trim().replaceAll("\\s+"," ");
    }
    static boolean matches(String spoken,String stored){
        String needle=normalize(spoken),name=normalize(stored);
        return needle.length()>=2&&(" "+name+" ").contains(" "+needle+" ");
    }
}
