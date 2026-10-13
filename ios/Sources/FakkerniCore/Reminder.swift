import Foundation

public enum Digits {
    public static func latin(_ text: String) -> String {
        String(text.map { character in
            guard let scalar = character.unicodeScalars.first, character.unicodeScalars.count == 1 else { return character }
            for base: UInt32 in [0x0660, 0x06F0] where scalar.value >= base && scalar.value <= base + 9 {
                return Character(String(scalar.value - base))
            }
            return character
        })
    }
}

public enum PhoneNumber {
    public static func clean(_ input: String) -> String {
        let text = Digits.latin(input).trimmingCharacters(in: .whitespacesAndNewlines)
        guard text.count <= 40, text.range(of: #"^\+?[0-9 ()-]{3,40}$"#, options: .regularExpression) != nil else { return "" }
        let digits = text.filter { $0 >= "0" && $0 <= "9" }
        guard digits.count >= 3 else { return "" }
        return (text.hasPrefix("+") ? "+" : "") + digits
    }
}

public struct Reminder: Codable, Identifiable, Equatable, Sendable {
    public var id: UUID
    public var title: String
    public var eventTime: Date
    public var alertTime: Date
    public var phone: String
    public var contactName: String
    public var completed: Bool

    public init(id: UUID = UUID(), title: String, eventTime: Date, phone: String = "", contactName: String = "", completed: Bool = false, now: Date = Date()) {
        self.id = id
        self.title = Digits.latin(title.trimmingCharacters(in: .whitespacesAndNewlines))
        self.eventTime = eventTime
        self.alertTime = max(eventTime.addingTimeInterval(-3600), now.addingTimeInterval(3))
        self.phone = PhoneNumber.clean(phone)
        self.contactName = Digits.latin(contactName)
        self.completed = completed
    }
}

public enum DraftError: Error, Equatable { case invalid, oversized, noSpeech }

public struct VoiceDraft: Identifiable, Equatable, Sendable {
    public let id = UUID()
    public var transcript: String
    public var title: String
    public var date: String
    public var time: String
    public var phone: String
    public var contactName: String
    public var needsReview: Bool
    public var extractionFailed: Bool

    public init(transcript: String = "", title: String = "", date: String = "", time: String = "", phone: String = "", contactName: String = "", needsReview: Bool = true, extractionFailed: Bool = false) {
        self.transcript = transcript; self.title = title; self.date = date; self.time = time
        self.phone = phone; self.contactName = contactName; self.needsReview = needsReview; self.extractionFailed = extractionFailed
    }

    public static func parse(_ data: Data, transcript override: String? = nil) throws -> VoiceDraft {
        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { throw DraftError.invalid }
        func field(_ key: String, max: Int) throws -> String {
            let value = Digits.latin((object[key] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines))
            guard value.count <= max else { throw DraftError.oversized }
            return value
        }
        let transcript = try override.map { Digits.latin($0.trimmingCharacters(in: .whitespacesAndNewlines)) } ?? field("transcript", max: 6000)
        guard !transcript.isEmpty else { throw DraftError.noSpeech }
        guard transcript.count <= 6000 else { throw DraftError.oversized }
        let title = try field("title", max: 250)
        var date = try field("date", max: 10)
        var time = try field("time", max: 5)
        if date.range(of: #"^[0-9]{4}-[0-9]{2}-[0-9]{2}$"#, options: .regularExpression) == nil { date = "" }
        if time.range(of: #"^([01][0-9]|2[0-3]):[0-5][0-9]$"#, options: .regularExpression) == nil { time = "" }
        return VoiceDraft(transcript: transcript, title: title, date: date, time: time,
                          phone: PhoneNumber.clean(try field("phone", max: 40)), contactName: try field("contact_name", max: 120),
                          needsReview: (object["needs_review"] as? Bool ?? true) || date.isEmpty || time.isEmpty || title.isEmpty)
    }

    /// Missing, invalid, past, nonexistent, and ambiguous wall-clock times require a human choice.
    public func eventDate(timeZone: TimeZone = .current, now: Date = Date()) -> Date? {
        let d = date.split(separator: "-").compactMap { Int($0) }
        let t = time.split(separator: ":").compactMap { Int($0) }
        guard d.count == 3, t.count == 2, (1...12).contains(d[1]), (1...31).contains(d[2]), (0...23).contains(t[0]), (0...59).contains(t[1]) else { return nil }
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = timeZone
        var parts = DateComponents(); parts.year = d[0]; parts.month = d[1]; parts.day = d[2]; parts.hour = t[0]; parts.minute = t[1]; parts.second = 0
        guard let candidate = calendar.date(from: parts), candidate > now else { return nil }
        let read = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: candidate)
        guard read.year == d[0], read.month == d[1], read.day == d[2], read.hour == t[0], read.minute == t[1] else { return nil }
        let start = calendar.startOfDay(for: candidate).addingTimeInterval(-1)
        let first = calendar.nextDate(after: start, matching: parts, matchingPolicy: .strict, repeatedTimePolicy: .first)
        let last = calendar.nextDate(after: start, matching: parts, matchingPolicy: .strict, repeatedTimePolicy: .last)
        guard first == last else { return nil }
        return candidate
    }

    public static func fallback(_ transcript: String) -> VoiceDraft {
        VoiceDraft(transcript: Digits.latin(transcript), title: String(Digits.latin(transcript).prefix(250)), extractionFailed: true)
    }
}

public struct ReminderFileStore {
    public let url: URL
    public init(url: URL) { self.url = url }
    public func load() throws -> [Reminder] {
        guard FileManager.default.fileExists(atPath: url.path) else { return [] }
        return try JSONDecoder().decode([Reminder].self, from: Data(contentsOf: url))
    }
    public func save(_ reminders: [Reminder]) throws {
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try JSONEncoder().encode(reminders).write(to: url, options: .atomic)
    }
}
