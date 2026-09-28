package com.fakkerni.reminder;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import java.util.*;

final class LocalContacts {
    static final class Match {
        final String name,phone;
        Match(String name,String phone){this.name=name;this.phone=phone;}
    }
    static List<Match> find(Context context,String spoken){
        List<Match> matches=new ArrayList<>();Set<String> seen=new HashSet<>();
        try(Cursor rows=context.getContentResolver().query(Phone.CONTENT_URI,
                new String[]{Phone.DISPLAY_NAME,Phone.NUMBER},null,null,Phone.DISPLAY_NAME+" ASC")){
            if(rows!=null)while(rows.moveToNext()){
                if(Thread.currentThread().isInterrupted())break;
                String name=rows.getString(0),phone=Digits.latin(rows.getString(1));
                if(!phone.matches("\\+?[0-9 ()-]{3,40}")||!ContactNames.matches(spoken,name))continue;
                String identity=ContactNames.normalize(name)+"|"+phone.replaceAll("[ ()-]","");
                if(seen.add(identity))matches.add(new Match(name,phone));
            }
        }
        return matches;
    }
}
