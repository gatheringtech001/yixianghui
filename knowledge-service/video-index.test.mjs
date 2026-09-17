import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import { join } from 'node:path';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { prepareVideoIndex, currentVideoIndex, videoFrameTimes, extractVideoFrames } from './video-index.mjs';
import { applyMediaUsagePolicy } from './media-usage-policy.mjs';
import { backfillVideos } from './backfill-video-index.mjs';
import { syncMedia } from './media-sync.mjs';

const point = (content = '画面: 浴室墙上有提示牌。') => ({ id: 'test1', payload: { title: '任意基地', content,
  source_url: 'https://vcnnjnb870d6.feishu.cn/file/abcdefghijklmnop',
  media: { kind: 'video', fileToken: 'abcdefghijklmnop', startSeconds: 4, endSeconds: 8,
    tags: ['花洒', '不可用'], usage: { usable: false, version: 1, hasVisibleText: true } } } });
async function fixture(t) {
  const directory = await fs.mkdtemp(join(os.tmpdir(), 'video-index-'));
  t.after(() => fs.rm(directory, { recursive: true, force: true }));
  let calls = 0;
  const options = { directory, extract: async payload => ({ hash: 'a'.repeat(64), frames: videoFrameTimes(payload.media).map(time => ({ time, url: 'data:image/jpeg;base64,eA==' })) }),
    labeler: { config: { model: 'test' }, complete: async body => {
      calls++;
      const frames = body.messages[1].content.filter(c => c.type === 'text').map(c => ({ ...JSON.parse(c.text), description: '浴室花洒和小心地滑提示牌', tags: ['浴室', '花洒', '提示牌'], text: 'natural', face: false }));
      return { output: { frames } };
    } } };
  return { options, calls: () => calls };
}

test('time evidence is bounded, includes the end, and never shifts to zero', () => {
  assert.deepEqual(videoFrameTimes({ startSeconds: 30, endSeconds: 35 }), [30, 32, 34, 34.9]);
  assert.deepEqual(videoFrameTimes({ startSeconds: 0, endSeconds: .1 }), [0, .05]);
  assert.throws(() => videoFrameTimes({ startSeconds: 0, endSeconds: 60 }));
});

test('natural text is usable; index survives content rewriting and skips repeat vision', async t => {
  const { options, calls } = await fixture(t);
  const result = await prepareVideoIndex(point(), options);
  assert.ok(currentVideoIndex(result.payload));
  const policy = applyMediaUsagePolicy(result.payload);
  assert.equal(policy.media.usage.usable, true);
  assert.equal(policy.media.usage.hasPostproductionText, false);
  assert.ok(policy.media.tags.includes('花洒')); assert.ok(!policy.media.tags.includes('不可用'));
  await prepareVideoIndex({ ...result, payload: policy }, options);
  await prepareVideoIndex(point(), options);
  assert.equal(calls(), 1);
  assert.equal(currentVideoIndex({ ...result.payload, source_updated_at: 'changed' }), null);
});

test('existing positive subtitle evidence preserves bans without downloading or model calls', async t => {
  const { options, calls } = await fixture(t);
  options.extract = async () => assert.fail('must not download');
  const result = await prepareVideoIndex(point('画面: 山峰，左下角有白色字幕。'), options);
  assert.equal(result.payload.media.videoIndex.evidenceSource, 'existing-edited-observations');
  assert.equal(applyMediaUsagePolicy(result.payload).media.usage.usable, false);
  assert.equal(calls(), 0);
});

test('uncertain text remains pending, edited frames are banned, manual bans survive', async t => {
  for (const text of ['uncertain', 'edited', 'natural']) {
    const { options } = await fixture(t);
    const complete = options.labeler.complete;
    options.labeler.complete = async body => {
      const result = await complete(body); result.output.frames[0].text = text; return result;
    };
    const result = await prepareVideoIndex(point('画面: 窗户，没有字幕。'), options);
    assert.equal(applyMediaUsagePolicy(result.payload).media.usage.usable, text === 'uncertain' ? null : text !== 'edited');
    result.payload.media.tags.push('人工禁用');
    assert.equal(applyMediaUsagePolicy(result.payload).media.usage.usable, false);
  }
});

test('incomplete or altered frame times cannot be cached as a valid index', async t => {
  const { options } = await fixture(t);
  options.labeler.complete = async () => ({ output: { frames: [{ time: 0 }] } });
  await assert.rejects(prepareVideoIndex(point(), options), /Incomplete/);
  assert.deepEqual(await fs.readdir(options.directory), []);
});

test('unsupported model tags are recorded and removed without losing valid frame evidence', async t => {
  const { options } = await fixture(t); const complete = options.labeler.complete;
  options.labeler.complete = async body => { const r = await complete(body); r.output.frames[0].tags.push('双床', '麦田'); return r; };
  const result = await prepareVideoIndex(point(), options);
  assert.ok(currentVideoIndex(result.payload));
  assert.deepEqual(result.payload.media.videoIndex.rejectedTags.map(r => r.tag), ['双床', '麦田']);
  assert.ok(!result.payload.media.tags.includes('双床'));
});

test('real ffmpeg extraction verifies source bytes and uses private cached files', async t => {
  const { options } = await fixture(t);
  const file = join(options.directory, 'fixture.mp4');
  execFileSync('ffmpeg', ['-v', 'error', '-f', 'lavfi', '-i', 'color=c=blue:s=64x64:d=1', '-y', file]);
  const bytes = await fs.readFile(file); const p = point().payload;
  p.media.startSeconds = 0; p.media.endSeconds = 1; p.media.sha256 = createHash('sha256').update(bytes).digest('hex');
  const extracted = await extractVideoFrames(p, { directory: join(options.directory, 'sources'), open: async () => new Response(bytes) });
  assert.equal(extracted.hash, p.media.sha256); assert.equal(extracted.frames.length, 2);
  assert.ok(extracted.frames.every(f => f.url.startsWith('data:image/jpeg;base64,')));
  p.media.sha256 = 'b'.repeat(64);
  await assert.rejects(extractVideoFrames(p, { directory: join(options.directory, 'sources'), open: async () => new Response(bytes) }), /checksum/);
});

test('audio extending past the last video frame does not request nonexistent frames', async t => {
  const { options } = await fixture(t);
  const file = join(options.directory, 'audio-tail.mp4');
  execFileSync('ffmpeg', ['-v', 'error', '-f', 'lavfi', '-i', 'color=c=blue:s=64x64:d=1',
    '-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=mono', '-t', '2.2', '-c:v', 'libx264', '-c:a', 'aac', '-y', file]);
  const bytes = await fs.readFile(file); const p = point();
  p.payload.media.startSeconds = 0; p.payload.media.endSeconds = 2.2;
  const extraction = { directory: join(options.directory, 'sources'), open: async () => new Response(bytes) };
  const result = await extractVideoFrames(p.payload, extraction);
  assert.ok(result.frames.every(frame => frame.time < 1));
  assert.equal(result.sampleEndSeconds, 1);
  const indexed = await prepareVideoIndex(p, { ...options, extract: async () => result });
  assert.ok(currentVideoIndex(indexed.payload));
  assert.equal(indexed.payload.media.videoIndex.sampleEndSeconds, 1);
  p.payload.media.startSeconds = 1.5;
  await assert.rejects(extractVideoFrames(p.payload, extraction), /no video frames/);
});

test('video backfill writes and verifies vectors then resumes without duplicate vision', async t => {
  const { options, calls } = await fixture(t); let stored = point();
  const store = { config: { collection: 'test', dimensions: 2 }, api: async () => ({ result: [structuredClone(stored)] }),
    upsert: async ([row]) => { stored = structuredClone(row); stored.vector.dense = [.6, .8]; } };
  const run = { store, points: [point()], models: { embed: async () => [[3, 4]] }, directory: join(options.directory, 'run'),
    indexer: p => prepareVideoIndex(p, options) };
  assert.equal((await backfillVideos(run)).pending, 1); assert.equal(calls(), 0);
  assert.deepEqual((await backfillVideos({ ...run, apply: true })).written, ['test1']);
  assert.equal(stored.payload.media.usage.usable, true);
  assert.equal((await backfillVideos({ ...run, points: [stored], apply: true })).pending, 0);
  assert.equal(calls(), 1);
});

test('new video ingestion cannot publish without completed indexing', async t => {
  const { options } = await fixture(t);
  for (const videoIndexer of [null, async p => p, async () => { throw new Error('upstream failed'); }]) {
    await assert.rejects(syncMedia({ videoIndexer, visualTagsDirectory: options.directory, manifestFile: join(options.directory, 'manifest.json'),
      snapshot: { version: 1, createdAt: '2026-09-17T00:00:00Z', records: [{ id: 'video1', title: '基地', text: '画面: 房间',
        sourceUrl: point().payload.source_url, media: point().payload.media }] },
      store: { ensureCollection: () => assert.fail('must not publish') }, models: { embed: () => assert.fail('must not embed') },
    }));
  }
});

test('all sibling evidence is prepared before publication; a later subtitle blocks the file', async t => {
  const { options } = await fixture(t);
  const first = point();
  const second = point('画面: 湖泊，底部白色字幕。'); second.id = 'test2'; second.payload.media.startSeconds = 8; second.payload.media.endSeconds = 10;
  const saved = new Map([first, second].map(p => [p.id, p]));
  let preparations = 0, releases = 0;
  const report = await backfillVideos({ apply: true, directory: join(options.directory, 'run'), points: [first, second],
    indexer: p => { preparations++; return prepareVideoIndex(p, options); }, releaseFile: async () => { releases++; }, models: { embed: async () => [[1]] },
    store: { config: { collection: 'test', dimensions: 1 }, api: async (_, r) => ({ result: JSON.parse(r.body).ids.map(id => saved.get(id)) }),
      upsert: async rows => rows.forEach(p => saved.set(p.id, p)) } });
  assert.deepEqual(report.failed, []); assert.equal(report.written.length, 2);
  assert.ok([...saved.values()].every(p => p.payload.media.usage.usable === false));
  assert.equal(preparations, 2);
  assert.equal(releases, 1);
});

test('new video synchronization publishes once and reuses persisted observations', async t => {
  const { options, calls } = await fixture(t); let writes = 0;
  const config = { videoIndexer: p => prepareVideoIndex(p, options), visualTagsDirectory: options.directory,
    manifestFile: join(options.directory, 'manifest.json'),
    snapshot: { version: 1, createdAt: '2026-09-17T00:00:00Z', records: [{ id: 'video1', title: '基地', text: '画面: 房间',
      sourceUrl: point().payload.source_url, media: point().payload.media }] },
    store: { config: { collection: 'test' }, ensureCollection: async () => {}, deleteIds: async () => {}, upsert: async rows => {
      writes++; assert.ok(currentVideoIndex(rows[0].payload)); assert.equal(rows[0].payload.media.usage.usable, true);
    } }, models: { embed: async () => [[1]] },
  };
  await syncMedia(config); await syncMedia(config);
  assert.equal(writes, 1); assert.equal(calls(), 1);
});
