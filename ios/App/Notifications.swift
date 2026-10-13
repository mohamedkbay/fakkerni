import UIKit
import UserNotifications
import FakkerniCore

final class NotificationScheduler {
    private let center = UNUserNotificationCenter.current()
    func configureCategories(ar: Bool) {
        let open = UNNotificationAction(identifier: "OPEN", title: ar ? "عرض التذكير" : "Open reminder", options: [.foreground])
        let call = UNNotificationAction(identifier: "CALL", title: ar ? "اتصال" : "Call", options: [.foreground, .authenticationRequired])
        center.setNotificationCategories([
            UNNotificationCategory(identifier: "REMINDER", actions: [open], intentIdentifiers: []),
            UNNotificationCategory(identifier: "REMINDER_CALL", actions: [open, call], intentIdentifiers: [])
        ])
    }
    func isAuthorized() async -> Bool { await center.notificationSettings().authorizationStatus == .authorized }
    func pendingIDs() async -> Set<String> { Set(await center.pendingNotificationRequests().map(\.identifier)) }
    func schedule(_ reminder: Reminder, tone: Tone, ar: Bool, askPermission: Bool = true) async throws {
        if askPermission { guard try await center.requestAuthorization(options: [.alert, .sound, .badge]) else { throw AppError.permission } }
        guard await isAuthorized() else { throw AppError.permission }
        guard reminder.alertTime > Date(), !reminder.completed else { return }
        let content = UNMutableNotificationContent()
        content.title = reminder.title
        let formatter = DateFormatter(); formatter.locale = Locale(identifier: ar ? "ar_LY@numbers=latn" : "en_GB"); formatter.dateFormat = "EEEE d MMM · HH:mm"
        content.body = (ar ? "موعدك: " : "Your event: ") + Digits.latin(formatter.string(from: reminder.eventTime))
        content.sound = tone.filename.map { UNNotificationSound(named: UNNotificationSoundName($0)) } ?? .default
        content.categoryIdentifier = reminder.phone.isEmpty ? "REMINDER" : "REMINDER_CALL"
        content.userInfo = ["reminderID": reminder.id.uuidString, "phone": reminder.phone]
        content.threadIdentifier = "fakkerni-reminders"
        content.interruptionLevel = .active
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, reminder.alertTime.timeIntervalSinceNow), repeats: false)
        try await center.add(UNNotificationRequest(identifier: reminder.id.uuidString, content: content, trigger: trigger))
    }
    func test(tone: Tone, ar: Bool) async throws {
        guard try await center.requestAuthorization(options: [.alert, .sound, .badge]) else { throw AppError.permission }
        let content = UNMutableNotificationContent()
        content.title = ar ? "فكّرني — تجربة التنبيه" : "Fakkerni — alert test"
        content.body = ar ? "التنبيه وصل. هكي بيظهر تذكيرك على شاشة القفل." : "The alert arrived. This is how your reminder appears on the lock screen."
        content.sound = tone.filename.map { UNNotificationSound(named: UNNotificationSoundName($0)) } ?? .default
        try await center.add(UNNotificationRequest(identifier: "fakkerni-test", content: content, trigger: UNTimeIntervalNotificationTrigger(timeInterval: 10, repeats: false)))
    }
    func cancel(_ id: UUID) { center.removePendingNotificationRequests(withIdentifiers: [id.uuidString]); center.removeDeliveredNotifications(withIdentifiers: [id.uuidString]) }
}

@MainActor final class NotificationRouter: ObservableObject {
    static let shared = NotificationRouter()
    @Published var reminderID: UUID?
    var shouldCall = false
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) { completionHandler([.banner, .list, .sound]) }
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let identifier = response.notification.request.content.userInfo["reminderID"] as? String
        let call = response.actionIdentifier == "CALL"
        Task { @MainActor in
            NotificationRouter.shared.shouldCall = call
            NotificationRouter.shared.reminderID = identifier.flatMap(UUID.init(uuidString:))
            completionHandler()
        }
    }
}
