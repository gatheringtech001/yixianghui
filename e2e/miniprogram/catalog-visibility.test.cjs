const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')

async function loadCatalogModule() {
  const sourcePath = path.resolve(__dirname, '../../shop-mnp/utils/travelCatalog.js')
  const source = await fs.readFile(sourcePath, 'utf8')
  const moduleUrl = 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
  return import(moduleUrl)
}

test('travel city cards exclude empty and disabled catalog records', async () => {
  const { buildHotCityCards } = await loadCatalogModule()
  const categories = [
    { categoryId: 25, parentId: 0, categoryName: '全国旅居', status: '1' },
    { categoryId: 38, parentId: 25, categoryName: '昆明', orderNum: 1, status: '1' },
    { categoryId: 27, parentId: 25, categoryName: '曲靖', orderNum: 2, status: '1' },
    { categoryId: 64, parentId: 25, categoryName: '普洱', orderNum: 3, status: '1' },
    { categoryId: 57, parentId: 25, categoryName: '弥勒', orderNum: 4, status: '1' }
  ]
  const ads = [
    { contentId: 17, adName: '曲靖', adImage: '/old-qujing.jpg', linkUrl: '27', orderNum: 1, status: '1' },
    { contentId: 14, adName: '昆明', adImage: '/kunming.jpg', linkUrl: '38', orderNum: 2, status: '1' }
  ]
  const goods = [
    { goodsId: 1, categoryId: 38, goodsName: '昆明基地', goodsCover: '/kunming-base.jpg', goodsType: 'hotel', status: '1' },
    { goodsId: 2, categoryId: 27, goodsName: '空封面测试', goodsCover: '', goodsType: 'hotel', status: '1' },
    { goodsId: 3, categoryId: 64, goodsName: '普洱基地', goodsCover: '/puer-base.jpg', goodsType: 'hotel', status: '1' },
    { goodsId: 4, categoryId: 57, goodsName: '旧版弥勒基地', goodsCover: '/mile-old.jpg', goodsType: 'hotel', status: '0' }
  ]

  const cards = buildHotCityCards(categories, ads, goods)

  assert.deepEqual(cards.map(item => item.adName), ['昆明', '普洱'])
  assert.deepEqual(cards.map(item => item.goodsCount), [1, 1])
  assert.equal(cards[1].adImage, '/static/home-design/city-puer.jpg')
})
