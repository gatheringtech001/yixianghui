const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const os = require('node:os')
const path = require('node:path')
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

async function launchMiniProgram(testContext, exceptions) {
  await fs.mkdir(resultsDir, { recursive: true })
  const projectConfig = JSON.parse(await fs.readFile(
    path.join(projectPath, 'project.config.json'),
    'utf8'
  ))
  assert.match(projectConfig.appid, /^wx[0-9a-z]+$/)
  assert.notEqual(projectConfig.appid, 'touristappid')
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
  return miniProgram
}

async function closeMiniProgram(testContext, miniProgram) {
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

async function invokeCurrentVmMethod(miniProgram, method, ...args) {
  return miniProgram.evaluate(({ methodName, methodArgs }) => {
    const pages = getCurrentPages()
    const vm = pages[pages.length - 1].$vm
    if (!vm || typeof vm[methodName] !== 'function') {
      throw new Error(`Missing page method: ${methodName}`)
    }
    return vm[methodName](...methodArgs)
  }, { methodName: method, methodArgs: args })
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
    return { source, status: response.status, ok: response.ok && hasImageSignature(bytes) }
  }))
  const failures = results.filter(result => !result.ok)
    .map(result => `${result.status} ${result.source}`)
  assert.deepEqual(failures, [], `${message}: every source should return an image`)
}

function extractRichTextImageSources(html) {
  return [...String(html || '').matchAll(/<img\b[^>]*\bsrc=["']([^"']+)["']/gi)]
    .map(match => match[1])
}

module.exports = {
  assertImageSourcesAvailable,
  captureScreenshot,
  closeMiniProgram,
  extractRichTextImageSources,
  getCurrentPageState,
  invokeCurrentVmMethod,
  launchMiniProgram,
  localBackendPrefix,
  runStep,
  setCurrentVmState,
  waitUntil
}
