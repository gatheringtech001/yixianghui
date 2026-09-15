import test from 'node:test';
import assert from 'node:assert/strict';
import { applyFileUsagePolicies, applyMediaUsagePolicy, mediaUsagePolicy, usageSourceHash, preserveUsageTextReview, usageScopeEvidence } from './media-usage-policy.mjs';
const payload = content => ({ title: '昆明某基地', content, media: { kind: 'video', fileToken: 'source-token', startSeconds: 30, endSeconds: 60 } });
test('scope review recognizes timestamped observations without treating source names as visual evidence', () => {
  assert.deepEqual(usageScopeEvidence('来源: 昆明景点\n0秒 画面: 普通花草。\n33.5秒 画面: 树叶特写。'), ['普通花草', '树叶特写']);
  assert.deepEqual(usageScopeEvidence('来源: 昆明景点\n语音转写: 普通花草'), []);
});
test('scope reclassification preserves existing text conclusions and independent review evidence', () => {
  const p = payload('画面: 普通小泡池与植物。');
  for (const hasVisibleText of [true, false, null]) {
    const previous = { version: 1, sourceHash: usageSourceHash(p), exclusive: true, hasVisibleText, reviewEvidence: 'dense-frames' };
    const next = preserveUsageTextReview(p, { exclusive: false, hasVisibleText: false, reason: '无地域标志的小泡池' }, previous);
    assert.equal(next.hasVisibleText, hasVisibleText);
    assert.equal(next.exclusive, false);
    assert.equal(next.reviewEvidence, 'dense-frames');
  }
  assert.equal(preserveUsageTextReview(p, { hasVisibleText: null }, { version: 1, sourceHash: 'old', hasVisibleText: false }).hasVisibleText, null);
});
test('generic plants and bed closeups can be shared, landmarks and rooms cannot', () => {
  for (const text of ['画面: 树叶与花草特写。\n画面文字: 无', '画面: 床铺特写，白色床单和枕头。\n画面文字: 无']) {
    const p = payload(text);
    assert.equal(mediaUsagePolicy(p).exclusive, true);
    assert.equal(mediaUsagePolicy(p, { version: 1, sourceHash: usageSourceHash(p), exclusive: false, reason: '画面仅有通用局部物体' }).exclusive, false);
  }
  for (const text of ['画面: 昆明滇池与西山全景，前景有花草。', '画面: 寺庙建筑前的树木。', '画面: 客房全景，床铺和窗外城市。', '语音转写: 花花草草']) {
    assert.equal(mediaUsagePolicy(payload(text)).exclusive, true);
  }
});
test('subtitles, scene lettering and uncertain lettering cannot be labelled clean', () => {
  for (const content of ['画面: 树木，底部白色字幕。', '画面: 床铺。\n画面文字: 欢迎入住', '画面: 墙上有汉字。']) {
    const result = applyMediaUsagePolicy(payload(content));
    assert.equal(result.media.usage.usable, false); assert.ok(result.media.tags.includes('不可用'));
  }
  assert.equal(mediaUsagePolicy(payload('画面: 花草。\n画面文字: 无')).hasVisibleText, false);
  assert.notEqual(mediaUsagePolicy(payload('画面: 树叶特写，无文字。\n画面文字: 无')).hasVisibleText, true);
  assert.equal(mediaUsagePolicy(payload('画面: 花草。\n不确定: 疑似字幕，无法辨认')).hasVisibleText, null);
});
test('explicit unreadable text in uncertainty notes remains unusable even after a clean review', () => {
  for (const note of ['桌面及其他物品上的文字无法清晰辨认', '床头柜上的小卡片和设备文字过小，无法清晰辨认']) {
    const p = payload('画面: 普通双床客房。\n不确定: ' + note);
    assert.equal(mediaUsagePolicy(p, { version: 1, sourceHash: usageSourceHash(p), hasVisibleText: false }).usable, false);
  }
  const p = payload('画面: 普通双床客房。\n不确定: 未观察到清晰可读文字；房间用途无法确认。');
  assert.notEqual(mediaUsagePolicy(p).hasVisibleText, true);
  assert.equal(mediaUsagePolicy(payload('画面: 客房。\n不确定: 是否有文字无法确认')).hasVisibleText, null);
});
test('policy labels preserve visual tags, identity and time range without changing retrieval content', () => {
  const p = payload('画面: 树叶特写。\n画面文字: 无'); p.media.tags = ['树叶'];
  p.media.usageReview = { version: 1, sourceHash: usageSourceHash(p), exclusive: false, reason: '树叶近景' };
  const result = applyMediaUsagePolicy(p);
  assert.equal(result.content, p.content); assert.equal(result.media.startSeconds, 30);
  assert.ok(result.media.tags.includes('非专属')); assert.ok(result.media.tags.includes('树叶'));
  assert.deepEqual(applyMediaUsagePolicy(result), result);
});
test('image lettering and text in any sibling clip block the complete file', () => {
  const p = payload('图片画面: 餐桌。\n图片文字: 标牌写着“温馨提示”，其余文字无法辨认');
  const other = payload('画面: 树木。\n画面文字: 无');
  const result = applyFileUsagePolicies([{ id: 'a', payload: p }, { id: 'b', payload: other }]);
  assert.ok(result.every(point => point.payload.media.usage.usable === false));
});
