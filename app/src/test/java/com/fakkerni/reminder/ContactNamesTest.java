package com.fakkerni.reminder;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class ContactNamesTest {
    @Test public void arabicDiacriticsAndAlefNormalize(){assertTrue(ContactNames.matches("احمد","أَحْمَد علي"));}
    @Test public void matchesWholeNameTokens(){assertTrue(ContactNames.matches("Ali","Ahmed Ali"));assertFalse(ContactNames.matches("Ali","Khalid"));}
    @Test public void keepsDifferentNamesSeparate(){assertFalse(ContactNames.matches("حسن","حسين"));}
    @Test public void rejectsEmptyOrVeryShortName(){assertFalse(ContactNames.matches("", "أحمد"));assertFalse(ContactNames.matches("a","Adam"));}
    @Test public void accentsAndCaseNormalize(){assertTrue(ContactNames.matches("JOSE", "José García"));}
    @Test public void matchesArabicAndEnglishNamesBothWays(){
        assertTrue(ContactNames.matches("أحمد","Ahmed Ali"));
        assertTrue(ContactNames.matches("Mohamed","محمد علي"));
        assertTrue(ContactNames.matches("Ali","علي أحمد"));
        assertTrue(ContactNames.matches("Sara","سارة"));
        assertFalse(ContactNames.matches("حسن","Hussein"));
    }
    @Test public void phoneSearchRequiresFourDigits(){
        assertTrue(ContactNames.phoneMatches("9123","+218 91 234 5678"));
        assertTrue(ContactNames.phoneMatches("٠٩١٢","0912345678"));
        assertFalse(ContactNames.phoneMatches("091","0912345678"));
    }
    @Test public void exactPhraseNotLooseTokens(){assertTrue(ContactNames.matches("احمد علي","د. أحمد علي العمل"));assertFalse(ContactNames.matches("احمد علي","علي احمد"));}
    @Test public void parsesSpokenNameWithoutInventingPhone()throws Exception{
        VoiceDraft d=VoiceDraft.parse(new JSONObject().put("transcript","اتصل بأحمد غدوة").put("contact_name","أحمد").toString());
        assertEquals("أحمد",d.contactName);assertEquals("",d.phone);assertTrue(d.needsReview);
    }
    @Test public void oldDraftsRemainCompatible()throws Exception{assertEquals("",VoiceDraft.parse("{\"transcript\":\"meeting\"}").contactName);}
}
