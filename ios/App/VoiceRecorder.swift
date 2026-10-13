import AVFoundation
import SwiftUI
import FakkerniCore

@MainActor final class VoiceRecorder: NSObject, ObservableObject, AVAudioRecorderDelegate {
    @Published var recording = false
    @Published var processing = false
    @Published var level: Double = 0
    @Published var seconds = 0
    @Published var status = ""
    @Published var draft: VoiceDraft?
    @Published var needsConnection = false
    private(set) var holding = false
    private var recorder: AVAudioRecorder?
    private var timer: Timer?
    private var file: URL?
    private var task: Task<Void, Never>?
    private var activeProvider: AIProvider?
    private var activeKey: String?

    func press(_ model: AppModel) {
        guard !holding, !processing, !recording else { return }
        guard let provider = model.activeProvider, let key = KeychainStore.read(provider) else { needsConnection = true; return }
        holding = true; status = ""; activeProvider = provider; activeKey = key
        task = Task { [weak self] in
            let session = AVAudioSession.sharedInstance()
            let allowed = await withCheckedContinuation { continuation in session.requestRecordPermission { continuation.resume(returning: $0) } }
            guard let self, self.holding, !Task.isCancelled else { return }
            guard allowed else { self.holding = false; self.status = model.t("فعّل الميكروفون من إعدادات الآيفون.", "Enable microphone access in iPhone Settings."); return }
            do {
                try session.setCategory(.playAndRecord, mode: .voiceChat, options: [.defaultToSpeaker, .allowBluetooth])
                try session.setActive(true)
                let url = FileManager.default.temporaryDirectory.appendingPathComponent("fakkerni-\(UUID().uuidString).m4a")
                self.file = url
                let recorder = try AVAudioRecorder(url: url, settings: [AVFormatIDKey: kAudioFormatMPEG4AAC, AVSampleRateKey: 24000, AVNumberOfChannelsKey: 1, AVEncoderBitRateKey: 32000, AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue])
                self.recorder = recorder; recorder.delegate = self; recorder.isMeteringEnabled = true
                guard recorder.record() else { throw AppError.storage }
                self.recording = true; self.seconds = 0
                UIImpactFeedbackGenerator(style: .soft).impactOccurred()
                self.timer = Timer.scheduledTimer(withTimeInterval: 0.08, repeats: true) { [weak self] _ in
                    Task { @MainActor in
                        guard let self, let recorder = self.recorder, self.recording else { return }
                        recorder.updateMeters()
                        self.level = self.level * 0.5 + max(0, min(1, (Double(recorder.averagePower(forChannel: 0)) + 55) / 55)) * 0.5
                        self.seconds = Int(recorder.currentTime)
                        if recorder.currentTime >= 60 { self.release(model) }
                    }
                }
            } catch { self.cancel(); self.status = model.t("تعذّر بدء التسجيل. جرّب مرة ثانية.", "Could not start recording. Please try again.") }
        }
    }
    func release(_ model: AppModel) {
        let wasHolding = holding
        holding = false
        guard recording, let recorder, let file, let provider = activeProvider, let key = activeKey else { if wasHolding { task?.cancel() }; return }
        let duration = recorder.currentTime
        recorder.stop(); self.recorder = nil; timer?.invalidate(); timer = nil; recording = false; level = 0
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        defer { try? FileManager.default.removeItem(at: file); self.file = nil; activeKey = nil; activeProvider = nil }
        guard duration >= 0.5, let audio = try? Data(contentsOf: file), !audio.isEmpty else { status = model.t("اضغط وسجّل تذكيرك، وبعدها ارفع صبعك.", "Hold the button while you speak, then release."); return }
        processing = true; status = model.t("نكتب كلامك ونرتّب الموعد…", "Writing your words and finding the time…")
        task = Task { [weak self] in
            do {
                let draft = try await AIClient().transcribe(audio: audio, provider: provider, key: key)
                guard !Task.isCancelled else { return }
                self?.draft = draft; self?.status = ""; self?.processing = false
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            } catch {
                guard !Task.isCancelled else { return }
                self?.processing = false; self?.status = model.message(for: error)
            }
        }
    }
    func toggle(_ model: AppModel) { if recording { release(model) } else { press(model) } }
    func cancel() {
        holding = false; task?.cancel(); task = nil; timer?.invalidate(); timer = nil
        recorder?.stop(); recorder = nil; recording = false; processing = false; level = 0
        if let file { try? FileManager.default.removeItem(at: file) }; file = nil; activeKey = nil; activeProvider = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
    nonisolated func audioRecorderEncodeErrorDidOccur(_ recorder: AVAudioRecorder, error: Error?) {
        Task { @MainActor in self.cancel(); self.status = "تعذّر التسجيل. / Recording failed." }
    }
}
