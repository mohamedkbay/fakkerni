package com.fakkerni.reminder;

import java.text.Normalizer;
import java.util.Locale;

/** Local, token-level Arabic/Latin name matching and explicit phone-number lookup. */
final class ContactNames {
    static String normalize(String value){
        if(value==null)return "";
        return Normalizer.normalize(Digits.latin(value),Normalizer.Form.NFKD).replaceAll("\\p{M}","")
                .replace('ى','ي').replace("ـ","").toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+"," ").trim().replaceAll("\\s+"," ");
    }
    static boolean matches(String spoken,String stored){
        String needle=normalize(spoken),name=normalize(stored);
        if(needle.length()<2)return false;
        if((" "+name+" ").contains(" "+needle+" "))return true;
        String[] search=needle.split(" "),parts=name.split(" ");
        for(int start=0;start+search.length<=parts.length;start++){
            boolean all=true;
            for(int i=0;i<search.length;i++)if(!equivalent(search[i],parts[start+i])){all=false;break;}
            if(all)return true;
        }
        return false;
    }
    static boolean phoneMatches(String spoken,String number){
        String query=Digits.latin(spoken==null?"":spoken).replaceAll("[^0-9]","");
        String digits=Digits.latin(number==null?"":number).replaceAll("[^0-9]","");
        return query.length()>=4&&digits.contains(query);
    }
    private static boolean equivalent(String left,String right){
        if(left.equals(right))return true;
        boolean la=left.matches(".*[\\u0600-\\u06ff].*"),ra=right.matches(".*[\\u0600-\\u06ff].*");
        if(la==ra)return false;
        String a=la?left:right,b=la?right:left;
        if(a.equals("ساره")||a.equals("سارة"))return b.equals("sara")||b.equals("sarah");
        String known=alias(a);
        if(!known.isEmpty())return known.equals(b);
        String arabic=skeleton(a),latin=skeleton(b);
        return arabic.length()>=3&&arabic.equals(latin);
    }
    private static String alias(String ar){
        switch(ar){
            case "علي":return "ali";
            case "عمر":return "omar";
            case "يوسف":return "yousef";
            case "ابراهيم":return "ibrahim";
            case "فاطمه":case "فاطمة":return "fatima";
            case "حسين":return "hussein";
            case "حسن":return "hassan";
            default:return "";
        }
    }
    private static String skeleton(String value){
        StringBuilder out=new StringBuilder();
        for(int i=0;i<value.length();i++){
            char ch=value.charAt(i);String piece;
            switch(ch){
                case 'ا':case 'أ':case 'إ':case 'آ':case 'ع':case 'ء':case 'ة':piece="";break;
                case 'ب':piece="b";break;case 'ت':case 'ط':piece="t";break;
                case 'ث':piece="th";break;case 'ج':piece="j";break;
                case 'ح':case 'ه':piece="h";break;case 'خ':piece="kh";break;
                case 'د':case 'ض':piece="d";break;case 'ذ':piece="dh";break;
                case 'ر':piece="r";break;case 'ز':case 'ظ':piece="z";break;
                case 'س':case 'ص':piece="s";break;case 'ش':piece="sh";break;
                case 'غ':piece="gh";break;case 'ف':piece="f";break;
                case 'ق':piece="q";break;case 'ك':piece="k";break;
                case 'ل':piece="l";break;case 'م':piece="m";break;
                case 'ن':piece="n";break;case 'و':piece="w";break;
                case 'ي':case 'ى':piece="y";break;
                default:piece="aeiou".indexOf(ch)>=0?"":String.valueOf(ch);
            }
            if(!piece.isEmpty()&&!out.toString().endsWith(piece))out.append(piece);
        }
        return out.toString();
    }
}
