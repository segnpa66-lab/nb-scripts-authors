import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import '../web/battle-rules.js';

const config = JSON.parse(readFileSync(new URL('../web/battle-config.json', import.meta.url), 'utf8'));
const { initial, validate } = globalThis.BATTLE_RULES;
const flags = new Set([13,16,17,18,21,23,25,26,27,28,29,30]);

test('unchanged battle settings remain valid when the API reports 0..1 for restrictTeamJoin', () => {
  const parameters = config.ids.map((id, i) => ({ id, default_value: config.defaults[i] }));
  parameters[23] = { id: config.ids[23], default_value: 0, min_value: 0, max_value: 1 };
  const shown = initial(config, parameters, null);
  const submitted = shown.map((value, i) => flags.has(i) ? (value === 1 ? 1 : i === 23 ? -1 : 0) : value);
  assert.equal(submitted[23], -1);
  assert.deepEqual(validate(config, submitted), submitted);
});

test('saved values are clamped to protocol bounds before rendering', () => {
  const saved = [...config.defaults];
  saved[0] = 999;
  saved[23] = -1;
  const values = initial(config, [], saved);
  assert.equal(values[0], config.max[0]);
  assert.equal(values[23], -1);
  assert.equal(validate(config, values)[23], -1);
});
