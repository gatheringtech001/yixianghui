const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const read = file => fs.readFileSync(path.resolve(__dirname, '../../shop-mnp', file), 'utf8')

function loadPreview(uni = {}) {
  const script = read('utils/detailImagePreview.js').replace(/export /g, '')
  return vm.runInNewContext(script + '\n({detailImageUrl, buildDetailImagePreview, previewDetailImages})', {uni})
}

test('preview resolves original URLs, static and absolute images without double host prefixes', () => {
  const {detailImageUrl} = loadPreview()
  const host = 'https://example.invalid/api/'
  assert.equal(detailImageUrl('/profile/a.jpg', host), host + 'profile/a.jpg')
  assert.equal(detailImageUrl('profile/a.jpg', host), host + 'profile/a.jpg')
  assert.equal(detailImageUrl('https://cdn.invalid/a.jpg', host), 'https://cdn.invalid/a.jpg')
  assert.equal(detailImageUrl('//cdn.invalid/a.jpg', host), 'https://cdn.invalid/a.jpg')
  assert.equal(detailImageUrl('@/static/a.jpg', host), '/static/a.jpg')
  assert.equal(detailImageUrl('data:image/png;base64,abc', host), 'data:image/png;base64,abc')
  assert.equal(detailImageUrl(undefined, host), '')
})

test('gallery opens the tapped original, filters blanks and keeps swipe order', () => {
  const {buildDetailImagePreview} = loadPreview()
  const options = buildDetailImagePreview('/b.jpg', ['/a.jpg', '', '/b.jpg', '/a.jpg', null], 'https://x.invalid')
  assert.equal(options.current, 'https://x.invalid/b.jpg')
  assert.deepEqual(Array.from(options.urls), ['https://x.invalid/a.jpg', 'https://x.invalid/b.jpg'])
  assert.equal(buildDetailImagePreview('', ['/a.jpg'], ''), null)
  assert.deepEqual(Array.from(buildDetailImagePreview('/c.jpg', ['/a.jpg'], '').urls), ['/c.jpg', '/a.jpg'])
})

test('shared page method invokes native zoomable preview; empty images do nothing and failures show feedback', () => {
  const calls = [], toasts = []
  const {previewDetailImages} = loadPreview({previewImage: options => calls.push(options), showToast: options => toasts.push(options)})
  previewDetailImages.call({host: 'https://x.invalid'}, '', [])
  assert.equal(calls.length, 0)
  previewDetailImages.call({host: 'https://x.invalid'}, '/b.jpg', ['/a.jpg', '/b.jpg'])
  assert.equal(calls.length, 1)
  assert.equal(calls[0].current, 'https://x.invalid/b.jpg')
  assert.deepEqual(Array.from(calls[0].urls), ['https://x.invalid/a.jpg', 'https://x.invalid/b.jpg'])
  calls[0].fail()
  assert.equal(toasts.length, 1)
})

test('travel, activity and education covers and rich-text images all opt into preview', () => {
  for (const file of ['GoodsDetails/SojournGoodsDetails.vue', 'GoodsDetails/EducationGoodsDetails.vue', 'Activity/detail/index.vue']) {
    const page = read('packagesMall/' + file)
    assert.match(page, /import \{[^}]*previewDetailImages[^}]*\} from '@\/utils\/detailImagePreview'/)
    assert.match(page, /@tap\.stop="previewDetailImages\(/)
    assert.match(page, /<u-parse[^>]*:preview="true"/)
    assert.match(page, /return previewDetailImages\.call\(this, current, images\)/)
  }
  const travel = read('packagesMall/GoodsDetails/SojournGoodsDetails.vue')
  assert.match(travel, /previewDetailImages\(item, bannerImages\)/)
  assert.match(travel, /previewDetailImages\(resolveCoverUrl\(skuGroup.cover\)\)/)
})
