import assert from 'node:assert/strict'
import test from 'node:test'

import { normalizeRichTextHtml } from '../utils/richText.js'

test('normalizes fixed-size product detail images to the available width', () => {
	const html = '<img width="1200" height="1800" style="width:1200px;height:1800px;max-width:none" src="a.jpg">'
	const result = normalizeRichTextHtml(html)

	assert.doesNotMatch(result, /width="1200"/)
	assert.doesNotMatch(result, /height="1800"/)
	assert.match(result, /width:100%/)
	assert.match(result, /max-width:100%/)
	assert.doesNotMatch(result, /height:auto/)
})

test('removes clipping dimensions from rich-text wrappers', () => {
	const html = '<div style="width:1200px;height:300px;overflow:hidden"><img src="a.jpg"></div>'
	const result = normalizeRichTextHtml(html)

	assert.doesNotMatch(result, /width\s*:\s*1200px/)
	assert.doesNotMatch(result, /height\s*:\s*300px/)
	assert.doesNotMatch(result, /overflow\s*:\s*hidden/)
})
