const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const os = require('node:os')
const path = require('node:path')
const test = require('node:test')
const automator = require('miniprogram-automator')

const cliPath = process.env.WECHAT_DEVTOOLS_CLI ||
  '/Applications/wechatwebdevtools.app/Contents/MacOS/cli'
const projectPath = process.env.MINIPROGRAM_PROJECT_PATH || path.resolve(
  __dirname,
  '../../shop-mnp/unpackage/dist/dev/mp-weixin'
)
const resultsDir = path.join(os.tmpdir(), 'yixianghui-e2e', 'miniprogram')

async function waitUntil(check, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (await check()) return
    await new Promise(resolve => setTimeout(resolve, 250))
  }
  throw new Error(`Condition was not met within ${timeoutMs} ms`)
}

async function runStep(testContext, { label, action, timeoutMs = 20000 }) {
  testContext.diagnostic(label)
  let timeout
  try {
    return await Promise.race([
      action(),
      new Promise((_, reject) => {
        timeout = setTimeout(() => reject(new Error(`${label} timed out`)), timeoutMs)
      })
    ])
  } finally {
    clearTimeout(timeout)
  }
}

async function getCurrentPageState(miniProgram, fields) {
  return miniProgram.evaluate(requestedFields => {
    const pages = getCurrentPages()
    const page = pages[pages.length - 1]
    const state = { route: page.route }
    for (const field of requestedFields) {
      state[field] = page.data[field]
    }
    return state
  }, fields)
}

async function setCurrentVmState(miniProgram, data) {
  return miniProgram.evaluate(nextData => {
    const pages = getCurrentPages()
    const vm = pages[pages.length - 1].$vm
    Object.assign(vm, nextData)
  }, data)
}

async function invokeCurrentVmMethod(miniProgram, method) {
  return miniProgram.evaluate(methodName => {
    const pages = getCurrentPages()
    const vm = pages[pages.length - 1].$vm
    if (!vm || typeof vm[methodName] !== 'function') {
      throw new Error(`Missing page method: ${methodName}`)
    }
    return vm[methodName]()
  }, method)
}

async function captureScreenshot(miniProgram, fileName) {
  const data = await miniProgram.screenshot()
  assert.equal(typeof data, 'string', 'screenshot should return base64 data')
  const outputPath = path.join(resultsDir, fileName)
  await fs.writeFile(outputPath, data, 'base64')
  return outputPath
}

test('home loads backend data and search behavior works', { timeout: 120000 }, async testContext => {
  await fs.mkdir(resultsDir, { recursive: true })
  const projectConfig = JSON.parse(await fs.readFile(
    path.join(projectPath, 'project.config.json'),
    'utf8'
  ))
  assert.match(projectConfig.appid, /^wx[0-9a-z]+$/)
  assert.notEqual(projectConfig.appid, 'touristappid')
  const exceptions = []
  const miniProgram = await runStep(testContext, {
    label: 'launch WeChat DevTools automation',
    timeoutMs: 70000,
    action: () => automator.launch({
      cliPath,
      projectPath,
      trustProject: true,
      timeout: 60000,
      projectConfig: {
        appid: projectConfig.appid,
        libVersion: '3.16.2',
        setting: { urlCheck: false }
      }
    })
  })

  miniProgram.on('exception', error => exceptions.push(error))

  try {
    await runStep(testContext, {
      label: 'seed local site selection',
      action: () => miniProgram.callWxMethod('setStorageSync', 'site', {
        deptId: 100,
        deptName: 'E2E Test Site'
      })
    })
    const home = await runStep(testContext, {
      label: 'open home page',
      action: () => miniProgram.reLaunch('/pages/home/home')
    })
    assert.ok(home, 'home page should open')
    assert.equal(home.path, 'pages/home/home')
    const homeState = await getCurrentPageState(miniProgram, ['host'])
    assert.equal(homeState.host, 'http://127.0.0.1:18080/api')

    await runStep(testContext, {
      label: 'wait for backend categories',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['navList'])
        return Array.isArray(state.navList) && state.navList.length > 0
      })
    })
    await runStep(testContext, {
      label: 'capture home screenshot',
      action: () => captureScreenshot(miniProgram, 'home.png')
    })

    await runStep(testContext, {
      label: 'invoke home search behavior',
      action: () => invokeCurrentVmMethod(miniProgram, 'onSearch')
    })

    await runStep(testContext, {
      label: 'wait for search page',
      action: () => waitUntil(async () => {
        const page = await miniProgram.currentPage()
        return page && page.path === 'packagesMall/search/search'
      })
    })
    const searchPage = await miniProgram.currentPage()
    assert.equal(searchPage.path, 'packagesMall/search/search')
    await runStep(testContext, {
      label: 'set search keyword',
      action: () => setCurrentVmState(miniProgram, { keyword: '旅居' })
    })
    await waitUntil(async () => {
      const state = await getCurrentPageState(miniProgram, ['keyword'])
      return state.keyword === '旅居'
    })
    const keywordState = await getCurrentPageState(miniProgram, ['keyword'])
    assert.equal(keywordState.keyword, '旅居')

    await runStep(testContext, {
      label: 'submit search behavior',
      action: () => invokeCurrentVmMethod(miniProgram, 'onSearch')
    })
    await runStep(testContext, {
      label: 'wait for search response',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['searching'])
        return state.searching === false
      })
    })
    const state = await getCurrentPageState(miniProgram, [
      'keyword',
      'showSearchResult',
      'showSearchEmpty'
    ])
    assert.equal(state.keyword, '旅居')
    assert.equal(state.showSearchResult || state.showSearchEmpty, true)
    await runStep(testContext, {
      label: 'capture search screenshot',
      action: () => captureScreenshot(miniProgram, 'search.png')
    })
    assert.deepEqual(exceptions, [])
  } finally {
    try {
      await runStep(testContext, {
        label: 'close WeChat DevTools automation',
        timeoutMs: 5000,
        action: () => miniProgram.close()
      })
    } catch (error) {
      miniProgram.disconnect()
      testContext.diagnostic(error.message)
    }
  }
})
