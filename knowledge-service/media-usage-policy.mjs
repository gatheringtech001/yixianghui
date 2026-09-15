import { visualEvidence } from './visual-tags.mjs';
import { contentHash } from './lib.mjs';
import fs from 'node:fs/promises';

const POLICY_TAGS = new Set(['专属', '非专属', '不可用', '无可见文字', '待文字复核']);
const TEXT = /文字|字幕|汉字|字样|水印|标语|题字|印字|二维码|文字标识|英文单词|大字|小字|中文|截图|界面|海报/;
const UNCERTAIN = /不确定|无法|不能|疑似|可能|不清|模糊/;
const NO_TEXT = /^(?:无|没有|未见|未发现|未检测到|不含|未出现)(?:任何|明显|清晰|可见|可读|后期|原生|画面中|的|[、，\s]|文字|字幕|水印|标识)*[。.]?$/;
export const usageSourceHash = payload => contentHash(payload.visual_tagging?.baseContent ?? payload.content ?? '');
export async function readUsageReview(point, directory = '/var/lib/yixianghui-knowledge/media-usage-v1/reviews') {
  if (!/^[a-f0-9-]{36}$/.test(point.id)) return undefined;
  try { return JSON.parse(await fs.readFile(`${directory}/${point.id}.json`, 'utf8')); }
  catch (error) { if (error.code === 'ENOENT') return undefined; throw error; }
}

export function mediaUsagePolicy(payload = {}, review = payload.media?.usageReview) {
  const content = payload.visual_tagging?.baseContent ?? payload.content ?? '';
  const evidence = visualEvidence(content);
  const textLines = String(content).split('\n').filter(line => /^(?:图片文字|文字|画面文字|可见文字|文字识别|OCR)[:：]/i.test(line))
    .map(line => line.replace(/^[^:：]+[:：]\s*/, '').trim());
  const textEvidence = [...evidence.filter(line => TEXT.test(line) && !NO_TEXT.test(line)), ...textLines.filter(line => line && !NO_TEXT.test(line)
    && (!UNCERTAIN.test(line) || /文字|印刷|字幕|汉字|字样|“|”/.test(line)))];
  const reviewed = review?.version === 1 && review.sourceHash === usageSourceHash(payload);
  const absent = /(?:没有看到|未看到|未观察到|未见|没有|不含)(?:任何|明显|清晰|可见|可读)*文字/.test(content);
  const hasVisibleText = textEvidence.length || (reviewed && review.hasVisibleText === true) ? true
    : (textLines.some(line => NO_TEXT.test(line)) || absent || (reviewed && review.hasVisibleText === false)) ? false : null;
  const generic = reviewed && review.exclusive === false;
  return { version: 1, exclusive: !generic, hasVisibleText, usable: hasVisibleText === true ? false : hasVisibleText === false ? true : null,
    reason: reviewed ? review.reason : '专属性尚未确认，限制在原基地使用',
    textReason: hasVisibleText === true ? textEvidence.slice(0, 3).join('；') : hasVisibleText === false ? '已有观察明确记录未见文字' : '缺少明确无字结论，出片前必须复核',
    evidenceSource: 'existing-frame-observations', coverage: payload.media?.sampling || 'source observation only' };
}

export function applyMediaUsagePolicy(payload, policy = mediaUsagePolicy(payload)) {
  if (!['image', 'video'].includes(payload.media?.kind)) return payload;
  const tags = [...new Set([...(payload.media.tags || payload.asset?.tags || []).filter(tag => !POLICY_TAGS.has(tag)),
    policy.exclusive ? '专属' : '非专属', policy.usable === false ? '不可用' : policy.hasVisibleText === false ? '无可见文字' : '待文字复核'])];
  return { ...payload, media: { ...payload.media, tags, usage: policy } };
}

// 同一文件任何片段出现文字都禁用整份素材；混合场景不得当作纯通用空镜。
export function applyFileUsagePolicies(points, reviews = new Map()) {
  const groups = new Map();
  for (const point of points) {
    const key = point.payload.media?.sha256 || point.payload.media?.fileToken || point.payload.asset?.checksum || point.id;
    const review = reviews.get(point.id) || point.payload.media?.usageReview;
    const reviewedPoint = review ? { ...point, payload: { ...point.payload, media: { ...point.payload.media, usageReview: review } } } : point;
    const group = groups.get(key) || []; group.push({ point: reviewedPoint, policy: mediaUsagePolicy(point.payload, review) }); groups.set(key, group);
  }
  return [...groups.values()].flatMap(group => {
    const text = group.find(item => item.policy.hasVisibleText === true)?.policy;
    const exclusive = group.some(item => item.policy.exclusive);
    return group.map(({ point, policy }) => ({ ...point, payload: applyMediaUsagePolicy(point.payload,
      { ...policy, exclusive, ...(exclusive && !policy.exclusive ? { reason: '同一素材其他片段含专属环境或尚未确认专属性' } : {}),
        ...(text ? { hasVisibleText: true, usable: false, textReason: text.textReason } : {}) }) }));
  });
}
