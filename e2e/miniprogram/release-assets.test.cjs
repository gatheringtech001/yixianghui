const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')

const projectRoot = path.resolve(__dirname, '../..')
const signInPage = path.join(projectRoot, 'shop-mnp/pages/SignIn/SignIn.vue')
const heroJpeg = path.join(projectRoot, 'shop-mnp/static/home-design/coin-hero-bg.jpg')
const heroPng = path.join(projectRoot, 'shop-mnp/static/home-design/coin-hero-bg.png')

test('obsolete Settings icons are excluded without excluding referenced assets', async () => {
  const root = path.join(projectRoot, 'shop-mnp')
  const manifest = JSON.parse(await fs.readFile(path.join(root, 'manifest.json'), 'utf8'))
  assert.ok(manifest['mp-weixin'].packOptions.ignore.some(
    entry => entry.type === 'folder' && entry.value === 'static/settings-icons'))

  async function checkReferences(directory) {
    for (const entry of await fs.readdir(directory, { withFileTypes: true })) {
      if (['node_modules', 'unpackage', '.git'].includes(entry.name)) continue
      const file = path.join(directory, entry.name)
      if (entry.isDirectory()) await checkReferences(file)
      else if (/\.(vue|js|json|scss|css)$/.test(entry.name) && file !== path.join(root, 'manifest.json')) {
        assert.doesNotMatch(await fs.readFile(file, 'utf8'), /settings-icons/, file)
      }
    }
  }
  await checkReferences(root)
})

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
  assert.ok(total <= 160 * 1024, `main package photographs use ${total} bytes`)
})
