import test from 'node:test'
import assert from 'node:assert/strict'
import { publicationIssues, publicationLabel, treeLabel, plainContent, reviewQuery, isNewPublication } from '../src/utils/listing-operations.mjs'

const goods = { goodsName: '测试课程', goodsCover: '/cover.jpg', deptId: 105, categoryId: 63, price: 250, stock: 5, goodsType: 'education' }
test('publication labels preserve the existing 0/1 contract', () => {
  assert.equal(publicationLabel('1'), '已上架'); assert.equal(publicationLabel(0), '未上架')
})
test('category and site names resolve nested ids rather than showing raw ids', () => {
  assert.equal(treeLabel([{ categoryId: 0, children: [{ categoryId: 63, categoryName: '形象生活' }] }], 63), '形象生活')
  assert.equal(treeLabel([], 63), '')
})
test('course publication requires sellable stock without inventing unspecified dates', () => {
  assert.deepEqual(publicationIssues(goods, 'goods'), [])
  assert.ok(publicationIssues({ ...goods, stock: 0 }, 'goods').includes('填写可售库存'))
})
test('paid activity uses actual payable price, dates and capacity', () => {
  const activity = { activityName: '测试活动', activityCover: '/cover.jpg', deptId: 105, address: '测试地址', activityTime: '周末', isFree: 0, price: 50, vipPrice: 0 }
  assert.ok(publicationIssues(activity, 'activity').includes('填写大于 0 的实付价'))
  assert.deepEqual(publicationIssues({ ...activity, isFree: 1 }, 'activity'), [])
  assert.ok(publicationIssues({ ...activity, isFree: 1, signEndTime: '2020-01-01 10:00:00' }, 'activity', [], Date.UTC(2026, 10, 9)).includes('报名截止时间已过'))
})
test('multispec publication checks active stocked priced SKU', () => {
  const form = { ...goods, goodsType: 'travel', isSku: 1 }
  assert.ok(publicationIssues(form, 'goods', []).length)
  assert.deepEqual(publicationIssues(form, 'goods', [{ status: '1', stock: 2, price: 100 }]), [])
})
test('preview never executes rich text as HTML', () => {
  assert.equal(plainContent('<p>第一段</p><p>第二段</p>'), '第一段\n第二段')
})
test('review tabs filter at the server before pagination', () => {
  assert.equal(reviewQuery({ pageNum: 2 }, 'pending').status, '0')
  assert.equal(reviewQuery({}, 'processed')['params[reviewBucket]'], 'processed')
  assert.equal(reviewQuery({}, 'exception').status, '5')
  assert.equal(reviewQuery({}, 'all').status, null)
})

test('readiness only gates a transition to publication, not existing sold-out content edits', () => {
  assert.equal(isNewPublication('1', '1'), false)
  assert.equal(isNewPublication('0', '1'), true)
  assert.equal(isNewPublication(null, '1'), true)
  assert.equal(isNewPublication('1', '0'), false)
})
