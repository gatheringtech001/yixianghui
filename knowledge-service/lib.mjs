import crypto from "node:crypto";

const HAN = /[\p{Script=Han}]/u;
const WORDS = /[\p{Letter}\p{Number}]+/gu;

function cutsSurrogatePair(text, index) {
  if (index <= 0 || index >= text.length) return false;
  const previous = text.charCodeAt(index - 1);
  const current = text.charCodeAt(index);
  return previous >= 0xd800 && previous <= 0xdbff && current >= 0xdc00 && current <= 0xdfff;
}

function splitLong(text, maxChars, overlap) {
  const parts = [];
  let start = 0;
  while (start < text.length) {
    let end = Math.min(text.length, start + maxChars);
    if (cutsSurrogatePair(text, end)) end += 1;
    parts.push(text.slice(start, end));
    if (end === text.length) break;
    start = Math.max(start + 1, end - overlap);
    if (cutsSurrogatePair(text, start)) start -= 1;
  }
  return parts;
}

export function sanitizeText(value) {
  const text = String(value ?? "");
  let output = "";
  for (let index = 0; index < text.length; index += 1) {
    const code = text.charCodeAt(index);
    if (code >= 0xd800 && code <= 0xdbff) {
      const next = text.charCodeAt(index + 1);
      if (next >= 0xdc00 && next <= 0xdfff) {
        output += text[index] + text[index + 1];
        index += 1;
      } else {
        output += "\ufffd";
      }
    } else if (code >= 0xdc00 && code <= 0xdfff) {
      output += "\ufffd";
    } else {
      output += text[index];
    }
  }
  return output;
}

export function chunkDocument(title, raw, options = {}) {
  const maxChars = options.maxChars ?? 1200;
  const overlap = options.overlap ?? 160;
  if (maxChars < 200 || overlap < 0 || overlap >= maxChars) {
    throw new Error("invalid chunk options");
  }
  const safeTitle = sanitizeText(title);
  const content = sanitizeText(raw)
    .replace(/\r\n?/g, "\n")
    .replace(/[\t\u00a0]+/g, " ")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
  if (!content) return [];

  const paragraphs = content.split(/\n\s*\n/).flatMap((part) =>
    part.length > maxChars ? splitLong(part, maxChars, overlap) : [part]
  );
  const chunks = [];
  let current = "";
  for (const paragraph of paragraphs) {
    const candidate = current ? `${current}\n\n${paragraph}` : paragraph;
    if (candidate.length <= maxChars) {
      current = candidate;
      continue;
    }
    if (current) chunks.push(current);
    let tailStart = Math.max(0, current.length - overlap);
    if (cutsSurrogatePair(current, tailStart)) tailStart -= 1;
    const tail = current.slice(tailStart);
    current = tail ? `${tail}\n\n${paragraph}` : paragraph;
    if (current.length > maxChars) {
      const pieces = splitLong(current, maxChars, overlap);
      chunks.push(...pieces.slice(0, -1));
      current = pieces.at(-1) ?? "";
    }
  }
  if (current) chunks.push(current);
  return chunks.map((contentPart, index) => {
    const safeContent = sanitizeText(contentPart);
    return {
      index,
      text: safeTitle ? `${safeTitle}\n${safeContent}` : safeContent,
      content: safeContent,
    };
  });
}

export function lexicalTokens(value) {
  const text = String(value ?? "").normalize("NFKC").toLocaleLowerCase("zh-CN");
  const tokens = [];
  for (const match of text.matchAll(WORDS)) {
    const word = match[0];
    if (![...word].some((char) => HAN.test(char))) {
      tokens.push(word);
      continue;
    }
    const chars = [...word].filter((char) => HAN.test(char));
    tokens.push(...chars);
    for (let index = 0; index + 1 < chars.length; index += 1) {
      tokens.push(chars[index] + chars[index + 1]);
    }
  }
  return tokens;
}

function tokenIndex(token) {
  return crypto.createHash("sha256").update(token).digest().readUInt32BE(0);
}

export function sparseVector(value) {
  const counts = new Map();
  for (const token of lexicalTokens(value)) {
    const index = tokenIndex(token);
    counts.set(index, (counts.get(index) ?? 0) + 1);
  }
  const entries = [...counts.entries()].sort(([left], [right]) => left - right);
  return {
    indices: entries.map(([index]) => index),
    values: entries.map(([, count]) => 1 + Math.log(count)),
  };
}

export function pointId(sourceId, chunkIndex) {
  const bytes = crypto.createHash("sha256")
    .update(`${sourceId}:${chunkIndex}`)
    .digest()
    .subarray(0, 16);
  bytes[6] = (bytes[6] & 0x0f) | 0x50;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = bytes.toString("hex");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function contentHash(value) {
  return crypto.createHash("sha256").update(String(value)).digest("hex");
}

export function validateQuestion(value) {
  const question = String(value ?? "").trim();
  if (question.length < 2 || question.length > 500) {
    throw new Error("question must contain 2 to 500 characters");
  }
  return question;
}

export function applyRerankOrder(candidates, order, limit) {
  const byId = new Map(candidates.map((item) => [String(item.id), item]));
  const selected = [];
  for (const id of order) {
    const item = byId.get(String(id));
    if (!item || selected.includes(item)) continue;
    selected.push(item);
    if (selected.length === limit) break;
  }
  return selected;
}
