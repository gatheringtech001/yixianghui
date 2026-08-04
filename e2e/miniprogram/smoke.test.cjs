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
const localBackendPrefix = 'http://127.0.0.1:18080/api/profile/'

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

function hasImageSignature(bytes) {
  const ascii = bytes.toString('ascii', 0, 12)
  return (
    bytes.subarray(0, 4).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47])) ||
    bytes.subarray(0, 3).equals(Buffer.from([0xff, 0xd8, 0xff])) ||
    ascii.startsWith('GIF8') ||
    (ascii.startsWith('RIFF') && ascii.slice(8, 12) === 'WEBP')
  )
}

async function assertImageSourcesAvailable(sources, message) {
  const uniqueSources = [...new Set(sources)]
  assert.ok(uniqueSources.length > 0, `${message}: expected at least one image`)
  uniqueSources.forEach(source => {
    assert.equal(typeof source, 'string', `${message}: image source should be a string`)
    assert.ok(
      source.startsWith(localBackendPrefix),
      `${message}: image should come from the local backend: ${source}`
    )
  })

  const results = await Promise.all(uniqueSources.map(async source => {
    const response = await fetch(source)
    const bytes = Buffer.from(await response.arrayBuffer())
    return {
      source,
      status: response.status,
      ok: response.ok && hasImageSignature(bytes)
    }
  }))
  const failures = results
    .filter(result => !result.ok)
    .map(result => `${result.status} ${result.source}`)
  assert.deepEqual(failures, [], `${message}: every source should return an image`)
}

function extractRichTextImageSources(html) {
  return [...String(html || '').matchAll(/<img\b[^>]*\bsrc=["']([^"']+)["']/gi)]
    .map(match => match[1])
}

async function assertBackendImagesAvailable(miniProgram) {
  const sources = await miniProgram.evaluate(() => {
    const pages = getCurrentPages()
    const vm = pages[pages.length - 1].$vm
    return [
      vm.brandLogoDisplay,
      vm.housekeeperAvatarDisplay,
      vm.heroImage,
      ...(vm.hotCardList || []).map(item => item.adImage),
      ...(vm.currentGoodsList || []).map(item => item.goodsCover),
      ...(vm.contact || []).map(item => item.adImage)
    ].map(source => (
      typeof source === 'string' && source.startsWith('/profile/')
        ? vm.host + source
        : source
    ))
  })
  const backendSources = sources.filter(source => (
    typeof source === 'string' &&
    source.startsWith(localBackendPrefix)
  ))
  await assertImageSourcesAvailable(backendSources, 'home rendered backend images')
}

test('home loads backend site goods and search behavior works', { timeout: 120000 }, async testContext => {
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
        deptId: 108,
        deptName: '昆明'
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
      label: 'wait for backend site goods',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, [
          'currentCityDeptId',
          'currentGoodsList'
        ])
        return state.currentCityDeptId === 108 &&
          Array.isArray(state.currentGoodsList) &&
          state.currentGoodsList.length > 0
      })
    })
    const homeAssetState = await getCurrentPageState(miniProgram, [
      'brandLogoUrl',
      'housekeeperAvatarUrl'
    ])
    await assertImageSourcesAvailable([
      homeAssetState.brandLogoUrl,
      homeAssetState.housekeeperAvatarUrl
    ], 'home configured assets')
    await runStep(testContext, {
      label: 'verify rendered backend images',
      action: () => assertBackendImagesAvailable(miniProgram)
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

    await runStep(testContext, {
      label: 'return to home before tab checks',
      action: () => miniProgram.reLaunch('/pages/home/home')
    })
    await waitUntil(async () => {
      const page = await miniProgram.currentPage()
      return page && page.path === 'pages/home/home'
    })

    await runStep(testContext, {
      label: 'open customer service page',
      action: () => miniProgram.callWxMethod('switchTab', {
        url: '/pages/MembersOpened/MembersOpened'
      })
    })
    await runStep(testContext, {
      label: 'wait for customer service assets',
      action: () => waitUntil(async () => {
        const customerState = await getCurrentPageState(miniProgram, ['customerData'])
        const customerData = customerState.customerData || {}
        const staffList = customerData.staffList || []
        return typeof customerData.qrCode === 'string' &&
          customerData.qrCode.startsWith(localBackendPrefix) &&
          staffList.length >= 2 &&
          staffList.every(staff => (
            typeof staff.qrCode === 'string' && staff.qrCode.startsWith(localBackendPrefix)
          ))
      })
    })
    const customerState = await getCurrentPageState(miniProgram, ['customerData'])
    await assertImageSourcesAvailable([
      customerState.customerData.qrCode,
      customerState.customerData.headerBg,
      ...customerState.customerData.staffList.map(staff => staff.qrCode)
    ], 'customer service configured assets')
    await captureScreenshot(miniProgram, 'customer-service.png')

    await runStep(testContext, {
      label: 'open profile page',
      action: () => miniProgram.callWxMethod('switchTab', {
        url: '/pages/my/my'
      })
    })
    await runStep(testContext, {
      label: 'wait for profile steward asset',
      action: () => waitUntil(async () => {
        const profileState = await getCurrentPageState(miniProgram, ['stewardImageUrl'])
        return typeof profileState.stewardImageUrl === 'string' &&
          profileState.stewardImageUrl.startsWith(localBackendPrefix)
      })
    })
    const profileState = await getCurrentPageState(miniProgram, ['stewardImageUrl'])
    await assertImageSourcesAvailable(
      [profileState.stewardImageUrl],
      'profile steward asset'
    )
    await captureScreenshot(miniProgram, 'profile.png')

    await runStep(testContext, {
      label: 'open activity rich text page',
      action: () => miniProgram.reLaunch('/packagesMall/Activity/detail/index?id=1')
    })
    await runStep(testContext, {
      label: 'wait for activity rich text',
      action: () => waitUntil(async () => {
        const activityState = await getCurrentPageState(miniProgram, ['detailInfo'])
        return activityState.detailInfo && activityState.detailInfo.content
      })
    })
    const activityState = await getCurrentPageState(miniProgram, ['detailInfo'])
    await assertImageSourcesAvailable(
      extractRichTextImageSources(activityState.detailInfo.content),
      'activity rich text assets'
    )
    await captureScreenshot(miniProgram, 'activity-rich-text.png')

    await runStep(testContext, {
      label: 'open single page rich text',
      action: () => miniProgram.reLaunch('/packagesPublic/Article/index?id=3')
    })
    await runStep(testContext, {
      label: 'wait for single page rich text',
      action: () => waitUntil(async () => {
        const articleState = await getCurrentPageState(miniProgram, ['detailInfo'])
        return articleState.detailInfo && articleState.detailInfo.content
      })
    })
    const articleState = await getCurrentPageState(miniProgram, ['detailInfo'])
    await assertImageSourcesAvailable(
      extractRichTextImageSources(articleState.detailInfo.content),
      'single page rich text assets'
    )
    await captureScreenshot(miniProgram, 'single-page-rich-text.png')
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
