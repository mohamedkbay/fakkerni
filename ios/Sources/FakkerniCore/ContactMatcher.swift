import Foundation

public struct ContactPerson: Identifiable, Equatable, Sendable {
    public let id: String
    public let name: String
    public let phone: String
    public init(id: String, name: String, phone: String) { self.id = id; self.name = name; self.phone = PhoneNumber.clean(phone) }
}

public enum ContactMatcher {
    public static func normalize(_ text: String) -> String {
        let value = Digits.latin(text).decomposedStringWithCompatibilityMapping
            .unicodeScalars.filter { !CharacterSet.nonBaseCharacters.contains($0) }
        return String(String.UnicodeScalarView(value)).lowercased()
            .replacingOccurrences(of: "ى", with: "ي").replacingOccurrences(of: "ـ", with: "")
            .components(separatedBy: CharacterSet.alphanumerics.inverted).filter { !$0.isEmpty }.joined(separator: " ")
    }
    public static func phoneMatches(_ query: String, _ phone: String) -> Bool {
        let q = Digits.latin(query).filter { $0 >= "0" && $0 <= "9" }
        let p = Digits.latin(phone).filter { $0 >= "0" && $0 <= "9" }
        return q.count >= 4 && p.contains(q)
    }
    public static func nameMatches(_ query: String, _ name: String, partial: Bool = false) -> Bool {
        let q = normalize(query), n = normalize(name)
        guard q.count >= 2 else { return false }
        if partial && n.contains(q) { return true }
        if (" " + n + " ").contains(" " + q + " ") { return true }
        let a = q.split(separator: " ").map(String.init), b = n.split(separator: " ").map(String.init)
        guard a.count <= b.count else { return false }
        return (0...(b.count-a.count)).contains { start in zip(a, b[start...]).allSatisfy { equivalent($0, $1) } }
    }
    public static func search(_ query: String, in contacts: [ContactPerson]) -> [ContactPerson] {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return contacts }
        return contacts.filter { nameMatches(query, $0.name, partial: true) || phoneMatches(query, $0.phone) }
    }
    private static func equivalent(_ left: String, _ right: String) -> Bool {
        if left == right { return true }
        func arabic(_ value: String) -> Bool { value.unicodeScalars.contains { (0x0600...0x06FF).contains($0.value) } }
        guard arabic(left) != arabic(right) else { return false }
        let ar = arabic(left) ? left : right, en = arabic(left) ? right : left
        let aliases: [String: [String]] = ["محمد": ["mohamed", "mohammed", "muhammad", "mohammad"], "احمد": ["ahmed", "ahmad"], "علي": ["ali"], "عمر": ["omar", "umar"], "يوسف": ["yousef", "yusuf", "youssef"], "ابراهيم": ["ibrahim"], "سارة": ["sara", "sarah"], "ساره": ["sara", "sarah"], "فاطمة": ["fatima", "fatimah"], "فاطمه": ["fatima", "fatimah"], "حسن": ["hassan", "hasan"], "حسين": ["hussein", "hussain"]]
        if let names = aliases[ar] { return names.contains(en) }
        let map: [Character: String] = ["ا":"", "ع":"", "ء":"", "ة":"", "ب":"b", "ت":"t", "ط":"t", "ث":"th", "ج":"j", "ح":"h", "ه":"h", "خ":"kh", "د":"d", "ض":"d", "ذ":"dh", "ر":"r", "ز":"z", "ظ":"z", "س":"s", "ص":"s", "ش":"sh", "غ":"gh", "ف":"f", "ق":"q", "ك":"k", "ل":"l", "م":"m", "ن":"n", "و":"w", "ي":"y"]
        func skeleton(_ value: String) -> String {
            var result = ""
            for character in value {
                let piece = map[character] ?? ("aeiou".contains(character) ? "" : String(character))
                if !piece.isEmpty && !result.hasSuffix(piece) { result += piece }
            }
            return result
        }
        let a = skeleton(ar), b = skeleton(en)
        return a.count >= 3 && a == b
    }
}
