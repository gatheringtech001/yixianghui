const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const vm = require('node:vm')

function popup() {
  const source = fs.readFileSync(path.resolve(__dirname, '../../shop-mnp/uview-ui/components/u-popup/u-popup.vue'), 'utf8')
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1].replace('export default', 'module.exports =')
  const timers = new Map()
  const events = []
  let clock = 0
  let nextId = 0
  const context = {
    module: { exports: {} },
    setTimeout(fn, delay) { const id = ++nextId; timers.set(id, { fn, at: clock + delay }); return id },
    clearTimeout(id) { timers.delete(id) }
  }
  vm.runInNewContext(script, context)
  const component = context.module.exports
  const instance = { ...component.data(), popup: true, duration: 250,
    $emit: (...args) => events.push(args), $nextTick() {} }
  for (const [name, method] of Object.entries(component.methods)) instance[name] = method.bind(instance)
  return {
    instance, events,
    value(value) { component.watch.valueCom.handler.call(instance, value) },
    tick(ms) {
      const end = clock + ms
      while (true) {
        const first = [...timers.entries()].filter(([, t]) => t.at <= end).sort((a, b) => a[1].at - b[1].at)[0]
        if (!first) break
        const [id, timer] = first; timers.delete(id); clock = timer.at; timer.fn()
      }
      clock = end
    }
  }
}

test('hidden popup initialization does not emit a close back to its parent', () => {
  const p = popup()
  p.value(false)
  p.tick(300)
  assert.deepEqual(p.events, [])
})

test('a fast channel response is not hidden by the initialization close timer', () => {
  const p = popup()
  p.value(false)
  p.tick(20)
  p.value(true)
  p.tick(300)
  assert.equal(p.instance.visibleSync, true)
  assert.equal(p.instance.showDrawer, true)
})

test('closing during the opening animation cancels the pending open', () => {
  const p = popup()
  p.value(true)
  p.tick(10)
  p.value(false)
  p.tick(100)
  assert.equal(p.instance.showDrawer, false)
  p.tick(250)
  assert.equal(p.instance.visibleSync, false)
})

test('reopening during the closing animation leaves the popup visible', () => {
  const p = popup()
  p.value(true); p.tick(100)
  p.value(false); p.tick(20)
  p.value(true); p.tick(300)
  assert.equal(p.instance.visibleSync, true)
  assert.equal(p.instance.showDrawer, true)
})
