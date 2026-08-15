const assert = require('node:assert/strict')
const test = require('node:test')

const {
  closeMiniProgram,
  connectOrLaunchAutomation,
  getCurrentPageState,
  runStep,
  trackMiniProgram,
  waitUntil
} = require('./test-helpers.cjs')

function testContext() {
  return { diagnostic() {} }
}

test('cleanup disconnects automation without closing the project window', async () => {
  let closeCalls = 0
  let disconnectCalls = 0
  const miniProgram = {
    close() { closeCalls += 1 },
    disconnect() { disconnectCalls += 1 }
  }

  trackMiniProgram(miniProgram)
  await closeMiniProgram(testContext(), miniProgram)

  assert.equal(disconnectCalls, 1)
  assert.equal(closeCalls, 0)
})

test('a timed-out step disconnects the stuck automation session', async () => {
  let disconnectCalls = 0
  const miniProgram = {
    disconnect() { disconnectCalls += 1 }
  }
  trackMiniProgram(miniProgram)

  await assert.rejects(
    runStep(testContext(), {
      label: 'stuck interaction',
      timeoutMs: 10,
      action: () => new Promise(() => {})
    }),
    /stuck interaction timed out/
  )

  assert.equal(disconnectCalls, 1)
})

test('a stalled condition check cannot bypass the overall wait timeout', async () => {
  let disconnectCalls = 0
  trackMiniProgram({
    disconnect() { disconnectCalls += 1 }
  })

  const guard = new Promise((_, reject) => {
    setTimeout(() => reject(new Error('test guard expired')), 150)
  })
  await assert.rejects(
    Promise.race([
      waitUntil(() => new Promise(() => {}), 20),
      guard
    ]),
    /Condition check timed out after 20 ms/
  )

  assert.equal(disconnectCalls, 1)
})

test('a direct state read has its own timeout and releases the connection', async () => {
  let disconnectCalls = 0
  const miniProgram = {
    disconnect() { disconnectCalls += 1 },
    evaluate() { return new Promise(() => {}) }
  }
  trackMiniProgram(miniProgram)

  const guard = new Promise((_, reject) => {
    setTimeout(() => reject(new Error('test guard expired')), 150)
  })
  await assert.rejects(
    Promise.race([
      getCurrentPageState(miniProgram, [], 20),
      guard
    ]),
    /read current page state timed out/
  )

  assert.equal(disconnectCalls, 1)
})

test('automation reuses the fixed existing endpoint before invoking the CLI', async () => {
  const existing = { disconnect() {} }
  const calls = []
  const automator = {
    async connect(options) {
      calls.push(['connect', options])
      return existing
    },
    async launch(options) {
      calls.push(['launch', options])
      throw new Error('launch should not be called')
    }
  }

  const result = await connectOrLaunchAutomation(testContext(), automator, {
    port: 9425,
    projectPath: '/tmp/project'
  })

  assert.equal(result, existing)
  assert.deepEqual(calls, [[
    'connect',
    { wsEndpoint: 'ws://127.0.0.1:9425' }
  ]])
})

test('automation enables a session without closing DevTools when no endpoint exists', async () => {
  const launched = { disconnect() {} }
  const calls = []
  const automator = {
    async connect() {
      calls.push('connect')
      throw new Error('connection refused')
    },
    async launch(options) {
      calls.push(['launch', options])
      return launched
    }
  }
  const options = { port: 9425, projectPath: '/tmp/project' }

  const result = await connectOrLaunchAutomation(testContext(), automator, options)

  assert.equal(result, launched)
  assert.deepEqual(calls, ['connect', ['launch', options]])
})
