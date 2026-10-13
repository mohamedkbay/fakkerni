import { copyFileSync, mkdirSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../', import.meta.url));
const output = `${root}ios/Resources/`;
mkdirSync(output, { recursive: true });
copyFileSync(`${root}app/src/main/res/font/cairo.ttf`, `${output}cairo.ttf`);
copyFileSync(`${root}app/src/main/assets/morphicons.json`, `${output}morphicons.json`);
copyFileSync(`${root}app/src/main/assets/MORPHICONS-LICENSE.txt`, `${output}MORPHICONS-LICENSE.txt`);
copyFileSync(`${root}docs/licenses/Cairo-OFL.txt`, `${output}Cairo-OFL.txt`);
for (const id of ['bell', 'flow', 'received', 'message', 'spark', 'soft_message', 'modern', 'quick', 'bright']) {
  execFileSync('/usr/bin/afconvert', ['-f', 'caff', '-d', 'ima4', `${root}app/src/main/res/raw/tone_${id}.mp3`, `${output}tone_${id}.caf`]);
}
execFileSync('/usr/bin/swift', [`${root}ios/Scripts/GenerateIcon.swift`, `${output}Assets.xcassets/AppIcon.appiconset/AppIcon.png`]);
