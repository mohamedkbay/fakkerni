import { mkdirSync, writeFileSync } from 'node:fs';

// Two tiny, original, monophonic alert tones. Run with: node scripts/generate-tones.mjs
const rate = 24000;
const output = new URL('../app/src/main/res/raw/', import.meta.url);
mkdirSync(output, { recursive: true });
function note(t, start, duration, hz, gain, bell = false) {
  const x = t - start;
  if (x < 0 || x >= duration) return 0;
  const attack = Math.min(1, x / 0.012);
  const fade = bell ? Math.exp(-4.2 * x / duration) : Math.min(1, (duration - x) / 0.06);
  const phase = Math.PI * 2 * hz * x;
  const voice = bell
    ? Math.sin(phase) + 0.31 * Math.sin(phase * 2.006) + 0.12 * Math.sin(phase * 3.91)
    : Math.sin(phase) + 0.19 * Math.sin(phase * 2);
  return gain * attack * fade * voice;
}
function write(name, length, synthesis) {
  const count = Math.round(rate * length);
  const wav = Buffer.alloc(44 + count * 2);
  wav.write('RIFF', 0); wav.writeUInt32LE(wav.length - 8, 4);
  wav.write('WAVEfmt ', 8); wav.writeUInt32LE(16, 16);
  wav.writeUInt16LE(1, 20); wav.writeUInt16LE(1, 22);
  wav.writeUInt32LE(rate, 24); wav.writeUInt32LE(rate * 2, 28);
  wav.writeUInt16LE(2, 32); wav.writeUInt16LE(16, 34);
  wav.write('data', 36); wav.writeUInt32LE(count * 2, 40);
  let peak = 0;
  for (let i = 0; i < count; i++) peak = Math.max(peak, Math.abs(synthesis(i / rate)));
  const gain = peak > 0 ? 0.88 / peak : 0;
  for (let i = 0; i < count; i++) {
    const value = Math.max(-1, Math.min(1, synthesis(i / rate) * gain));
    wav.writeInt16LE(Math.round(value * 32767), 44 + i * 2);
  }
  writeFileSync(new URL(name + '.wav', output), wav);
}
write('reminder_chime', 2.0, t =>
  note(t, 0, 0.8, 660, 0.54, true) + note(t, 0.14, 1.15, 880, 0.5, true)
  + note(t, 0.62, 1.16, 990, 0.42, true));
write('reminder_pulse', 1.7, t =>
  note(t, 0, 0.24, 740, 0.54) + note(t, 0.32, 0.24, 740, 0.54)
  + note(t, 0.68, 0.32, 988, 0.62) + note(t, 1.08, 0.32, 988, 0.62));
