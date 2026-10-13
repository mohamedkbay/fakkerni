import SwiftUI
import AVFoundation
import FakkerniCore

struct SettingsScreen: View {
    @EnvironmentObject private var model: AppModel
    @State private var testing = false
    @State private var testMessage: String?
    var body: some View {
        Form {
            Section(model.t("مساعدك", "Your assistant")) {
                TextField(model.t("اسم المساعد", "Assistant name"), text: $model.name).onChange(of: model.name) { value in if value.count > 40 { model.name = String(value.prefix(40)) } }
                Picker(model.t("المظهر", "Appearance"), selection: $model.theme) { Text(model.t("داكن", "Dark")).tag("dark"); Text(model.t("فاتح", "Light")).tag("light") }
                Picker("Language / اللغة", selection: $model.language) { Text("العربية").tag("ar"); Text("English").tag("en") }
            }.listRowBackground(Palette.surface)
            Section(model.t("الذكاء الاصطناعي", "Artificial intelligence")) {
                NavigationLink { ConnectionsScreen() } label: { HStack { Text(model.t("ربط AI", "AI connections")); Spacer(); Text(model.activeProvider?.label ?? model.t("غير مربوط", "Not connected")).foregroundStyle(.secondary) } }.accessibilityIdentifier("settings-ai")
            }.listRowBackground(Palette.surface)
            Section {
                NavigationLink { SoundPickerScreen() } label: { HStack { Text(model.t("نغمة التنبيه", "Alert sound")); Spacer(); Text(model.tone.label(model.ar)).foregroundStyle(.secondary) } }.accessibilityIdentifier("alert-sounds")
                Button { test() } label: { HStack { MorphIcon(name: "bell").frame(width: 22, height: 22); Text(model.t("جرّب التنبيه بعد 10 ثواني", "Test alert in 10 seconds")); if testing { Spacer(); ProgressView() } }.frame(minHeight: 44) }.disabled(testing).accessibilityIdentifier("test-alert")
                if let testMessage { Text(testMessage).font(.cairo(13, relativeTo: .caption)).foregroundStyle(.secondary) }
                HStack { Text(model.t("الإشعارات", "Notifications")); Spacer(); Text(model.notificationsAllowed ? model.t("مفعّلة", "Enabled") : model.t("تحتاج إذن", "Permission needed")).foregroundStyle(.secondary) }
                Button(model.t("إعدادات إشعارات الآيفون", "iPhone notification settings")) { UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!) }
            } header: { Text(model.t("التنبيهات", "Alerts")) } footer: { Text(model.t("اقفل الشاشة للتجربة. الصوت وشاشة القفل يتبعوا إعدادات الآيفون والصامت والتركيز.", "Lock the screen for the test. Sound and lock-screen display follow your iPhone notification, Silent Mode, and Focus settings.")) }
                .listRowBackground(Palette.surface)
            Section { Text("فكّرني · 1.6.0").frame(maxWidth: .infinity).foregroundStyle(.secondary).font(.cairo(13, relativeTo: .caption)) }.listRowBackground(Color.clear)
        }.scrollContentBackground(.hidden).background(Palette.background)
            .navigationTitle(model.t("الإعدادات", "Settings")).task { await model.refreshNotificationStatus() }
    }
    private func test() {
        testing = true; testMessage = nil
        Task {
            do { try await model.notifications.test(tone: model.tone, ar: model.ar); testMessage = model.t("تجهّز. اقفل الشاشة توا، واستنّى 10 ثواني.", "Ready. Lock your screen now and wait 10 seconds."); await model.refreshNotificationStatus() }
            catch { testMessage = model.message(for: error) }
            testing = false
        }
    }
}

struct SoundPickerScreen: View {
    @EnvironmentObject private var model: AppModel
    @State private var player: AVAudioPlayer?
    @State private var changing = false
    @State private var failure: String?
    var body: some View {
        List {
            Section {
                ForEach(Tone.allCases) { tone in
                    Button { choose(tone) } label: {
                        HStack { Text(tone.label(model.ar)).foregroundStyle(.primary); Spacer(); if tone == model.tone { MorphIcon(name: "check").frame(width: 22, height: 22) } }.frame(minHeight: 44)
                    }.disabled(changing).accessibilityIdentifier("tone-" + tone.rawValue)
                        .accessibilityAddTraits(tone == model.tone ? .isSelected : [])
                }
            } footer: { Text(model.t("اضغط على إحدى النغمات باش تسمعها وتختارها. نغمة الجهاز تستعمل الصوت الافتراضي، وتقدر تجربها بزر التنبيه بعد 10 ثواني.", "Tap a bundled tone to preview and select it. System sound uses the default notification sound; preview it with the 10-second alert test.")) }
            if let failure { Text(failure).foregroundStyle(.red) }
        }.navigationTitle(model.t("نغمة التنبيه", "Alert sound"))
            .scrollContentBackground(.hidden).background(Palette.background)
            .onDisappear { player?.stop(); player = nil; try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation) }
    }
    private func choose(_ tone: Tone) {
        changing = true; failure = nil; player?.stop()
        Task {
            do {
                try await model.changeTone(tone)
                if let filename = tone.filename, let url = Bundle.main.url(forResource: filename.replacingOccurrences(of: ".caf", with: ""), withExtension: "caf") {
                    try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
                    try AVAudioSession.sharedInstance().setActive(true)
                    player = try AVAudioPlayer(contentsOf: url); player?.play()
                }
            } catch { failure = model.message(for: error) }
            changing = false
        }
    }
}

struct ConnectionsScreen: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var connecting: AIProvider?
    @State private var removing: AIProvider?
    var body: some View {
        List {
            Section {
                Text(model.t("اربط بالمفتاح مرة واحدة، وفعّل مزود واحد فقط. التسجيل يمشي للمزود المفعّل وحده.", "Save your key once and activate one provider. Recordings go only to the active provider."))
                    .foregroundStyle(.secondary)
            }.listRowBackground(Color.clear)
            ForEach(AIProvider.allCases) { provider in
                Section(provider.label) {
                    if model.connected.contains(provider) {
                        HStack { Text(model.activeProvider == provider ? model.t("مفعّل", "Active") : model.t("مربوط", "Connected")); Spacer(); MorphIcon(name: model.activeProvider == provider ? "check" : "plus").frame(width: 22, height: 22) }
                        if model.activeProvider != provider { Button(model.t("تفعيل بضغطة", "Activate in one tap")) { model.provider = provider } }
                        Button(model.t("تغيير المفتاح", "Change key")) { connecting = provider }
                        Button(model.t("فصل الربط", "Disconnect"), role: .destructive) { removing = provider }
                    } else { Button(model.t("ربط ", "Connect ") + provider.label) { connecting = provider }.accessibilityIdentifier("connect-" + provider.rawValue) }
                    Link(model.t("ناخذ مفتاح من المنصة", "Get a key from the provider"), destination: provider.keysURL)
                }.listRowBackground(Palette.surface)
            }
            Section { Text(model.t("الأسماء والتذكيرات يبقوا على جهازك. المفاتيح محفوظة في Keychain. الكتابة اليدوية ما تحتاجش AI.", "Contacts and reminders stay on your device. Keys are saved in Keychain. Manual entry needs no AI.")) }.font(.cairo(13, relativeTo: .caption)).foregroundStyle(.secondary).listRowBackground(Color.clear)
        }.navigationTitle(model.t("ربط AI", "AI connections")).navigationBarTitleDisplayMode(.inline)
            .scrollContentBackground(.hidden).background(Palette.background)
            .sheet(item: $connecting) { provider in ConnectionEditor(provider: provider) }
            .confirmationDialog(model.t("تفصل الربط؟", "Disconnect this provider?"), isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } }), titleVisibility: .visible) {
                if let value = removing { Button(model.t("فصل", "Disconnect"), role: .destructive) { do { try model.disconnect(value) } catch { model.error = model.message(for: error) }; removing = nil } }
            }
    }
}

struct ConnectionEditor: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let provider: AIProvider
    @State private var key = ""
    @State private var verifying = false
    @State private var failure: String?
    @State private var task: Task<Void, Never>?
    var body: some View {
        NavigationStack {
            Form {
                Section(provider.label) {
                    SecureField("API key", text: $key).textInputAutocapitalization(.never).autocorrectionDisabled().keyboardType(.asciiCapable).privacySensitive().environment(\.layoutDirection, .leftToRight).accessibilityIdentifier("api-key")
                    Link(model.t("افتح صفحة المفاتيح", "Open the key page"), destination: provider.keysURL)
                }
                Section { Button { connect() } label: { HStack { Text(model.t("تحقق واربط", "Verify & connect")); if verifying { Spacer(); ProgressView() } } }.disabled(verifying || !provider.accepts(key.trimmingCharacters(in: .whitespacesAndNewlines))).accessibilityIdentifier("verify-connect") }
                if let failure { Section { Text(failure).foregroundStyle(.red) } }
                Section { Text(model.t("المفتاح ينحفظ مرة واحدة. التحقق ما يرسلش تسجيل ولا أسماء.", "Your key is saved once. Verification sends no recording or contacts.")) }.foregroundStyle(.secondary)
            }.navigationTitle(model.t("ربط ", "Connect ") + provider.label).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(model.t("إلغاء", "Cancel")) { task?.cancel(); dismiss() } } }
                .onDisappear { task?.cancel(); key = "" }
        }
    }
    private func connect() {
        verifying = true; failure = nil
        let value = key.trimmingCharacters(in: .whitespacesAndNewlines)
        task = Task {
            do { try await model.connect(provider, key: value); guard !Task.isCancelled else { return }; key = ""; dismiss() }
            catch { guard !Task.isCancelled else { return }; failure = model.message(for: error); verifying = false }
        }
    }
}
