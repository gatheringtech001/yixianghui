export const businessNames = ['全国旅居', '聚会活动', '芳华学院', '云南好物']
export const metricKeys = ['orderCount', 'paidCount', 'pendingCount', 'cancelledCount', 'refundedCount', 'receivedAmount', 'refundAmount', 'netAmount']
export function formatMetric(value, money = false) {
  if (value === null || value === undefined || value === '' || !Number.isFinite(Number(value))) return '—'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: money ? 2 : 0, maximumFractionDigits: money ? 2 : 0 })
}
export function businessRows(rows) {
  if (!Array.isArray(rows)) return []
  const names = [...businessNames, ...rows.map(row => row.business).filter(name => !businessNames.includes(name))]
  return [...new Set(names)].map(business => rows.find(row => row.business === business) ||
    { business, ...Object.fromEntries(metricKeys.map(key => [key, 0])) })
}
export function trafficMetric(traffic, key) {
  return traffic && traffic.status === 'available' ? traffic[key] : null
}
