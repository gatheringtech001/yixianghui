import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { publishMedia, publicationBatches, publishSnapshot, refreshDocuments } from './publish-media.mjs';

test('post-retirement text refresh neither overwrites nor republishes media snapshot', async () => {
  let script;
  const snapshot = { version: 1, records: [], textOnly: true, expectedDocuments: [{ token: 'new', title: 'current' }] };
  const hash = crypto.createHash('sha256').update(JSON.stringify(snapshot)).digest('hex');
  await publishSnapshot(snapshot, { wait: async () => {}, command: (args, input) => {
    if (args.includes('put')) { script = JSON.parse(input).properties.source.script; return ''; }
    return JSON.stringify({ properties: { instanceView: { executionState: 'Succeeded', exitCode: 0,
      output: script ? JSON.stringify({ textReconciliationComplete: true, publicationHash: hash }) : '' } } });
  } });
  assert.ok(script.indexOf('if (true) process.exit(0)') < script.indexOf('fs.writeFileSync'));
  assert.ok(script.indexOf("/admin/sync") < script.indexOf('textReconciliationComplete:true'));
  assert.match(script, /TEXT_RECEIPT\nexit 0\nfi\nnode --env-file/);
  let published;
  await refreshDocuments([{ token: 'new', title: 'current' }], async value => {
    published = value; return { textReconciliationComplete: true };
  });
  assert.deepEqual(published, snapshot);
  await assert.rejects(refreshDocuments([], () => assert.fail()), /Invalid/);
  await assert.rejects(refreshDocuments([{ token: 'new', title: 'current' }], async () => ({})), /Missing/);
});

test('cold work is bounded but warm records are not needlessly split into many ARM jobs', () => {
  const records = Array.from({ length: 300 }, (_, i) => ({ id: 'image:' + i, media: { kind: 'image' } }));
  const snapshot = { version: 1, records };
  const cold = publicationBatches(snapshot);
  assert.equal(cold.length, 3); assert.ok(cold.every(batch => batch.length <= 128));
  assert.equal(publicationBatches(snapshot, new Set(records.map(row => row.id))).length, 1);
  const videos = Array.from({ length: 20 }, (_, i) => ({ id: 'video:' + i,
    media: { kind: 'video', fileToken: 'file' + Math.floor(i / 2), startSeconds: (i % 2) * 30, endSeconds: (i % 2 + 1) * 30 } }));
  const batches = publicationBatches({ version: 1, records: videos });
  for (const batch of batches) {
    assert.ok(batch.length <= 8);
    for (const row of batch) assert.equal(batch.filter(sibling => sibling.media.fileToken === row.media.fileToken).length, 2);
  }
  assert.deepEqual(batches.flat(), videos);
});

test('ARM success must carry this exact batch hash, not the preceding command receipt', async () => {
  const snapshot = { version: 1, records: [] };
  const hash = crypto.createHash('sha256').update(JSON.stringify(snapshot)).digest('hex');
  let puts = 0, polls = 0;
  const command = args => {
    if (args.includes('put')) { puts++; return ''; }
    const view = !puts ? { executionState: 'Succeeded', exitCode: 0 }
      : ++polls === 1 ? { executionState: 'Succeeded', exitCode: 0, output: JSON.stringify({ publicationHash: 'f'.repeat(64) }) }
        : { executionState: 'Succeeded', exitCode: 0, output: JSON.stringify({ publicationHash: hash, records: 0 }) };
    return JSON.stringify({ properties: { instanceView: view } });
  };
  const result = await publishSnapshot(snapshot, { command, wait: async () => {}, attempts: 3 });
  assert.equal(result.publicationHash, hash); assert.equal(polls, 2); assert.equal(puts, 1);
});

test('active remote publication is never overwritten by another PUT', async () => {
  await assert.rejects(publishSnapshot({ version: 1, records: [] }, {
    command: args => { assert.ok(!args.includes('put')); return JSON.stringify({ properties: { instanceView: { executionState: 'Running' } } }); },
    wait: async () => {},
  }), /already running/);
});

test('remote failures expose exit/cause without credential-bearing URLs', async () => {
  let submitted = false;
  await assert.rejects(publishSnapshot({ version: 1, records: [] }, {
    command: args => {
      if (args.includes('put')) { submitted = true; return ''; }
      return JSON.stringify({ properties: { instanceView: submitted
        ? { executionState: 'Failed', startTime: new Date().toISOString(), exitCode: -1,
          error: 'Timeout: signal: killed api-key=fixture-secret https://private.test/?token=hidden' }
        : { executionState: 'Succeeded', exitCode: 0 } } });
    }, wait: async () => {},
  }), error => /Timeout: signal: killed/.test(error.message) && /exit=-1/.test(error.message)
    && !/fixture-secret|private.test|hidden/.test(error.message));
});

test('transient status GET failures poll the accepted job instead of resubmitting it', async () => {
  const snapshot = { version: 1, records: [] };
  const hash = crypto.createHash('sha256').update(JSON.stringify(snapshot)).digest('hex');
  let puts = 0, polls = 0;
  await publishSnapshot(snapshot, { wait: async () => {}, attempts: 4, command: args => {
    if (args.includes('put')) { puts++; return ''; }
    if (puts && ++polls < 3) throw new Error('ARM read timeout');
    return JSON.stringify({ properties: { instanceView: { executionState: 'Succeeded', exitCode: 0,
      output: puts ? JSON.stringify({ publicationHash: hash }) : '' } } });
  } });
  assert.equal(puts, 1); assert.equal(polls, 3);
});

test('full multi-batch publication finalizes only after every batch succeeds', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'media-publication-'));
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  const file = path.join(root, 'snapshot.json');
  const records = Array.from({ length: 4 }, (_, id) => ({ id: 'image:' + id, text: crypto.randomBytes(300_000).toString('base64') }));
  await fs.writeFile(file, JSON.stringify({ version: 1, replacement: true, records, failures: [] }));
  const calls = [];
  await publishMedia(file, async batch => calls.push(batch));
  assert.ok(calls.length > 2);
  assert.ok(calls.slice(0, -1).every(batch => batch.partial && !batch.replacement));
  assert.equal(calls[0].refreshText, true);
  assert.ok(calls.slice(1).every(batch => !batch.refreshText));
  const final = calls.at(-1);
  assert.equal(final.replacement, true); assert.equal(final.partial, false);
  assert.deepEqual(final.records, []);
  assert.deepEqual(final.replacementRecords.map(row => row.id), records.map(row => row.id));
  let attempts = 0;
  await assert.rejects(publishMedia(file, async () => { if (++attempts === 2) throw new Error('transport failed'); }), /transport failed/);
  assert.equal(attempts, 2);
  await fs.writeFile(file, JSON.stringify({ version: 1, replacement: true, partial: true, records }));
  await assert.rejects(publishMedia(file, () => assert.fail('must not publish incomplete data')), /incomplete/);
});
