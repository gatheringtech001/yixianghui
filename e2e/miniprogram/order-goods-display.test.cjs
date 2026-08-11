const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')

async function loadOrderGoodsDisplay() {
  const sourcePath = path.resolve(
    __dirname,
    '../../shop-mnp/utils/orderGoodsDisplay.js'
  )
  const source = await fs.readFile(sourcePath, 'utf8')
  const encoded = Buffer.from(source).toString('base64')
  return import(`data:text/javascript;base64,${encoded}`)
}

test('order goods display separates product, spec and custom stay duration', async () => {
  const {
    collectGoodsIds,
    getOrderProductName,
    getOrderProductSpec
  } = await loadOrderGoodsDisplay()
  const hotel = {
    goodsId: 32,
    goodsType: 'hotel',
    goodsName: '湖景房',
    specifications: '湖景大床房'
  }
  const productNameMap = { 32: '昆明古滇基地' }

  assert.equal(
    getOrderProductName({ goodsId: 32 }, hotel, productNameMap),
    '昆明古滇基地'
  )
  assert.equal(
    getOrderProductSpec({ skuSeqNo: 2 }, hotel, productNameMap),
    '湖景大床房'
  )
  assert.equal(
    getOrderProductSpec({ skuSeqNo: 0, interCount: 3 }, hotel, productNameMap),
    '湖景大床房 · 自选4天3晚'
  )
  assert.deepEqual(
    collectGoodsIds([{ goodsId: 32 }, { goodsId: 32 }, { goodsId: 38 }]),
    [32, 38]
  )
})
