import SwiftUI
import AVFoundation
import SafariServices
import FakkerniCore

struct SettingsScreen: View {
    @EnvironmentObject private var model: AppModel
    var body: some View {
        Form {
            Section(model.t("مساعدك", "Your assistant")) {
                TextField(model.t("اسم المساعد", "Assistant name"), text: $model.name).onChange(of: model.name) { model.name = String(Digits.latin($0).prefix(40)) }
                Picker(model.t("المظهر", "Appearance"), selection: $model.theme) { Text(model.t("داكن", "Dark")).tag("dark"); Text(model.t("فاتح", "Light")).tag("light") }
                Picker("Language / اللغة", selection: $model.language) { Text("العربية").tag("ar"); Text("English").tag("en") }
            }.listRowBackground(Palette.surface)
            Section(model.t("الذكاء الاصطناعي", "Artificial intelligence")) {
                NavigationLink { ConnectionsScreen() } label: { HStack { Text(model.t("ربط AI", "AI connections")); Spacer(); Text(model.activeProvider?.label ?? model.t("غير مربوط", "Not connected")).foregroundStyle(Palette.secondary) } }.accessibilityIdentifier("settings-ai")
            }.listRowBackground(Palette.surface)
            Section {
                NavigationLink { SoundPickerScreen() } label: { HStack { Text(model.t("نغمة التنبيه", "Alert sound")); Spacer(); Text(model.tone.label(model.ar)).foregroundStyle(Palette.secondary) } }.accessibilityIdentifier("alert-sounds")
                HStack { Text(model.t("الإشعارات", "Notifications")); Spacer(); Text(model.notificationsAllowed ? model.t("مفعّلة", "Enabled") : model.t("تحتاج إذن", "Permission needed")).foregroundStyle(Palette.secondary) }
                Button(model.t("إعدادات إشعارات الآيفون", "iPhone notification settings")) { UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!) }
            } header: { Text(model.t("التنبيهات", "Alerts")) } footer: { Text(model.t("التنبيهات تتجهّز تلقائياً لما تحفظ التذكير. الصوت وشاشة القفل يتبعوا إعدادات الآيفون والصامت والتركيز.", "Alerts are scheduled automatically when you save a reminder. Sound and lock-screen display follow your iPhone notification, Silent Mode, and Focus settings.")) }
                .listRowBackground(Palette.surface)
            Section { Text("فكّرني · 1.6.0").frame(maxWidth: .infinity).foregroundStyle(Palette.secondary).font(.cairo(13, relativeTo: .caption)) }.listRowBackground(Color.clear)
        }.scrollContentBackground(.hidden).background(Palette.background)
            .navigationTitle(model.t("الإعدادات", "Settings")).task { await model.refreshNotificationStatus() }
    }
}

struct SoundPickerScreen: View {
    @EnvironmentObject private var model: AppModel
    @State private var player: AVAudioPlayer?
    @State private var changing = false
    @State private var failure: String?
    @State private var selectionTask: Task<Void, Never>?
    var body: some View {
        List {
            Section {
                ForEach(Tone.allCases) { tone in
                    Button { choose(tone) } label: {
                        HStack { Text(tone.label(model.ar)).foregroundStyle(.primary); Spacer(); if tone == model.tone { MorphIcon(name: "check").frame(width: 22, height: 22) } }.frame(minHeight: 44)
                    }.disabled(changing).accessibilityIdentifier("tone-" + tone.rawValue)
                        .accessibilityAddTraits(tone == model.tone ? .isSelected : [])
                }
            } footer: { Text(model.t("اضغط على إحدى النغمات باش تسمعها وتختارها. نغمة الجهاز تستعمل صوت الإشعارات الافتراضي.", "Tap a bundled tone to preview and select it. System sound uses the default notification sound.")) }
            if let failure { Text(failure).foregroundStyle(.red) }
        }.navigationTitle(model.t("نغمة التنبيه", "Alert sound"))
            .scrollContentBackground(.hidden).background(Palette.background)
            .onDisappear { selectionTask?.cancel(); player?.stop(); player = nil; try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation) }
    }
    private func choose(_ tone: Tone) {
        changing = true; failure = nil; player?.stop()
        selectionTask = Task {
            do {
                try await model.changeTone(tone)
                guard !Task.isCancelled else { return }
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
    @State private var returnAfterConnection = false
    var presentedAsSheet = false
    var body: some View {
        List {
            Section {
                Text(model.t("اختار الخدمة، ادخل لحسابك أو افتح حساب جديد، واربطه مرة واحدة. خدمة واحدة فقط تكون مفعّلة.", "Choose a service, sign in or create an account, then connect it once. Only one service is active at a time."))
                    .foregroundStyle(Palette.secondary)
            }.listRowBackground(Color.clear)
            ForEach(AIProvider.allCases) { provider in
                Section(provider.label) {
                    if model.connected.contains(provider) {
                        HStack { Text(model.activeProvider == provider ? model.t("مفعّل", "Active") : model.t("مربوط", "Connected")); Spacer(); MorphIcon(name: model.activeProvider == provider ? "check" : "plus").frame(width: 22, height: 22) }
                        if model.activeProvider != provider { Button(model.t("تفعيل بضغطة", "Activate in one tap")) { model.provider = provider } }
                        Button(model.t("تغيير المفتاح", "Change key")) { connecting = provider }
                        Button(model.t("فصل الربط", "Disconnect"), role: .destructive) { removing = provider }
                    } else { Button(model.t("ربط ", "Connect ") + provider.label) { connecting = provider }.accessibilityIdentifier("connect-" + provider.rawValue) }
                }.listRowBackground(Palette.surface)
            }
            Section { Text(model.t("الأسماء والتذكيرات يبقوا على جهازك. المفاتيح محفوظة في Keychain. الكتابة اليدوية ما تحتاجش AI.", "Contacts and reminders stay on your device. Keys are saved in Keychain. Manual entry needs no AI.")) }.font(.cairo(13, relativeTo: .caption)).foregroundStyle(Palette.secondary).listRowBackground(Color.clear)
        }.navigationTitle(model.t("ربط AI", "AI connections")).navigationBarTitleDisplayMode(.inline)
            .scrollContentBackground(.hidden).background(Palette.background)
            .toolbar {
                if presentedAsSheet {
                    ToolbarItem(placement: .confirmationAction) { Button(model.t("تم", "Done")) { dismiss() }.accessibilityIdentifier("close-connections") }
                }
            }
            .sheet(item: $connecting, onDismiss: {
                if returnAfterConnection { returnAfterConnection = false; dismiss() }
            }) { provider in ConnectionEditor(provider: provider, onComplete: { returnAfterConnection = true; connecting = nil }) }
            .confirmationDialog(model.t("تفصل الربط؟", "Disconnect this provider?"), isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } }), titleVisibility: .visible) {
                if let value = removing { Button(model.t("فصل", "Disconnect"), role: .destructive) { do { try model.disconnect(value) } catch { model.error = model.message(for: error) }; removing = nil } }
            }
    }
}

struct ConnectionEditor: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let provider: AIProvider
    var onComplete: () -> Void = {}
    @State private var key = ""
    @State private var verifying = false
    @State private var failure: String?
    @State private var task: Task<Void, Never>?
    @State private var enteringKey = false
    @State private var showingAccount = false
    @State private var connected = false
    var body: some View {
        NavigationStack {
            if connected {
                VStack(spacing: 20) {
                    MorphIcon(name: "check").frame(width: 56, height: 56)
                    Text(model.t("تم ربط ", "Connected to ") + provider.label).font(.cairo(26, relativeTo: .title2))
                    Text(model.t("حسابك جاهز. توا ارجع للمساعد وسجّل تذكيرك.", "Your account is ready. Return to your assistant and record a reminder.")).foregroundStyle(Palette.secondary).multilineTextAlignment(.center)
                    Button(model.t("الرجوع للبرنامج", "Return to Fakkerni")) { onComplete() }.buttonStyle(PrimaryButtonStyle()).accessibilityIdentifier("finish-connection")
                }.padding(24).frame(maxWidth: 520).frame(maxWidth: .infinity, maxHeight: .infinity).background(Palette.background)
                    .interactiveDismissDisabled()
            } else {
            Form {
                Section {
                    HStack(spacing: 14) {
                        BrandMark().frame(width: 32, height: 32).accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(provider.label).font(.cairo(24, relativeTo: .title2))
                            Text(model.t(enteringKey ? "آخر خطوة للربط" : "مساعدك، بحسابك", enteringKey ? "One last step to connect" : "Your assistant, your account")).foregroundStyle(Palette.secondary)
                        }
                    }.padding(.vertical, 8)
                    Text(model.t("تسجيل الدخول يصير في موقع الخدمة الرسمي. بعده أنشئ مفتاح API وانسخه؛ كلمة مرورك تبقى عند الخدمة.", "Sign in on the provider’s official website, then create and copy an API key. Your password stays with the provider."))
                        .font(.cairo(14, relativeTo: .subheadline)).foregroundStyle(Palette.secondary)
                    Button { showingAccount = true } label: {
                        HStack {
                            Text(model.t("ادخل أو افتح حساب", "Sign in or create account"))
                            Spacer()
                            MorphIcon(name: "back").rotationEffect(.degrees(135)).frame(width: 22, height: 22).accessibilityHidden(true)
                        }.frame(minHeight: 44)
                    }.accessibilityIdentifier("provider-account").disabled(verifying)
                    Text(provider.keysURL.host ?? "").font(.cairo(12, relativeTo: .caption)).foregroundStyle(Palette.secondary).environment(\.layoutDirection, .leftToRight)
                }.listRowBackground(Palette.surface)
                if enteringKey {
                    Section {
                        SecureField("API key", text: $key).textInputAutocapitalization(.never).autocorrectionDisabled().keyboardType(.asciiCapable).privacySensitive().environment(\.layoutDirection, .leftToRight).accessibilityIdentifier("api-key").disabled(verifying)
                            .onChange(of: key) { _ in failure = nil }
                        PasteButton(payloadType: String.self) { values in
                            if let value = values.first { key = value.trimmingCharacters(in: .whitespacesAndNewlines); failure = nil }
                        }.labelStyle(.titleAndIcon).disabled(verifying).accessibilityIdentifier("paste-provider-key")
                        if !key.isEmpty && !provider.accepts(key.trimmingCharacters(in: .whitespacesAndNewlines)) {
                            Text(model.t("الصق مفتاح API كامل من صفحة ", "Paste a complete API key from ") + provider.label + model.t("، مش كلمة مرور الحساب.", ", not your account password."))
                                .font(.cairo(13, relativeTo: .caption)).foregroundStyle(Palette.secondary).accessibilityIdentifier("key-format-help")
                        }
                    } header: { Text(model.t("الصق المفتاح مرة واحدة", "Paste your key once")) } footer: {
                        Text(model.t("الرجوع من الموقع ما يعنيش إن الحساب مربوط. نتحقق من المفتاح قبل التفعيل.", "Returning from the website does not connect your account. We verify the key before activation."))
                    }.listRowBackground(Palette.surface)
                } else {
                    Section { Button(model.t("عندي مفتاح، نكمّل الربط", "I have a key — continue")) { enteringKey = true }.frame(minHeight: 44).accessibilityIdentifier("have-provider-key") }.listRowBackground(Palette.surface)
                }
                if let failure { Section { Text(failure).foregroundStyle(.red) } }
                Section { Text(model.t("المفتاح ينحفظ مرة واحدة. التحقق ما يرسلش تسجيل ولا أسماء.", "Your key is saved once. Verification sends no recording or contacts.")) }.foregroundStyle(Palette.secondary)
            }.navigationTitle(model.t("ربط ", "Connect ") + provider.label).navigationBarTitleDisplayMode(.inline)
                .scrollContentBackground(.hidden).background(Palette.background)
                .safeAreaInset(edge: .bottom) {
                    if enteringKey {
                        Button { connect() } label: { HStack { Text(model.t("تحقق واربط", "Verify & connect")); if verifying { ProgressView().tint(Palette.inverse) } } }
                            .buttonStyle(PrimaryButtonStyle()).disabled(verifying || !provider.accepts(key.trimmingCharacters(in: .whitespacesAndNewlines)))
                            .accessibilityIdentifier("verify-connect").padding(20).background(Palette.background)
                    }
                }
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(model.t("إلغاء", "Cancel")) { task?.cancel(); dismiss() }.accessibilityIdentifier("cancel-connection") } }
                // Keep an app-owned return action outside Safari's toolbar so
                // users can always continue, even while the provider page loads.
                .fullScreenCover(isPresented: $showingAccount, onDismiss: { enteringKey = true }) {
                    VStack(spacing: 0) {
                        HStack {
                            Text(provider.label).font(.cairo(16, relativeTo: .headline))
                            Spacer()
                            Button(model.t("الرجوع للربط", "Back to connection")) { closeAccount() }
                                .font(.cairo(15, relativeTo: .body))
                                .frame(minHeight: 48)
                                .accessibilityIdentifier("return-from-provider")
                        }.padding(.horizontal, 20).background(Palette.background)
                        ProviderAccountBrowser(url: provider.keysURL, onClose: closeAccount)
                    }.background(Palette.background)
                }
            }
        }.onDisappear { if !showingAccount { task?.cancel(); key = "" } }
    }
    private func closeAccount() { showingAccount = false; enteringKey = true }
    private func connect() {
        verifying = true; failure = nil
        let value = key.trimmingCharacters(in: .whitespacesAndNewlines)
        task = Task {
            do { try await model.connect(provider, key: value); guard !Task.isCancelled else { return }; key = ""; connected = true; verifying = false; UINotificationFeedbackGenerator().notificationOccurred(.success) }
            catch { guard !Task.isCancelled else { return }; failure = model.message(for: error); verifying = false }
        }
    }
}

/// The system browser owns login cookies and credentials; the app never reads the page.
private struct ProviderAccountBrowser: UIViewControllerRepresentable {
    let url: URL
    var onClose: () -> Void
    func makeCoordinator() -> Coordinator { Coordinator(onClose: onClose) }
    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.dismissButtonStyle = .done
        controller.delegate = context.coordinator
        return controller
    }
    func updateUIViewController(_ controller: SFSafariViewController, context: Context) {}
    final class Coordinator: NSObject, SFSafariViewControllerDelegate {
        let onClose: () -> Void
        init(onClose: @escaping () -> Void) { self.onClose = onClose }
        func safariViewControllerDidFinish(_ controller: SFSafariViewController) { onClose() }
    }
}
