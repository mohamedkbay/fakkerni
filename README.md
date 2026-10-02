<div align="center">

# فكّرني · Fakkerni

### Say what you need to do. Review it. Get reminded.

[![Android 8+](https://img.shields.io/badge/Android-8%2B-202625?style=for-the-badge&logo=android&logoColor=white)](#install)
[![Latest release](https://img.shields.io/github/v/release/mohamedkbay/fakkerni?style=for-the-badge&color=66756D&label=RELEASE)](https://github.com/mohamedkbay/fakkerni/releases/latest)
[![Download APK](https://img.shields.io/badge/Download-APK-202625?style=for-the-badge&logo=android&logoColor=white)](https://github.com/mohamedkbay/fakkerni/releases/latest/download/fakkerni.apk)

**[Download the latest APK](https://github.com/mohamedkbay/fakkerni/releases/latest/download/fakkerni.apk)** · [All releases](https://github.com/mohamedkbay/fakkerni/releases) · [Report an issue](https://github.com/mohamedkbay/fakkerni/issues)

<br />

<img src="docs/media/brand.svg" width="160" alt="Fakkerni app mark" />

**v1.5.0** — One active AI connection, two original in-app alert sounds.

</div>

---

Fakkerni is a small, native Android reminder app built for quick, Arabic-first capture. Hold the microphone, speak in Libyan Arabic, review the written draft, and approve it. You can also type a reminder without an AI key or internet connection. Reminders alert you **one hour before** the event.

> “غدوة الساعة 5 العشية ذكّرني نتصل بأحمد.”

## What it does

- **Voice to reminder:** Transcribes Arabic speech, proposes an Arabic task title and local date/time, then waits for your approval. Nothing is scheduled directly from AI output.
- **One active AI:** Connect Groq, OpenAI, or Gemini on a dedicated page. Store more than one key, but activate only one provider at a time. A connected provider switches with one tap.
- **Simple sound choice:** Pick either of two original sounds—Chime or Pulse—and hear a preview inside the app. The 10-second test uses the selected Android notification channel.
- **Local contacts:** Search Arabic or English names and phone numbers. When speech mentions a contact, the app matches it locally; it never uploads the address book. The call action opens the dialer, not an automatic call.
- **Reliable scheduling:** Exact alarm-clock scheduling, a high-importance notification, vibration, and a lock-screen reminder card, subject to Android permissions and the phone's sound/DND settings.
- **Calm interface:** Dark and light modes, Cairo typeface, English digits `0–9` throughout, and locally rendered Morphicons animations.

The app name remains **فكّرني** in both interface languages. The assistant name and theme can be changed in Settings.

## Screens

<table>
<tr><th>Voice assistant</th><th>Review and edit</th><th>Reminders</th></tr>
<tr>
<td><img src="docs/media/assistant.png" width="250" alt="Voice assistant screen" /></td>
<td><img src="docs/media/editor.png" width="250" alt="Reminder editor" /></td>
<td><img src="docs/media/reminders.png" width="250" alt="Reminder list" /></td>
</tr>
</table>

[Watch the short GIF tour](docs/media/app-tour.gif). These are archived captures of the v1.3 interface, **not** screenshots of v1.5. No Android emulator image was available on the build machine for new device screenshots.

## Install

1. Download [fakkerni.apk](https://github.com/mohamedkbay/fakkerni/releases/latest/download/fakkerni.apk) on a phone running Android 8 or newer.
2. Allow installation from the source Android asks about. To update an existing installation, install over it **without uninstalling**, so your reminders stay on the phone.
3. Grant notification and exact-alarm permissions, then use **Settings → Test in 10 seconds** with the screen locked.

The APK is signed with the same local development certificate as the previous release, but is not a Play Store build. Check the release page for the APK SHA-256. A local build signed with your own certificate may not install as an update over the published APK.

## Connect AI

Open the assistant and tap the provider chip, or go to **Settings → AI connections**. Choose one of these supported voice providers:

| Provider | What the app uses | Get a key |
| --- | --- | --- |
| Groq | Whisper Large V3 Turbo + GPT-OSS 20B | [Groq Console](https://console.groq.com/keys) |
| OpenAI | `gpt-4o-mini-transcribe` + `gpt-4.1-mini` | [OpenAI platform](https://platform.openai.com/api-keys) |
| Gemini | `gemini-3.8-flash` for audio and draft extraction | [Google AI Studio](https://aistudio.google.com/app/apikey) |

Tap **Connect**, paste your key, and tap **Verify & connect**. The app verifies the key without sending a recording. After that, tap **Activate in one tap** to switch a previously connected provider. Only one is active; there is no automatic fallback to a different provider after an API failure. Availability, model permissions, and free-tier quotas are controlled by each provider. The basic key check does not guarantee access to every model. Groq users may need to enable the app's models in their [project limits](https://console.groq.com/settings/project/limits).

This is **bring your own API key**, not a universal “sign in with AI” service. Keys are encrypted using Android Keystore, stored outside Android backup, never bundled in the APK, and never written to logs. A key in a personal mobile app is still less secure than a trusted backend; restrict and rotate keys at the provider when possible. Never post a key in an issue, screenshot, or commit.

Audio and spoken text go to the **active** provider only. Contacts and saved reminders stay local. Manual entry and already-scheduled notifications do not use an AI service.

## Alert sounds and lock screen

Settings has an in-app choice between **Chime** and **Pulse**, two small original WAV tones included in the APK. Selecting one creates its Android notification channel, because Android fixes a channel's sound at creation. Use the built-in 10-second alarm test to verify the chosen tone on your actual phone. The app cannot override Do Not Disturb, muted alarm volume, disabled notifications, or manufacturer battery restrictions. Lock-screen content can be seen by others; review your phone's privacy settings.

The source for both sounds is [`scripts/generate-tones.mjs`](scripts/generate-tones.mjs). Run `node scripts/generate-tones.mjs` to regenerate them.

## Build and test

Requirements: JDK 17, Android SDK 36, Build Tools 35.0.0. The Gradle wrapper is included.

```bash
git clone https://github.com/mohamedkbay/fakkerni.git
cd fakkerni
# Set ANDROID_HOME or sdk.dir in an untracked local.properties.
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. No API key is needed to build or run unit tests. Device instrumentation tests under `app/src/androidTest` are intended for an isolated test emulator, not a personal phone. See the [v1.5.0 release notes](docs/releases/v1.5.0.md) for verification details.

Morphicons assets are prebuilt; regenerate them only if needed with `npm ci --ignore-scripts && npm run icons`.

## Credits and licensing

- [Morphicons](https://www.npmjs.com/package/morphicons): [MIT license](app/src/main/assets/MORPHICONS-LICENSE.txt)
- [Cairo](https://fonts.google.com/specimen/Cairo): [SIL Open Font License](docs/licenses/Cairo-OFL.txt)

No general license has yet been declared for the project source code; the third-party component licenses above still apply.
