import { mkdirSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

// Re-encode the nine user-supplied files for a compact, device-compatible APK.
// Usage: node scripts/prepare-alert-sounds.mjs /path/to/source-mp3-directory
const source = process.argv[2];
if (!source) throw new Error('Pass the directory containing the nine source MP3 files.');
const output = new URL('../app/src/main/res/raw/', import.meta.url);
mkdirSync(output, { recursive: true });
const sounds = [
  ['digitalstore07-bell-notification-430417.mp3', 'tone_bell.mp3'],
  ['soumages-notification-sounds-351833.mp3', 'tone_flow.mp3'],
  ['digitalstore07-sms-received-430428.mp3', 'tone_received.mp3'],
  ['digitalstore07-sms-notification-430383.mp3', 'tone_message.mp3'],
  ['universfield-new-notification-044-494239.mp3', 'tone_spark.mp3'],
  ['universfield-message-notification-199577.mp3', 'tone_soft_message.mp3'],
  ['lesiakower-modern-notification-sound-effect-481507.mp3', 'tone_modern.mp3'],
  ['universfield-new-notification-056-494256.mp3', 'tone_quick.mp3'],
  ['universfield-bright-notifications-151766.mp3', 'tone_bright.mp3'],
];
for (const [input, name] of sounds) {
  const result = spawnSync('ffmpeg', [
    '-y', '-v', 'error', '-i', join(source, input), '-vn', '-ac', '1', '-ar', '24000',
    '-c:a', 'libmp3lame', '-b:a', '48k', '-map_metadata', '-1', '-id3v2_version', '0',
    fileURLToPath(new URL(name, output)),
  ], { stdio: 'inherit' });
  if (result.status !== 0) throw new Error(`Could not process ${input}`);
}
