package com.fakkerni.reminder;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlertToneTest {
    @Test public void exactlyNineBundledSoundsPlusPhonePicker() {
        assertEquals(10,AlertTone.values().length);
        assertEquals(AlertTone.BELL,AlertTone.fromAssetId("bell"));
        assertEquals(AlertTone.BRIGHT,AlertTone.fromAssetId("bright"));
        assertNull(AlertTone.fromAssetId("phone"));
        assertNull(AlertTone.fromAssetId("../private"));
    }
    @Test public void phoneSoundGetsStableDistinctChannel() {
        String a=AlertTone.phoneChannelIdFor("content://media/internal/audio/media/1");
        assertEquals(a,AlertTone.phoneChannelIdFor("content://media/internal/audio/media/1"));
        assertNotEquals(a,AlertTone.phoneChannelIdFor("content://media/internal/audio/media/2"));
        assertTrue(a.startsWith("loud_reminders_phone_"));
    }
}
