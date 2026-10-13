import SwiftUI
import Security
import FakkerniCore

enum Tone: String, CaseIterable, Identifiable {
    case bell, flow, received, message, spark, softMessage = "soft_message", modern, quick, bright, system
    var id: String { rawValue }
    var filename: String? { self == .system ? nil : "tone_\(rawValue).caf" }
    func label(_ ar: Bool) -> String {
        let arabic = ["جرس واضح", "نغمة ممتدة", "استلام", "رسالة", "ومضة", "رسالة هادئة", "حديثة", "سريعة", "مشرقة", "نغمة الجهاز"]
        let english = ["Bell", "Flow", "Received", "Message", "Spark", "Soft message", "Modern", "Quick", "Bright", "System sound"]
        return (ar ? arabic : english)[Self.allCases.firstIndex(of: self)!]
    }
}

enum KeychainStore {
    static func read(_ provider: AIProvider) -> String? {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: "com.fakkerni.reminder.ios.ai", kSecAttrAccount as String: provider.rawValue, kSecReturnData as String: true, kSecMatchLimit as String: kSecMatchLimitOne]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
    static func save(_ key: String, for provider: AIProvider) throws {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: "com.fakkerni.reminder.ios.ai", kSecAttrAccount as String: provider.rawValue]
        let values: [String: Any] = [kSecValueData as String: Data(key.utf8), kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly]
        var status = SecItemUpdate(query as CFDictionary, values as CFDictionary)
        if status == errSecItemNotFound { status = SecItemAdd(query.merging(values) { _, new in new } as CFDictionary, nil) }
        guard status == errSecSuccess else { throw NSError(domain: NSOSStatusErrorDomain, code: Int(status)) }
    }
    static func remove(_ provider: AIProvider) throws {
        let status = SecItemDelete([kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: "com.fakkerni.reminder.ios.ai", kSecAttrAccount as String: provider.rawValue] as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else { throw NSError(domain: NSOSStatusErrorDomain, code: Int(status)) }
    }
}

@MainActor final class AppModel: ObservableObject {
    @Published var reminders: [Reminder] = []
    @Published var name: String { didSet { defaults.set(name, forKey: "name") } }
    @Published var theme: String { didSet { defaults.set(theme, forKey: "theme") } }
    @Published var language: String { didSet { defaults.set(language, forKey: "language") } }
    @Published var onboarded: Bool { didSet { defaults.set(onboarded, forKey: "onboarded") } }
    @Published var provider: AIProvider { didSet { defaults.set(provider.rawValue, forKey: "provider") } }
    @Published var tone: Tone { didSet { defaults.set(tone.rawValue, forKey: "tone") } }
    @Published var connected: Set<AIProvider> = []
    @Published var error: String?
    @Published var notificationsAllowed = false
    private(set) var loadFailed = false
    private let defaults: UserDefaults
    private let store: ReminderFileStore
    let notifications = NotificationScheduler()
    var ar: Bool { language == "ar" }
    var activeProvider: AIProvider? { connected.contains(provider) ? provider : nil }
    var colorScheme: ColorScheme { theme == "light" ? .light : .dark }
    var upcoming: [Reminder] { reminders.filter { !$0.completed && $0.eventTime > Date() }.sorted { $0.eventTime < $1.eventTime } }
    func t(_ arabic: String, _ english: String) -> String { ar ? arabic : english }

    init() {
        var testMode = false
        #if DEBUG
        testMode = ProcessInfo.processInfo.arguments.contains("-ui-testing")
        #endif
        let preferences = testMode ? UserDefaults(suiteName: "fakkerni.ui-tests")! : .standard
        defaults = preferences
        if testMode && ProcessInfo.processInfo.arguments.contains("-reset-fixture") { preferences.removePersistentDomain(forName: "fakkerni.ui-tests") }
        name = preferences.string(forKey: "name") ?? "رفيق"
        theme = preferences.string(forKey: "theme") ?? "dark"
        language = preferences.string(forKey: "language") ?? "ar"
        onboarded = preferences.bool(forKey: "onboarded")
        provider = AIProvider(rawValue: preferences.string(forKey: "provider") ?? "groq") ?? .groq
        tone = Tone(rawValue: preferences.string(forKey: "tone") ?? "bell") ?? .bell
        let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(testMode ? "FakkerniUITests" : "Fakkerni", isDirectory: true)
        store = ReminderFileStore(url: directory.appendingPathComponent("reminders.json"))
        #if DEBUG
        if testMode && ProcessInfo.processInfo.arguments.contains("-reset-fixture") { try? FileManager.default.removeItem(at: store.url) }
        #endif
        do { reminders = try store.load() }
        catch { loadFailed = true; self.error = "تعذّر قراءة التذكيرات. لم نغيّر الملف. / Could not read reminders; the file was preserved." }
        refreshConnections()
        #if DEBUG
        if testMode {
            if ProcessInfo.processInfo.arguments.contains("-skip-onboarding") { onboarded = true }
            if ProcessInfo.processInfo.arguments.contains("-light") { theme = "light" }
            if ProcessInfo.processInfo.arguments.contains("-english") { language = "en" }
        }
        if testMode && ProcessInfo.processInfo.arguments.contains("-screenshots") {
            onboarded = true
            if ProcessInfo.processInfo.arguments.contains("-light") { theme = "light" }
            if ProcessInfo.processInfo.arguments.contains("-english") { language = "en" }
            let now = Date()
            reminders = [Reminder(title: "اتصال بأحمد بخصوص المشروع", eventTime: now.addingTimeInterval(7200), contactName: "أحمد", now: now), Reminder(title: "مراجعة عرض التصميم", eventTime: now.addingTimeInterval(86400), now: now)]
        }
        #endif
    }
    func refreshConnections() { connected = Set(AIProvider.allCases.filter { KeychainStore.read($0) != nil }) }
    func connect(_ newProvider: AIProvider, key: String) async throws {
        try await AIClient().verify(provider: newProvider, key: key)
        try Task.checkCancellation()
        try KeychainStore.save(key, for: newProvider)
        refreshConnections(); provider = newProvider
    }
    func disconnect(_ value: AIProvider) throws { try KeychainStore.remove(value); refreshConnections() }
    func refreshNotificationStatus() async {
        notifications.configureCategories(ar: ar)
        notificationsAllowed = await notifications.isAuthorized()
    }
    func save(_ reminder: Reminder) async throws {
        guard !loadFailed else { throw AppError.storage }
        guard !reminder.title.isEmpty, reminder.title.count <= 250, reminder.eventTime > Date() else { throw AppError.invalidReminder }
        let old = reminders.first { $0.id == reminder.id }
        let count = upcoming.filter { $0.id != reminder.id }.count
        guard count < 60 else { throw AppError.capacity }
        try await notifications.schedule(reminder, tone: tone, ar: ar)
        var next = reminders.filter { $0.id != reminder.id }; next.append(reminder)
        do { try store.save(next); reminders = next; notificationsAllowed = true }
        catch {
            if let old, !old.completed { try? await notifications.schedule(old, tone: tone, ar: ar, askPermission: false) }
            else { notifications.cancel(reminder.id) }
            throw AppError.storage
        }
    }
    func delete(_ reminder: Reminder) {
        let next = reminders.filter { $0.id != reminder.id }
        do { try store.save(next); reminders = next; notifications.cancel(reminder.id) }
        catch { self.error = t("تعذّر حفظ التغيير. جرّب مرة ثانية.", "Could not save the change. Please try again.") }
    }
    func complete(_ reminder: Reminder) {
        var next = reminders
        guard let index = next.firstIndex(where: { $0.id == reminder.id }) else { return }
        next[index].completed = true
        do { try store.save(next); reminders = next; notifications.cancel(reminder.id) }
        catch { self.error = t("تعذّر حفظ التغيير.", "Could not save the change.") }
    }
    func changeTone(_ next: Tone) async throws {
        let previous = tone
        let pending = await notifications.pendingIDs()
        do {
            for reminder in upcoming where pending.contains(reminder.id.uuidString) {
                try await notifications.schedule(reminder, tone: next, ar: ar, askPermission: false)
            }
            tone = next
        } catch {
            for reminder in upcoming where pending.contains(reminder.id.uuidString) {
                try? await notifications.schedule(reminder, tone: previous, ar: ar, askPermission: false)
            }
            throw error
        }
    }
    func message(for error: Error) -> String {
        if let value = error as? AppError {
            switch value {
            case .permission: return t("فعّل إشعارات فكّرني من إعدادات الآيفون، وبعدها احفظ التذكير.", "Enable Fakkerni notifications in iPhone Settings, then save again.")
            case .storage: return t("تعذّر حفظ التذكير على الجهاز. حاول مرة ثانية.", "Could not save the reminder on this device. Try again.")
            case .capacity: return t("عندك 60 تذكير قادم. كمّل أو احذف تذكير قبل الإضافة.", "You have 60 upcoming reminders. Complete or remove one before adding another.")
            case .invalidReminder: return t("اكتب التذكير واختار موعدًا قادمًا.", "Enter a reminder and choose a future time.")
            }
        }
        if let api = error as? AIError {
            switch api {
            case .invalidKey, .http(401), .http(403): return t("المفتاح غير صالح أو الصلاحيات ناقصة. راجع صفحة ربط AI.", "The key is invalid or lacks access. Check AI connections.")
            case .http(429): return t("وصلت لحد الاستعمال. استنّى شوية أو اكتب بيدك.", "The usage limit was reached. Wait a little or type instead.")
            case .unavailableModels: return t("المفتاح يخدم، لكن نماذج الصوت أو التذكير غير متاحة في حسابك. فعّلها من منصة المزود.", "The key works, but the required voice or reminder models are unavailable. Enable them at your provider.")
            case .audioTooLarge: return t("التسجيل طويل أو فارغ. سجّل تذكير قصير.", "The recording is too long or empty. Record a short reminder.")
            default: return t("تعذّر فهم التسجيل. جرّب مرة ثانية أو اكتب بيدك.", "Could not understand the recording. Try again or type instead.")
            }
        }
        return t("العملية ما كملتش. تأكد من الإنترنت وحاول مرة ثانية.", "The action did not finish. Check your connection and try again.")
    }
}

enum AppError: Error { case permission, storage, capacity, invalidReminder }
