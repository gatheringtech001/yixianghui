import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import { detailImageUrl } from '../utils/detailImagePreview.js'

test('actual reservation list normalizes absolute, relative and missing covers without double host', () => {
	const source = readFileSync(new URL('../packagesMember/MyActivity/index.vue', import.meta.url), 'utf8')
	const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
		.replace(/^\s*import[^\n]+$/gm, '').replace('export default', 'module.exports =')
	const context = { module: { exports: {} }, detailImageUrl }
	vm.runInNewContext(script, context)
	const page = { host: 'https://shzxj.lk01.cn/api/' }
	const coverUrl = context.module.exports.methods.coverUrl.bind(page)
	for (const cover of ['https://shzxj.lk01.cn/api/profile/a.jpg', 'http://example.test/a.jpg', '//example.test/a.jpg', '/profile/a.jpg', 'profile/a.jpg', '']) {
		const url = coverUrl(cover)
		assert.equal(url, detailImageUrl(cover, page.host) || '/static/home-design/entry-stay.jpg')
		assert.doesNotMatch(url, /api\/https?:/)
	}
	assert.match(source, /:src="coverUrl\(item.activityCover\)" mode="aspectFill"/)
	assert.doesNotMatch(source, /host\s*\+\s*item.activityCover/)
})
