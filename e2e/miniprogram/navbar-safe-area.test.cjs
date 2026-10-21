const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const path = require('node:path')
const root = path.resolve(__dirname, '../../shop-mnp')
const source = fs.readFileSync(path.join(root, 'utils/navbarGeometry.js'), 'utf8')
const context = vm.createContext({})
vm.runInContext(source.replace('export ', ''), context)
const geometry = context.navbarTitleGeometry

test('long titles remain between the back button and actual capsule on all phone widths', () => {
  for (const viewport of [320, 360, 375, 390, 414, 430, 768]) {
    for (const capsuleWidth of [80, 96, 110]) {
      for (const back of [44, 60, 105]) {
        for (const requested of [125, 260, viewport * 2]) {
          const capsuleLeft = viewport - capsuleWidth
          const result = geometry(viewport, requested, capsuleLeft, back)
          const left = parseFloat(result.left), width = parseFloat(result.width)
          assert.ok(left >= back)
          assert.ok(left + width <= capsuleLeft - 8 + 0.001)
          assert.ok(width >= 0 && width <= requested)
          assert.equal('right' in result, false)
        }
      }
    }
  }
})

test('short titles retain screen centering when safe, independent of display mode', () => {
  const result = geometry(390, 100, 294, 44)
  assert.equal(result.left, '145px')
  assert.equal(result.width, '100px')
  assert.equal(geometry(375, 260, 279, 44).width, '227px')
})

test('common navbar uses capsule geometry and allows ellipsis inside its flex title', () => {
  const component = fs.readFileSync(path.join(root, 'uview-ui/components/u-navbar/u-navbar.vue'), 'utf8')
  assert.match(component, /return navbarTitleGeometry\(systemInfo.windowWidth/)
  assert.match(component, /class="u-title u-line-1"/)
  assert.match(component, /\.u-title\s*\{\s*min-width: 0;/)
  assert.doesNotMatch(component, /style.right = rightButtonWidth/)
})
