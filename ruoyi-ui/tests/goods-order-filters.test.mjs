import test from 'node:test'
import assert from 'node:assert/strict'
import { goodsOrderQuery, resetGoodsOrderQuery } from '../src/utils/goods-order-filters.mjs'
import { exportQuery } from '../src/utils/guanjia.mjs'

test('all orders never retain legacy travel inference', () => {
  assert.deepEqual(goodsOrderQuery({ businessType: 'travel', goodsType: 'hotel' }), { businessType: null, goodsType: null })
})
test('dictionary scopes are passed exactly, including future listing types', () => {
  for (const scope of ['training', 'hotel', 'education', 'new-type']) {
    assert.equal(goodsOrderQuery({ businessType: 'education' }, scope).goodsType, scope)
    assert.equal(goodsOrderQuery({}, scope).businessType, null)
  }
})
test('category, unpaid filter and order number survive query/export without mutation', () => {
  const query = { pageNum: 3, pageSize: 10, categoryId: 63, payStatus: '0', orderNo: 'synthetic' }
  const before = { ...query }
  const filtered = goodsOrderQuery(query, 'education')
  assert.equal(filtered.categoryId, 63)
  assert.equal(filtered.payStatus, '0')
  assert.deepEqual(exportQuery(filtered), { categoryId: 63, payStatus: '0', orderNo: 'synthetic', businessType: null, goodsType: 'education' })
  assert.deepEqual(query, before)
})
test('tab changes and reset clear hidden type-specific filters and preserve page size', () => {
  const query = { pageNum: 5, pageSize: 20, categoryId: 63, channel: 'test', travelStatus: '1', isCash: '1', goodsType: 'hotel' }
  assert.deepEqual(resetGoodsOrderQuery(query), { pageNum: 1, pageSize: 20, categoryId: null, channel: null, travelStatus: null, isCash: null, goodsType: null })
  assert.equal(query.pageNum, 5)
})
