import { visualEvidence } from './visual-tags.mjs';
import { contentHash } from './lib.mjs';
import fs from 'node:fs/promises';
import { currentImageIndex } from './image-index.mjs';
import { currentVideoIndex } from './video-index.mjs';

const POLICY_TAGS = new Set(['专属', '非专属', '不可用', '无可见文字', '待文字复核', '现场文字', '无后期字幕', '待入库复核']);
const TEXT = /文字|字幕|汉字|字样|水印|标语|题字|印字|二维码|文字标识|英文单词|大字|小字|中文|截图|界面|海报/;
const UNCERTAIN = /不确定|无法|不能|疑似|可能|不清|模糊/;
const NO_TEXT = /^(?:无|没有|未见|未发现|未检测到|不含|未出现)(?:任何|明显|清晰|可见|可读|后期|原生|画面中|的|[、，\s]|文字|字幕|水印|标识)*[。.]?$/;
export const usageSourceHash = payload => contentHash(payload.visual_tagging?.baseContent ?? payload.media?.videoIndex?.baseContent ?? payload.content ?? '');
export const usageScopeEvidence = content => visualEvidence(String(content).replace(/^\d+(?:\.\d+)?秒\s+画面[:：]/gm, '画面:'));
export function preserveUsageTextReview(payload, next, previous) {
  const current = previous?.version === 1 && previous.sourceHash === usageSourceHash(payload);
  // 专属性重审不覆盖相同素材已有的文字结论及独立抽帧证据。
  return current ? { ...previous, ...next, hasVisibleText: previous.hasVisibleText } : next;
}
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
  // 无法读出字的内容，不等于没有字；保留观察中的明确文字存在证据。
  const unreadableText = String(content).split('\n').filter(line => /^不确定[:：]/.test(line)).flatMap(line => line.split(/[。；]/))
    .filter(line => !/未见|没有|未观察到|未发现|未出现|不含|看不到|是否|疑似|可能/.test(line)
      && /(?:文字|字幕|印字|汉字|字样)[^。；]{0,32}(?:无法|不能|不清|模糊|过小|太小|辨认|识读|识别)/.test(line));
  const textEvidence = [...unreadableText, ...evidence.filter(line => TEXT.test(line) && !NO_TEXT.test(line)), ...textLines.filter(line => line && !NO_TEXT.test(line)
    && (!UNCERTAIN.test(line) || /文字|印刷|字幕|汉字|字样|“|”/.test(line)))];
  const reviewed = review?.version === 1 && review.sourceHash === usageSourceHash(payload);
  const absent = /(?:没有看到|未看到|未观察到|未见|没有|不含)(?:任何|明显|清晰|可见|可读)*文字/.test(content);
  const hasVisibleText = textEvidence.length || (reviewed && review.hasVisibleText === true) ? true
    : (textLines.some(line => NO_TEXT.test(line)) || absent || (reviewed && review.hasVisibleText === false)) ? false : null;
  const generic = reviewed && review.exclusive === false;
  const indexed = currentImageIndex(payload) || currentVideoIndex(payload);
  const noFrames = indexed?.evidenceSource === 'video-stream-boundary';
  const positive = [...evidence, ...textLines].join('；').replace(/(?:无|没有|未见|不含)(?:任何|明显|后期|可见)*(?:字幕|水印|压字)/g, '');
  const edited = /字幕|水印|后期压字|文字覆盖|后期贴纸/.test(positive);
  const postproduction = indexed ? (indexed.text === 'uncertain' ? null : indexed.text === 'edited') : edited ? true : hasVisibleText === false ? false : null;
  const previous = payload.media?.usage;
  const manualBlock = (payload.media?.tags || []).includes('人工禁用') || payload.media?.status === 'rejected'
    || (previous?.usable === false && /人工|版权|未授权|损坏/.test(previous.reason || ''));
  return { version: 2, exclusive: !generic, hasVisibleText: indexed ? (indexed.text === 'uncertain' ? null : indexed.text !== 'clean') : hasVisibleText,
    hasPostproductionText: postproduction, usable: manualBlock || noFrames || postproduction === true ? false : postproduction === false ? true : null,
    reason: manualBlock ? previous?.reason || '人工禁用' : noFrames ? '片段无视频画面，不可用于混剪' : reviewed ? review.reason : '专属性尚未确认，限制在原基地使用',
    textReason: indexed ? indexed.text : postproduction === true ? '已有观察记录后期字幕或水印' : postproduction === false ? '已有观察明确记录未见文字' : '后期文字状态未知，待入库复核；不能仅因现场文字禁用',
    evidenceSource: indexed ? (payload.media.kind === 'video' ? indexed.evidenceSource : 'ingested-image-v1') : 'existing-frame-observations',
    coverage: indexed ? (indexed.coverage || 'original image') : payload.media?.sampling || 'source observation only' };
}

export function applyMediaUsagePolicy(payload, policy = mediaUsagePolicy(payload)) {
  if (!['image', 'video'].includes(payload.media?.kind)) return payload;
  const tags = [...new Set([...(payload.media.tags || payload.asset?.tags || []).filter(tag => !POLICY_TAGS.has(tag)),
    policy.exclusive ? '专属' : '非专属', policy.usable === false ? '不可用' : policy.usable === null ? '待入库复核' : policy.hasVisibleText === false ? '无可见文字' : '现场文字'])];
  return { ...payload, media: { ...payload.media, tags, usage: policy } };
}

// 同一文件的后期字幕污染不能被其他片段掩盖；现场文字不传播禁用。
export function applyFileUsagePolicies(points, reviews = new Map()) {
  const groups = new Map();
  for (const point of points) {
    const key = point.payload.media?.sha256 || point.payload.media?.fileToken || point.payload.asset?.checksum || point.id;
    const review = reviews.get(point.id) || point.payload.media?.usageReview;
    const reviewedPoint = review ? { ...point, payload: { ...point.payload, media: { ...point.payload.media, usageReview: review } } } : point;
    const group = groups.get(key) || []; group.push({ point: reviewedPoint, policy: mediaUsagePolicy(point.payload, review) }); groups.set(key, group);
  }
  return [...groups.values()].flatMap(group => {
    const text = group.find(item => item.policy.hasPostproductionText === true)?.policy;
    const exclusive = group.some(item => item.policy.exclusive);
    return group.map(({ point, policy }) => ({ ...point, payload: applyMediaUsagePolicy(point.payload,
      { ...policy, exclusive, ...(exclusive && !policy.exclusive ? { reason: '同一素材其他片段含专属环境或尚未确认专属性' } : {}),
        ...(text ? { hasVisibleText: true, hasPostproductionText: true, usable: false, textReason: text.textReason } : {}) }) }));
  });
}
