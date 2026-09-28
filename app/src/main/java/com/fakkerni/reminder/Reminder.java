package com.fakkerni.reminder;

final class Reminder {
    final long id;
    final String title;
    final long eventTimeMillis;
    final String phoneNumber;
    final String contactName;

    Reminder(long id, String title, long eventTimeMillis, String phoneNumber) {
        this(id, title, eventTimeMillis, phoneNumber, "");
    }

    Reminder(long id, String title, long eventTimeMillis, String phoneNumber, String contactName) {
        this.id = id;
        this.title = Digits.latin(title);
        this.eventTimeMillis = eventTimeMillis;
        this.phoneNumber = Digits.latin(phoneNumber);
        this.contactName = Digits.latin(contactName);
    }
}
