const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')

const appRoot = path.resolve(__dirname, '../../shop-mnp')

async function loadRotationModule() {
  const source = await fs.readFile(
    path.join(appRoot, 'utils/housekeeperRotation.js'),
    'utf8'
  )
  const encoded = Buffer.from(source).toString('base64')
  return import(`data:text/javascript;base64,${encoded}`)
}

test('housekeeper selector alternates deterministically and wraps', async () => {
  const { selectRotatingHousekeeper } = await loadRotationModule()
  const staff = [{ name: '媛媛' }, { name: '曼曼' }]

  assert.deepEqual(selectRotatingHousekeeper(staff, ''), {
    item: staff[0],
    nextCursor: 1
  })
  assert.deepEqual(selectRotatingHousekeeper(staff, 1), {
    item: staff[1],
    nextCursor: 0
  })
  assert.deepEqual(selectRotatingHousekeeper(staff, 8), {
    item: staff[0],
    nextCursor: 1
  })
  assert.deepEqual(selectRotatingHousekeeper([], 0), {
    item: null,
    nextCursor: 0
  })
})

test('home and profile add-housekeeper actions reuse customer staff records', async () => {
  for (const page of ['pages/home/home.vue', 'pages/my/my.vue']) {
    const source = await fs.readFile(path.join(appRoot, page), 'utf8')
    assert.match(source, /@click\.stop="openHousekeeper"/, page)
    assert.match(source, /positionId:\s*CUSTOMER_SERVICE_POSITION_ID/, page)
    assert.match(source, /selectRotatingHousekeeper\(/, page)
    assert.match(source, /selectedContact\.adImage/, page)
    assert.doesNotMatch(source, /contact\[1\]/, page)
  }
})

test('customer service fallback and labels match the two named housekeepers', async () => {
  const source = await fs.readFile(
    path.join(appRoot, 'pages/MembersOpened/MembersOpened.vue'),
    'utf8'
  )

  assert.match(source, /客服\{\{ index \+ 1 \}\} \{\{ staff\.name \}\}/)
  assert.match(source, /name:\s*'媛媛'[\s\S]*wechat:\s*'15887297809'/)
  assert.match(source, /name:\s*'曼曼'[\s\S]*wechat:\s*'18008890435'/)
  assert.doesNotMatch(source, /name:\s*'小刘'/)
  assert.doesNotMatch(source, /name:\s*'小兰'/)
  assert.doesNotMatch(source, /name:\s*'小陈'/)
})

test('local production snapshot keeps the three verified QR assets mapped correctly', async () => {
  const fixture = await fs.readFile(
    path.resolve(__dirname, '../../sql/e2e-production-assets.sql'),
    'utf8'
  )

  assert.match(fixture, /\(19, 3, '二维码',[\s\S]*'\/profile\/upload\/2026\/08\/27\/customer-service-qr-v1\/travel-group-qr\.png'/)
  assert.match(fixture, /\(20, 7, '媛媛', '15887297809', '\/profile\/upload\/2026\/08\/27\/customer-service-qr-v1\/housekeeper-yuanyuan-qr\.png'/)
  assert.match(fixture, /\(21, 7, '曼曼', '18008890435', '\/profile\/upload\/2026\/08\/27\/customer-service-qr-v1\/housekeeper-manman-qr\.png'/)
  assert.match(fixture, /housekeeper-yuanyuan-qr\.png'[^\n]*, 1, '1'\)/)
  assert.match(fixture, /housekeeper-manman-qr\.png'[^\n]*, 2, '1'\)/)
})
