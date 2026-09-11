import { readFile } from 'node:fs/promises';

// 已确认被替代的来源使用精确 ID，禁止凭同名或导入时间删除独立资料。
export async function loadRetiredSources(file = process.env.KNOWLEDGE_RETIRED_SOURCES_FILE) {
  if (!file) return new Set();
  const data = JSON.parse(await readFile(file, 'utf8'));
  if (data.version !== 1 || !Array.isArray(data.sourceIds)
      || data.sourceIds.some(id => typeof id !== 'string' || !id.trim())
      || new Set(data.sourceIds).size !== data.sourceIds.length) {
    throw new Error('Invalid retired source registry');
  }
  return new Set(data.sourceIds);
}
