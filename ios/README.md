# Fakkerni for iPhone and iPad

A native SwiftUI companion to the Android app. Arabic-first, Cairo typography, the silver animated assistant, actual Morphicons frames, and light/dark modes. Requires **iOS 16 or later**.

## Install on your own iPhone without publishing to the App Store

The release's `fakkerni-ios-unsigned.ipa` is a device build for developers to sign. **It cannot be installed directly from GitHub.** An APK cannot be installed on iOS either.

For the free personal-device route:

1. Install **Xcode 16 or later** on a compatible Mac. Open `ios/Fakkerni.xcodeproj` from the source download or repository.
2. In Xcode → Settings → Accounts, sign in to your Apple Account locally. Do not share your password, certificate, or signing key in this repository.
3. Select the **Fakkerni** target → Signing & Capabilities. Enable automatic signing and choose your **Personal Team**. If the bundle identifier is unavailable, change it to your own unique identifier, such as `com.yourname.fakkerni`.
4. Connect and trust your iPhone, select it as the run destination, and enable **Developer Mode** if Xcode requests it. Press Run. Follow any device trust prompts in iPhone Settings.
5. With a free Personal Team, provisioning expires after **7 days**; rebuild and reinstall using Xcode. Keep the same bundle identifier/team to preserve local data where possible. This is personal testing, not public one-tap distribution.

A paid Apple Developer membership enables Ad Hoc distribution to registered devices or TestFlight testing. This repository contains no distribution certificate or provisioning profile and does not publish an App Store listing. See [Apple account limits](https://developer.apple.com/help/account/basics/about-your-developer-account), [running on a device](https://developer.apple.com/documentation/xcode/running-your-app-in-simulator-or-on-a-device), and [TestFlight](https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview/).

## Use

- First choose your assistant's name, language, and appearance.
- Open **AI connections**, choose Groq, OpenAI, or Gemini, enter your own key, and tap **Verify & connect**. Verification checks model access without sending audio. Only one provider is active; requests never silently fall back to another provider.
- Hold the microphone while you speak in Libyan Arabic, then release. A recording lasts at most 60 seconds. With VoiceOver, double-tap to start and again to stop. You can cancel processing or type instead.
- Review the transcript, task, event time, and person. A missing or ambiguous date/time stays unselected. Nothing is scheduled until you approve it.
- Choose a person by searching Arabic names, English names, or digits. Cross-script common-name matching is local and approximate. Multiple matches require a choice; the app never asks AI to invent a phone number or uploads the address book.
- Notifications normally arrive **one hour before** the event. For an event less than an hour away, the alert arrives shortly after saving. A reminder is not reported as saved if notification permission or scheduling fails.
- Long-press a reminder to call its selected person. The call action uses iOS's system phone flow and may require confirmation. You can also use the Call action on a reminder notification after unlocking.

## Notifications and sounds

Settings offers the nine owner-supplied sounds, converted to small IMA4 CAF files for iOS, plus **System sound**. Choosing a bundled sound previews it and updates pending reminders. iOS does not expose Android's phone-ringtone picker; System sound uses the system notification default.

Use **Settings → Test alert in 10 seconds**, lock the screen, and verify sound and display on your real phone. The system delivers scheduled local notifications even while the app is not running, subject to permissions, notification previews, Silent Mode, Focus, and volume settings. This app does not request Critical Alerts or claim to bypass those settings. The lock-screen card uses iOS's native notification presentation.

The iOS companion permits up to **60 upcoming reminders** so it stays within the system's finite pending-notification capacity and leaves room for the test alert. Completion/deletion cancels pending notifications. A stored alert time is not regenerated at every launch, preventing already-delivered reminders from sounding again. Notifications, microphone capture, contacts permissions, and audio must also be checked on actual hardware.

## AI and privacy

| Provider | Transcription | Reminder extraction |
| --- | --- | --- |
| Groq | `whisper-large-v3-turbo`, explicitly `language=ar` | `openai/gpt-oss-20b`, strict JSON schema |
| OpenAI | `gpt-4o-mini-transcribe`, explicitly `language=ar` | `gpt-4.1-mini`, Responses API, `store=false` |
| Gemini | `gemini-3.8-flash`, Arabic transcription prompt | Same request, structured JSON |

The app sends only the recording/transcript and current time/time zone to the selected provider. Spoken names/numbers are part of that content; the address book is not sent. Provider account policies, retention, pricing, quotas, and model permissions still apply. A successful transcription is retained as an editable draft if separate time extraction fails. API responses cannot schedule reminders directly.

Keys live in **Keychain**, unlocked-device-only and not migratable to another device. The source and binaries contain no keys. Temporary recordings are deleted after reading or cancellation; recording/processing is cancelled when the app leaves the foreground or audio is interrupted. There is no Android/iOS reminder sync or automatic key transfer. The iOS app has its own local JSON store.

## Build and verify

The checked-in Xcode project is ready to open. To regenerate it after changing `project.yml`, install [XcodeGen](https://github.com/yonaskolb/XcodeGen), then run:

```sh
cd ios
xcodegen generate
swift test
xcodebuild -project Fakkerni.xcodeproj -scheme Fakkerni \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  CODE_SIGNING_ALLOWED=NO test
```

Choose an installed Simulator name from `xcrun simctl list devices available`. `Scripts/ci.sh` selects available iPhone/iPad simulators, runs the UI tests, captures native dark/light screenshots, builds a generic iOS device binary, and packages the **unsigned** IPA. `.github/workflows/ios.yml` runs this on a GitHub-hosted Mac. API tests use synthetic recordings and mocked responses; they do not spend real API credits.

On a Mac with only Command Line Tools, the core library can still be compiled and checked without Xcode:

```sh
swiftc ios/Sources/FakkerniCore/*.swift ios/Scripts/CoreCheck.swift -o /tmp/fakkerni-core-check
/tmp/fakkerni-core-check
```

The full XCTest/Simulator build requires Xcode. Debug-only UI-test fixtures use a separate store and sample reminders; they are excluded from release behavior. CI screenshots use clearly synthetic reminder data, not a user's contacts or API keys.

## Assets

Run `node scripts/prepare-ios-assets.mjs` from the repository on macOS to copy Cairo/Morphicons, convert the nine existing compact MP3s with Apple's `afconvert`, and render the existing vector mark as an app icon. See the bundled font/icon licenses. The source recordings were supplied by the project owner; their original creators' rights remain unchanged.
