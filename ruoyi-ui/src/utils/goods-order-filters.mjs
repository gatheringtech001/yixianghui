// Match shop-mnp/pages/classify/classify.vue, not the legacy goods_type dictionary.
const services = [
  { scope: 'hotel', label: '全国旅居', names: ['全国旅居'] },
  { scope: 'activity', label: '聚会活动', names: ['聚会活动'], route: '/order/app_activity_order' },
  { scope: 'education', label: '芳华学院', names: ['老年教育'] },
  { scope: 'retail', label: '云南好物', names: ['云南好物', '云野好物'] }
]

export function orderServices(categories = []) {
  return services.map(service => {
    const root = categories.find(node => Number(node.parentId) === 0 && String(node.status) === '1' && service.names.includes(String(node.categoryName || '').trim()))
    return { ...service, categoryId: root?.categoryId ?? null }
  })
}

export function goodsOrderQuery(query, scope = 'all', entries = []) {
  const entry = entries.find(item => item.scope === scope && !item.route)
  // Missing mappings fail closed; clearing a subcategory means this business, never all orders.
  const categoryId = scope === 'all' ? query.categoryId : (entry?.categoryId == null ? -1 : (query.categoryId ?? entry.categoryId))
  return { ...query, businessType: null, goodsType: null, categoryId }
}

export function orderBusinessLabel(row, categories = [], entries = orderServices(categories)) {
  let node = categories.find(item => String(item.categoryId) === String(row.categoryId))
  const seen = new Set()
  while (node && Number(node.parentId) !== 0 && !seen.has(String(node.categoryId))) {
    seen.add(String(node.categoryId))
    node = categories.find(item => String(item.categoryId) === String(node.parentId))
  }
  const entry = node && entries.find(item => !item.route && String(item.categoryId) === String(node.categoryId))
  return entry?.label || (row.categoryId == null ? '未分类' : '其他商品')
}

// Clear hidden fields too: changing tabs must not leave the previous business's filters active.
export function resetGoodsOrderQuery(query) {
  return Object.fromEntries(Object.keys(query).map(key => [key,
    key === 'pageSize' ? query.pageSize : key === 'pageNum' ? 1 : null]))
}
