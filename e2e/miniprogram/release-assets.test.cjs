const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')

const projectRoot = path.resolve(__dirname, '../..')
const signInPage = path.join(projectRoot, 'shop-mnp/pages/SignIn/SignIn.vue')
const heroJpeg = path.join(projectRoot, 'shop-mnp/static/home-design/coin-hero-bg.jpg')
const heroPng = path.join(projectRoot, 'shop-mnp/static/home-design/coin-hero-bg.png')

test('coin hero uses the compressed release asset', async () => {
  const source = await fs.readFile(signInPage, 'utf8')
  const asset = await fs.stat(heroJpeg)

  assert.match(source, /\/static\/home-design\/coin-hero-bg\.jpg/)
  assert.equal(await fs.stat(heroPng).then(() => true, () => false), false)
  assert.ok(asset.size < 100 * 1024, `coin hero is ${asset.size} bytes`)
})

test('main package photographic assets stay within the release budget', async () => {
  const names = ['entry-stay-wide.jpg', 'entry-yunnan.jpg', 'entry-activity.jpg',
    'entry-education.jpg', 'entry-stay.jpg', 'coin-hero-bg.jpg']
  let total = 0
  for (const name of names) {
    total += (await fs.stat(path.join(projectRoot, 'shop-mnp/static/home-design', name))).size
  }
  assert.ok(total <= 180 * 1024, `main package photographs use ${total} bytes`)
})
