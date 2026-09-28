// Compile Morphicons' pure animation core to native Android polyline frames.
// No JS runtime or WebView is shipped in the APK.
import { resampleIcon, buildPlan, interpPolar, allocOutputs, Spring, SPRING_PRESETS } from 'morphicons';
import { mkdirSync, writeFileSync, copyFileSync } from 'node:fs';
import assert from 'node:assert/strict';

const icons = {
  mic: 'M9 5a3 3 0 0 1 6 0v7a3 3 0 0 1-6 0z M5 10v2a7 7 0 0 0 14 0v-2 M12 19v3 M8 22h8',
  stop: 'M6 6h12v12H6z',
  loader: 'M12 3a9 9 0 1 1-9 9',
  check: 'M5 12l4 4L19 6',
  retry: 'M3 10a9 9 0 1 1 2 8 M3 4v6h6',
  plus: 'M12 5v14 M5 12h14',
  close: 'M6 6l12 12 M18 6L6 18',
  back: 'M15 5l-7 7 7 7 M8 12h13',
  settings: 'M5 3v6 M5 15v6 M12 3v11 M12 20v1 M19 3v1 M19 10v11 M2 9h6v6H2z M9 14h6v6H9z M16 4h6v6h-6z',
  phone: 'M5 3H3v4c0 7.73 6.27 14 14 14h4v-6l-5-2-2 3a14 14 0 0 1-6-6l3-2-2-5z',
  trash: 'M3 6h18 M9 6V3h6v3 M5 6l1 15h12l1-15 M10 10v7 M14 10v7',
  bell: 'M6 9a6 6 0 0 1 12 0v5l2 3H4l2-3z M10 21h4',
  clock: 'M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18 M12 7v5l3 2'
};
const round = values => Array.from(values, n => {
  assert(Number.isFinite(n));
  return Math.round(n * 1000) / 1000;
});
const sampled = Object.fromEntries(Object.entries(icons).map(([k,v]) => [k, resampleIcon(v, 40)]));
const shapes = Object.fromEntries(Object.entries(sampled).map(([k,subs]) => [k,
  subs.map(s => ({p:round(s.pts), closed:s.closed}))]));
const transitions = {};
const pairs = [
  ['mic','stop'], ['stop','loader'], ['loader','check'], ['loader','retry'],
  ['retry','loader'], ['check','stop'], ['stop','mic'], ['retry','stop'],
  ['check','mic'], ['loader','mic'], ['plus','check'], ['trash','close'],
  ['phone','check'], ['bell','check']
];
for (const [from,to] of pairs) {
  const plan = buildPlan(sampled[from], sampled[to]);
  const out = allocOutputs(plan);
  const spring = new Spring();
  spring.config(SPRING_PRESETS.snappy.k, SPRING_PRESETS.snappy.c);
  spring.start();
  const frames = [];
  for (let i=0; i<50; i++) {
    const settled = spring.step(1/60);
    interpPolar(plan, settled ? 1 : spring.x, out);
    frames.push(out.map((p,k) => ({p:round(p), closed:plan.items[k].closed})));
    if(settled) break;
  }
  frames.push(shapes[to]);
  transitions[from+'_'+to] = frames;
}
mkdirSync('app/src/main/assets', {recursive:true});
writeFileSync('app/src/main/assets/morphicons.json', JSON.stringify({shapes,transitions}));
copyFileSync('node_modules/morphicons/LICENSE', 'app/src/main/assets/MORPHICONS-LICENSE.txt');
console.log('Morphicons: '+Object.keys(shapes).length+' icons; '+Object.keys(transitions).length+' verified transitions.');
