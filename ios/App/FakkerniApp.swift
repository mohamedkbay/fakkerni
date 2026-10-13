import SwiftUI
import AVFoundation
import FakkerniCore

@main struct FakkerniApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    @StateObject private var model = AppModel()
    var body: some Scene {
        WindowGroup {
            RootView().environmentObject(model)
                .font(.cairo())
                .environment(\.locale, Locale(identifier: model.ar ? "ar_LY@numbers=latn" : "en_GB"))
                .environment(\.layoutDirection, model.ar ? .rightToLeft : .leftToRight)
                .preferredColorScheme(model.colorScheme)
                .tint(Palette.accent)
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var model: AppModel
    @ObservedObject private var router = NotificationRouter.shared
    @Environment(\.scenePhase) private var scenePhase
    @State private var openedReminder: Reminder?
    var body: some View {
        Group { if model.onboarded { AssistantScreen() } else { OnboardingScreen() } }
            .background(Palette.background.ignoresSafeArea())
            .task { await model.refreshNotificationStatus(); openNotification() }
            .onChange(of: scenePhase) { phase in if phase == .active { Task { await model.refreshNotificationStatus() } } }
            .onChange(of: router.reminderID) { _ in openNotification() }
            .sheet(item: $openedReminder) { reminder in ReminderEditor(existing: reminder) }
            .alert(model.t("فكّرني", "Fakkerni"), isPresented: Binding(get: { model.error != nil }, set: { if !$0 { model.error = nil } })) {
                Button(model.t("تم", "Done"), role: .cancel) { model.error = nil }
            } message: { Text(model.error ?? "") }
    }
    private func openNotification() {
        guard let id = router.reminderID, let reminder = model.reminders.first(where: { $0.id == id }) else { return }
        openedReminder = reminder
        if router.shouldCall, let url = URL(string: "tel:" + PhoneNumber.clean(reminder.phone)), !reminder.phone.isEmpty { UIApplication.shared.open(url) }
        router.reminderID = nil; router.shouldCall = false
    }
}

struct OnboardingScreen: View {
    @EnvironmentObject private var model: AppModel
    var body: some View {
        VStack(spacing: 24) {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    AssistantOrb().frame(height: 260)
                    Text(model.t("خلّينا نبدأ بطريقتك.", "Let's make it yours.")).font(.cairo(30, relativeTo: .largeTitle).weight(.semibold))
                    Text(model.t("اختار اسم للمساعد والمظهر. تقدر تغيّرهم بعدين.", "Choose the assistant's name and appearance. Change them any time.")).foregroundStyle(Palette.secondary)
                    TextField(model.t("اسم المساعد", "Assistant name"), text: $model.name).textFieldStyle(.roundedBorder).accessibilityIdentifier("assistant-name").onChange(of: model.name) { model.name = String(Digits.latin($0).prefix(40)) }
                    Picker(model.t("المظهر", "Appearance"), selection: $model.theme) { Text(model.t("داكن", "Dark")).tag("dark"); Text(model.t("فاتح", "Light")).tag("light") }.pickerStyle(.segmented)
                    Picker("Language / اللغة", selection: $model.language) { Text("العربية").tag("ar"); Text("English").tag("en") }.pickerStyle(.segmented)
                }.frame(maxWidth: 560).padding(24).frame(maxWidth: .infinity)
            }
            Button(model.t("ابدأ", "Get started")) {
                model.name = String(model.name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(40)); model.onboarded = true
            }.buttonStyle(PrimaryButtonStyle()).disabled(model.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty).accessibilityIdentifier("get-started").frame(maxWidth: 520).padding(.horizontal, 24).padding(.bottom, 16)
        }.background(Palette.background.ignoresSafeArea())
    }
}

struct AssistantScreen: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @StateObject private var voice = VoiceRecorder()
    @State private var manual = false
    @State private var connections = false
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    AssistantOrb(recording: voice.recording, processing: voice.processing, amplitude: voice.level)
                        .frame(height: dynamicTypeSize.isAccessibilitySize ? 160 : 300).padding(.top, 12)
                    Text(voice.recording ? model.t("نسمع فيك…", "Listening…") : voice.processing ? model.t("لحظة، نرتّبها لك.", "One moment. Putting it together.") : model.t("قولها، ونرتّبها لك.", "Say it. I'll make it a reminder."))
                        .font(.cairo(27, relativeTo: .title).weight(.semibold)).multilineTextAlignment(.center)
                    Text(voice.status.isEmpty ? model.t("شن عندك؟ إمتى؟ ولو فيه شخص، قول اسمه.", "What, when, and who to call—just say it.") : voice.status)
                        .foregroundStyle(Palette.secondary).multilineTextAlignment(.center).accessibilityIdentifier("voice-status")
                    if voice.recording { Text(String(format: "%02d:%02d", voice.seconds/60, voice.seconds%60)).monospacedDigit().environment(\.layoutDirection, .leftToRight) }
                    if voice.processing { Button(model.t("إلغاء", "Cancel")) { voice.cancel(); voice.status = "" }.frame(minHeight: 44) }
                }.padding(.horizontal, 24).frame(maxWidth: 560).frame(maxWidth: .infinity)
            }
            .background(Palette.background.ignoresSafeArea())
            .navigationTitle(model.name).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { NavigationLink { ReminderListScreen() } label: { MorphIcon(name: "clock").frame(width: 24, height: 24).frame(width: 44, height: 44) }.accessibilityLabel(model.t("تذكيراتي", "My reminders")).accessibilityIdentifier("reminders") }
                ToolbarItem(placement: .navigationBarTrailing) { NavigationLink { SettingsScreen() } label: { MorphIcon(name: "settings").frame(width: 24, height: 24).frame(width: 44, height: 44) }.accessibilityLabel(model.t("الإعدادات", "Settings")).accessibilityIdentifier("settings") }
                ToolbarItem(placement: .principal) { Button { connections = true } label: { HStack(spacing: 8) { BrandMark().frame(width: 24, height: 24); VStack(spacing: 0) { Text(model.name).font(.cairo(14, relativeTo: .subheadline)); Text(model.activeProvider?.label ?? model.t("ربط AI", "Connect AI")).font(.cairo(11, relativeTo: .caption2)).foregroundStyle(Palette.secondary) } }.padding(.horizontal, 12).frame(minHeight: 44).background(Palette.surface, in: Capsule()) }.dynamicTypeSize(...DynamicTypeSize.xxxLarge).accessibilityIdentifier("ai-connections") }
            }
            .safeAreaInset(edge: .bottom) {
                VStack(spacing: 12) {
                    HStack(spacing: 28) {
                        Button { voice.cancel(); manual = true } label: { Image(systemName: "keyboard").font(.title3).frame(width: 54, height: 54).background(Palette.surface, in: Circle()) }.accessibilityLabel(model.t("نكتب بيدي", "Type instead")).accessibilityIdentifier("type-instead")
                        Button {} label: {
                            MorphIcon(name: voice.recording ? "stop" : voice.processing ? "loader" : "mic", spinning: voice.processing)
                                .frame(width: 30, height: 30).frame(width: 82, height: 82)
                                .foregroundStyle(Palette.inverse).background(Palette.accent, in: Circle())
                                .overlay(Circle().stroke(Palette.accent.opacity(0.25), lineWidth: 1).padding(-9))
                        }.disabled(voice.processing).accessibilityLabel(voice.recording ? model.t("إيقاف التسجيل", "Stop recording") : model.t("اضغط واحكي", "Hold to speak"))
                            .accessibilityHint(model.t("اضغط مطولًا واحكي، وارفع صبعك للتحليل. مع VoiceOver اضغط مرتين للبدء ومرة ثانية للإيقاف.", "Hold while speaking and release to analyze. With VoiceOver, double-tap to start, then double-tap again to stop."))
                            .accessibilityIdentifier("hold-to-speak")
                            .simultaneousGesture(DragGesture(minimumDistance: 0).onChanged { _ in voice.press(model) }.onEnded { _ in voice.release(model) })
                            .accessibilityAction { voice.toggle(model) }
                        NavigationLink { ReminderListScreen() } label: { MorphIcon(name: "bell").frame(width: 24, height: 24).frame(width: 54, height: 54).background(Palette.surface, in: Circle()) }.accessibilityLabel(model.t("تذكيراتي", "My reminders"))
                    }
                    Text(dynamicTypeSize.isAccessibilitySize ? model.t("اضغط واحكي", "Hold to speak") : model.t("اضغط واحكي · ارفع صبعك لما تكمل", "Hold to speak · release when you're done")).font(.cairo(13, relativeTo: .caption)).foregroundStyle(Palette.secondary)
                        .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true).padding(.horizontal, 20)
                    Text(model.t("ما ينحفظ شي إلا لما توافق", "Nothing saves before you approve")).font(.cairo(12, relativeTo: .caption2)).foregroundStyle(Palette.secondary)
                        .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true).padding(.horizontal, 20)
                }.frame(maxWidth: .infinity).padding(.top, 18).padding(.bottom, 12).background(Palette.background)
            }
            .sheet(isPresented: $manual) { ReminderEditor() }
            .sheet(item: $voice.draft) { draft in ReminderEditor(draft: draft) }
            .sheet(isPresented: $connections) { NavigationStack { ConnectionsScreen(presentedAsSheet: true) }.presentationDragIndicator(.visible) }
            .onChange(of: voice.needsConnection) { needed in if needed { connections = true; voice.needsConnection = false } }
            .onChange(of: scenePhase) { phase in if phase != .active { voice.cancel() } }
            .onReceive(NotificationCenter.default.publisher(for: AVAudioSession.interruptionNotification)) { _ in voice.cancel(); voice.status = model.t("التسجيل توقف. اضغط باش تسجّل من جديد.", "Recording was interrupted. Hold to record again.") }
            .onDisappear { voice.cancel() }
        }
    }
}
