import Foundation

// A small no-Xcode check; the full XCTest suite runs on the macOS CI runner.
@main enum CoreCheck {
    static func main() throws {
        func check(_ value: @autoclosure () -> Bool, _ message: String) { precondition(value(), message) }
        check(Digits.latin("١٢٣ ۴۵۶") == "123 456", "Western digits")
        check(ContactMatcher.nameMatches("أحمد", "Ahmed Ali"), "Cross-script names")
        check(!ContactMatcher.nameMatches("علي", "عبدالعليم"), "No substring AI match")
        check(PhoneNumber.clean("123;ext=4").isEmpty, "Phone URL validation")
        let json = Data("{\"transcript\":\"اتصل بأحمد\",\"title\":\"اتصال\",\"date\":\"2026-10-14\",\"time\":\"17:00\",\"needs_review\":false}".utf8)
        let draft = try VoiceDraft.parse(json)
        let expected = ISO8601DateFormatter().date(from: "2026-10-14T15:00:00Z")!
        check(draft.eventDate(timeZone: TimeZone(identifier: "Africa/Tripoli")!, now: .distantPast) == expected, "Libyan event time")
        let fallback = VoiceDraft.fallback("موعد الساعة ٥")
        check(fallback.date.isEmpty && fallback.time.isEmpty && fallback.transcript == "موعد الساعة 5", "No invented date")
        let reminder = Reminder(title: "موعد", eventTime: expected, now: .distantPast)
        check(reminder.alertTime == expected.addingTimeInterval(-3600), "One-hour offset")
        print("7 core checks passed")
    }
}
