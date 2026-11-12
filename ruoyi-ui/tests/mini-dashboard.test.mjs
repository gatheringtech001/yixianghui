import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { businessRows, formatMetric, trafficMetric } from '../src/utils/mini-dashboard.mjs'
test('missing or unavailable data is never displayed as a fabricated zero', () => {
  for (const value of [null, undefined, '', NaN, 'failed']) assert.equal(formatMetric(value), '—')
  assert.equal(trafficMetric({ status: 'unavailable', visitors: 123 }, 'visitors'), null)
  assert.equal(trafficMetric({ status: 'available', visitors: 0 }, 'visitors'), 0)
  assert.equal(formatMetric(0), '0')
  assert.equal(formatMetric('250.01', true), '250.01')
})
test('all mini-program businesses appear without dropping historical unmatched orders', () => {
  const rows = businessRows([{ business: '芳华学院', orderCount: 2, receivedAmount: '0.01' }, { business: '其他商品', orderCount: 1 }])
  assert.deepEqual(rows.map(row => row.business), ['全国旅居', '聚会活动', '芳华学院', '云南好物', '其他商品'])
  assert.equal(rows[2].receivedAmount, '0.01')
  assert.equal(rows[0].orderCount, 0)
  assert.deepEqual(businessRows(undefined), [])
})
test('dashboard requests selected order cohort, guards races, and removes CRM and demo counters', () => {
  const page = readFileSync(new URL('../src/views/index_v1.vue', import.meta.url), 'utf8')
  const cards = readFileSync(new URL('../src/views/dashboard/PanelGroup.vue', import.meta.url), 'utf8')
  assert.ok(page.includes('getUserStaticCount({ period: this.period })'))
  assert.ok(page.includes('request !== this.requestId'))
  assert.ok(page.includes('不是该期间现金流水'))
  for (const source of [page, cards]) for (const old of ['81212', '102400', 'expectedData', 'getCharData']) assert.ok(!source.includes(old))
  assert.ok(page.includes('business-mobile'))
})
