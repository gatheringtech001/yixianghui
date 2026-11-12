import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { goodsOrderQuery, resetGoodsOrderQuery, orderServices, orderBusinessLabel } from '../src/utils/goods-order-filters.mjs'
import { exportQuery } from '../src/utils/guanjia.mjs'
const categories = [
  { categoryId: 25, parentId: 0, categoryName: '全国旅居', status: '1' },
  { categoryId: 33, parentId: 0, categoryName: '云野好物', status: '1' },
  { categoryId: 58, parentId: 0, categoryName: '老年教育', status: '1' },
  { categoryId: 32, parentId: 0, categoryName: ' 聚会活动', status: '1', linkType: 'activity' },
  { categoryId: 57, parentId: 0, categoryName: '芳华学院', status: '0' },
  { categoryId: 63, parentId: 58, categoryName: '形象生活', status: '1' },
  { categoryId: 64, parentId: 63, categoryName: '历史子分类', status: '0' }
]
const entries = orderServices(categories)
test('business names follow current mini-program, not legacy type labels', () => {
  assert.deepEqual(entries.map(e => e.label), ['全国旅居', '聚会活动', '芳华学院', '云南好物'])
  const mini = readFileSync(new URL('../../shop-mnp/pages/classify/classify.vue', import.meta.url), 'utf8')
  entries.forEach(e => assert.ok(mini.includes(`<text>${e.label}</text>`)))
  assert.equal(entries.find(e => e.scope === 'education').categoryId, 58)
  assert.equal(entries.find(e => e.scope === 'activity').route, '/activity/app_activity_order')
})
test('business filters use category hierarchy and never constrain legacy goods type', () => {
  assert.deepEqual(goodsOrderQuery({ businessType: 'travel', goodsType: 'training', categoryId: null }, 'education', entries), { businessType: null, goodsType: null, categoryId: 58 })
  assert.equal(goodsOrderQuery({}, 'hotel', entries).categoryId, 25)
  assert.equal(goodsOrderQuery({}, 'retail', entries).categoryId, 33)
  assert.equal(goodsOrderQuery({}, 'unknown', entries).categoryId, -1)
  assert.equal(goodsOrderQuery({}, 'education', []).categoryId, -1)
})
test('all orders include unclassified and historical data without a type filter', () => {
  assert.deepEqual(goodsOrderQuery({ goodsType: 'hotel', categoryId: null }), { businessType: null, goodsType: null, categoryId: null })
})
test('subcategory, payment and export retain identical filters without mutation', () => {
  const query = { pageNum: 3, pageSize: 10, categoryId: 63, payStatus: '0', orderNo: 'synthetic' }
  const before = { ...query }
  const filtered = goodsOrderQuery(query, 'education', entries)
  assert.deepEqual(exportQuery(filtered), { categoryId: 63, payStatus: '0', orderNo: 'synthetic', businessType: null, goodsType: null })
  assert.deepEqual(query, before)
})
test('classification uses category ancestry even for mixed or missing legacy type codes', () => {
  assert.equal(orderBusinessLabel({ categoryId: 64, goodsType: 'training' }, categories), '芳华学院')
  assert.equal(orderBusinessLabel({ categoryId: 33 }, categories), '云南好物')
  assert.equal(orderBusinessLabel({ categoryId: 999, goodsType: 'education' }, categories), '其他商品')
  assert.equal(orderBusinessLabel({}, categories), '未分类')
  assert.equal(orderBusinessLabel({ categoryId: 1 }, [{ categoryId: 1, parentId: 1 }]), '其他商品')
})
test('tab changes and reset clear hidden filters and preserve page size', () => {
  const query = { pageNum: 5, pageSize: 20, categoryId: 63, channel: 'test', travelStatus: '1', isCash: '1', goodsType: 'hotel' }
  assert.deepEqual(resetGoodsOrderQuery(query), { pageNum: 1, pageSize: 20, categoryId: null, channel: null, travelStatus: null, isCash: null, goodsType: null })
  assert.equal(query.pageNum, 5)
})
