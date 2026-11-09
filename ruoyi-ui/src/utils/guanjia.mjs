const base = 'https://gatheringtech.com/guanjia/';
const scopes = ['all', 'mini', 'wechat', 'creative', 'knowledge', 'finance'];
const sources = ['goods', 'activity', 'goods-order', 'activity-order', 'home'];
export function guanjiaUrl(intent) {
  const p = new URLSearchParams();
  if (!sources.includes(intent.from)) throw new Error('后台来源无效');
  p.set('from', intent.from);
  p.set('task', intent.task);
  if (intent.task === 'edit') {
    if (!['goods', 'activity'].includes(intent.type) || !Number.isSafeInteger(Number(intent.id)) || Number(intent.id) < 1 || String(intent.id).length > 15) throw new Error('对象编号无效');
    p.set('type', intent.type); p.set('id', String(intent.id));
  } else if (intent.task === 'create' && ['hotel', 'education', 'activity', 'online'].includes(intent.kind)) p.set('kind', intent.kind);
  else if (intent.task === 'assistant' && scopes.includes(intent.scope)) p.set('scope', intent.scope);
  else if (intent.task === 'page' && ['orders', 'approvals', 'admin'].includes(intent.page)) p.set('page', intent.page);
  else throw new Error('AI入口任务无效');
  return base + '#native?' + p.toString();
}
export function exportQuery(query = {}) {
  return Object.fromEntries(Object.entries(query).filter(([key, value]) => !['pageNum', 'pageSize', 'reasonable'].includes(key) && value !== undefined));
}
export function rosterTarget(context) {
  const id = Number(context.id);
  if (!['goods', 'activity'].includes(context.type) || !Number.isSafeInteger(id) || id < 1) throw new Error('名单对象无效');
  return { url: 'system/app_' + context.type + '_order/roster', query: { [context.type === 'goods' ? 'goodsId' : 'activityId']: id }, permission: 'system:app_' + context.type + '_order:export' };
}
