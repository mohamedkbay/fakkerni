import XCTest
@testable import FakkerniCore

final class CoreTests: XCTestCase {
    let epoch = Date(timeIntervalSince1970: 0)
    let tripoli = TimeZone(identifier: "Africa/Tripoli")!
    func draft(_ changes: [String: Any] = [:]) throws -> VoiceDraft {
        var object: [String: Any] = ["transcript": "غدوة نتصل بأحمد", "title": "اتصال بأحمد", "date": "2026-10-14", "time": "17:00", "phone": "", "contact_name": "أحمد", "needs_review": false]
        object.merge(changes) { _, new in new }
        return try VoiceDraft.parse(JSONSerialization.data(withJSONObject: object))
    }
    func testArabicAndPersianDigitsBecomeWestern() { XCTAssertEqual(Digits.latin("١٢٣ ۴۵۶ 09"), "123 456 09") }
    func testPhoneCannotInjectURLParameters() { XCTAssertEqual(PhoneNumber.clean("+218 (91) 123-4567"), "+218911234567"); XCTAssertEqual(PhoneNumber.clean("123;ext=456"), ""); XCTAssertEqual(PhoneNumber.clean("*123#"), "") }
    func testLibyanDateUsesLocalTime() throws { XCTAssertEqual(try draft().eventDate(timeZone: tripoli, now: epoch)?.timeIntervalSince1970, 1791990000) }
    func testPastDateIsNotScheduled() throws { XCTAssertNil(try draft().eventDate(timeZone: tripoli, now: Date(timeIntervalSince1970: 2_000_000_000))) }
    func testImpossibleDateIsRejected() throws { XCTAssertNil(try draft(["date": "2026-02-30"]).eventDate(timeZone: tripoli, now: epoch)) }
    func testInvalidTimeRequiresReview() throws { let value = try draft(["time": "24:99"]); XCTAssertTrue(value.needsReview); XCTAssertNil(value.eventDate(timeZone: tripoli, now: epoch)) }
    func testMissingDateDoesNotGuessTomorrow() throws { let value = try draft(["date": ""]); XCTAssertTrue(value.needsReview); XCTAssertNil(value.eventDate(timeZone: tripoli, now: epoch)) }
    func testAmbiguousDaylightTimeRequiresSelection() throws { XCTAssertNil(try draft(["date": "2026-11-01", "time": "01:30"]).eventDate(timeZone: TimeZone(identifier: "America/New_York")!, now: epoch)) }
    func testNonexistentDaylightTimeRequiresSelection() throws { XCTAssertNil(try draft(["date": "2026-03-08", "time": "02:30"]).eventDate(timeZone: TimeZone(identifier: "America/New_York")!, now: epoch)) }
    func testOversizedModelFieldsAreRejected() { XCTAssertThrowsError(try draft(["title": String(repeating: "ا", count: 251)])) }
    func testFallbackPreservesArabicAndLeavesTimeEmpty() { let value = VoiceDraft.fallback("اتصل بأحمد الساعة ٥"); XCTAssertEqual(value.transcript, "اتصل بأحمد الساعة 5"); XCTAssertTrue(value.extractionFailed); XCTAssertTrue(value.date.isEmpty); XCTAssertTrue(value.time.isEmpty) }
    func testArabicDiacriticsAndAlefVariantsMatch() { XCTAssertTrue(ContactMatcher.nameMatches("أحمد", "احْمَد سالم")); XCTAssertFalse(ContactMatcher.nameMatches("علي", "عبدالعليم")) }
    func testEnglishArabicNamesMatchLocally() { XCTAssertTrue(ContactMatcher.nameMatches("محمد", "Mohamed Bay")); XCTAssertTrue(ContactMatcher.nameMatches("Ahmed", "أحمد سالم")); XCTAssertTrue(ContactMatcher.nameMatches("Sara", "سارة")) }
    func testPhoneSearchAcceptsArabicDigits() { XCTAssertTrue(ContactMatcher.phoneMatches("٩١١٢٣", "+218911234567")); XCTAssertFalse(ContactMatcher.phoneMatches("91", "+218911234567")) }
    func testAmbiguousContactsRemainMultipleChoices() { let people = [ContactPerson(id: "a", name: "أحمد سالم", phone: "1234"), ContactPerson(id: "b", name: "Ahmed Ali", phone: "5678")]; XCTAssertEqual(ContactMatcher.search("أحمد", in: people).count, 2) }
    func testAlertIsAnHourBeforeEvent() { let reminder = Reminder(title: "موعد", eventTime: epoch.addingTimeInterval(7200), now: epoch); XCTAssertEqual(reminder.alertTime, epoch.addingTimeInterval(3600)) }
    func testNearEventGetsImmediateAlertWithoutChangingEvent() { let reminder = Reminder(title: "موعد", eventTime: epoch.addingTimeInterval(120), now: epoch); XCTAssertEqual(reminder.alertTime, epoch.addingTimeInterval(3)); XCTAssertEqual(reminder.eventTime, epoch.addingTimeInterval(120)) }
    func testPersistenceRoundTripAndCorruptData() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = ReminderFileStore(url: directory.appendingPathComponent("reminders.json"))
        XCTAssertTrue(try store.load().isEmpty)
        let value = Reminder(title: "تذكير", eventTime: epoch.addingTimeInterval(7200), now: epoch)
        try store.save([value]); XCTAssertEqual(try store.load(), [value])
        try Data("broken".utf8).write(to: store.url); XCTAssertThrowsError(try store.load())
    }
    func testMultipartForcesArabicTranscription() {
        let body = AIRequests.transcription(audio: Data([0, 1]), provider: .groq, boundary: "Test")
        let text = String(decoding: body, as: UTF8.self)
        XCTAssertTrue(text.contains("name=\"language\"\r\n\r\nar\r\n")); XCTAssertTrue(text.contains("whisper-large-v3-turbo")); XCTAssertTrue(text.hasSuffix("--Test--\r\n"))
    }
    func testExtractionUsesSingleProviderAndStrictSchema() {
        let groq = AIRequests.extraction(transcript: "hello", provider: .groq, now: epoch, timeZone: tripoli)
        XCTAssertEqual(groq["model"] as? String, "openai/gpt-oss-20b")
        let format = groq["response_format"] as? [String: Any]
        XCTAssertEqual((format?["json_schema"] as? [String: Any])?["strict"] as? Bool, true)
        let openAI = AIRequests.extraction(transcript: "hello", provider: .openAI, now: epoch, timeZone: tripoli)
        XCTAssertEqual(openAI["store"] as? Bool, false); XCTAssertNil(openAI["messages"])
    }
    func testRefusalAndIncompleteResponseDoNotBecomeDrafts() throws {
        let truncated: [String: Any] = ["choices": [["finish_reason": "length", "message": ["content": "{}"]]]]
        XCTAssertThrowsError(try AIRequests.parseResponse(JSONSerialization.data(withJSONObject: truncated), provider: .groq, transcript: "كلام"))
        XCTAssertThrowsError(try AIRequests.parseResponse(Data("{\"status\":\"incomplete\",\"output\":[]}".utf8), provider: .openAI, transcript: "كلام"))
    }
    func testAuthoritativeTranscriptOverridesModelTranscript() throws {
        let object: [String: Any] = ["choices": [["finish_reason": "stop", "message": ["content": "{\"transcript\":\"wrong\",\"title\":\"اتصال\",\"date\":\"\",\"time\":\"\"}"]]]]
        let value = try AIRequests.parseResponse(JSONSerialization.data(withJSONObject: object), provider: .groq, transcript: "التسجيل الأصلي")
        XCTAssertEqual(value.transcript, "التسجيل الأصلي"); XCTAssertTrue(value.needsReview)
    }
}
