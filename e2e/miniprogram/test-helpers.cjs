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
const defaultAutomationPort = 9425
const activeMiniPrograms = new Set()

function resolveAutomationPort() {
  const rawPort = process.env.MINIPROGRAM_AUTOMATION_PORT
  const port = rawPort === undefined ? defaultAutomationPort : Number(rawPort)
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error(`Invalid MINIPROGRAM_AUTOMATION_PORT: ${rawPort}`)
  }
  return port
}

function trackMiniProgram(miniProgram) {
  activeMiniPrograms.add(miniProgram)
  return miniProgram
}

function disconnectMiniProgram(testContext, miniProgram) {
  activeMiniPrograms.delete(miniProgram)
  try {
    miniProgram.disconnect()
  } catch (error) {
    testContext?.diagnostic?.(`automation disconnect failed: ${error.message}`)
  }
}

function disconnectTrackedMiniPrograms(testContext) {
  for (const miniProgram of [...activeMiniPrograms]) {
    disconnectMiniProgram(testContext, miniProgram)
  }
}

async function withAutomationTimeout(options) {
  const { action, testContext, timeoutMessage, timeoutMs } = options
  let timeout
  const timeoutError = new Error(timeoutMessage)
  try {
    return await Promise.race([
      Promise.resolve().then(action),
      new Promise((_, reject) => {
        timeout = setTimeout(() => {
          disconnectTrackedMiniPrograms(testContext)
          reject(timeoutError)
        }, timeoutMs)
      })
    ])
  } finally {
    clearTimeout(timeout)
  }
}

async function waitUntil(check, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const remainingMs = Math.max(1, deadline - Date.now())
    const matched = await withAutomationTimeout({
      action: check,
      timeoutMs: remainingMs,
      timeoutMessage: `Condition check timed out after ${timeoutMs} ms`
    })
    if (matched) return
    await new Promise(resolve => setTimeout(resolve, 250))
  }
  throw new Error(`Condition was not met within ${timeoutMs} ms`)
}

async function runStep(testContext, { label, action, timeoutMs = 20000 }) {
  testContext.diagnostic(label)
  return withAutomationTimeout({
    action,
    testContext,
    timeoutMessage: `${label} timed out`,
    timeoutMs
  })
}

async function connectOrLaunchAutomation(testContext, automatorClient, launchOptions) {
  const wsEndpoint = `ws://127.0.0.1:${launchOptions.port}`
  try {
    return await runStep(testContext, {
      label: `connect existing WeChat DevTools automation on ${launchOptions.port}`,
      timeoutMs: 5000,
      action: () => automatorClient.connect({ wsEndpoint })
    })
  } catch (error) {
    testContext.diagnostic(`existing automation unavailable: ${error.message}`)
  }

  return runStep(testContext, {
    label: 'enable WeChat DevTools automation without closing the project',
    timeoutMs: 70000,
    action: () => automatorClient.launch(launchOptions)
  })
}

async function launchMiniProgram(testContext, exceptions) {
  await fs.mkdir(resultsDir, { recursive: true })
  const projectConfig = JSON.parse(await fs.readFile(
    path.join(projectPath, 'project.config.json'),
    'utf8'
  ))
  assert.match(projectConfig.appid, /^wx[0-9a-z]+$/)
  assert.notEqual(projectConfig.appid, 'touristappid')
  const miniProgram = await connectOrLaunchAutomation(testContext, automator, {
    cliPath,
    projectPath,
    port: resolveAutomationPort(),
    trustProject: true,
    timeout: 60000,
    projectConfig: {
      appid: projectConfig.appid,
      libVersion: '3.16.2',
      setting: { urlCheck: false }
    }
  })
  trackMiniProgram(miniProgram)
  miniProgram.on('exception', error => exceptions.push(error))
  return miniProgram
}

async function closeMiniProgram(testContext, miniProgram) {
  testContext.diagnostic('disconnect WeChat DevTools automation; keep project open')
  disconnectMiniProgram(testContext, miniProgram)
}

function installSignalCleanup() {
  for (const signal of ['SIGINT', 'SIGTERM']) {
    const handler = () => {
      disconnectTrackedMiniPrograms()
      process.removeListener(signal, handler)
      process.kill(process.pid, signal)
    }
    process.once(signal, handler)
  }
}

installSignalCleanup()

async function getCurrentPageState(miniProgram, fields, timeoutMs = 20000) {
  trackMiniProgram(miniProgram)
  return withAutomationTimeout({
    timeoutMs,
    timeoutMessage: 'read current page state timed out',
    action: () => miniProgram.evaluate(requestedFields => {
      const pages = getCurrentPages()
      const page = pages[pages.length - 1]
      const state = { route: page.route }
      for (const field of requestedFields) {
        state[field] = page.data[field]
      }
      return state
    }, fields)
  })
}

async function setCurrentVmState(miniProgram, data, timeoutMs = 20000) {
  trackMiniProgram(miniProgram)
  return withAutomationTimeout({
    timeoutMs,
    timeoutMessage: 'write current page state timed out',
    action: () => miniProgram.evaluate(nextData => {
      const pages = getCurrentPages()
      const vm = pages[pages.length - 1].$vm
      Object.assign(vm, nextData)
    }, data)
  })
}

async function invokeCurrentVmMethod(miniProgram, method, ...args) {
  trackMiniProgram(miniProgram)
  return withAutomationTimeout({
    timeoutMs: 20000,
    timeoutMessage: `invoke page method ${method} timed out`,
    action: () => miniProgram.evaluate(({ methodName, methodArgs }) => {
      const pages = getCurrentPages()
      const vm = pages[pages.length - 1].$vm
      if (!vm || typeof vm[methodName] !== 'function') {
        throw new Error(`Missing page method: ${methodName}`)
      }
      return vm[methodName](...methodArgs)
    }, { methodName: method, methodArgs: args })
  })
}

async function captureScreenshot(miniProgram, fileName, timeoutMs = 40000) {
  trackMiniProgram(miniProgram)
  const data = await withAutomationTimeout({
    timeoutMs,
    timeoutMessage: 'capture screenshot timed out',
    action: () => miniProgram.screenshot()
  })
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
  connectOrLaunchAutomation,
  extractRichTextImageSources,
  getCurrentPageState,
  invokeCurrentVmMethod,
  launchMiniProgram,
  localBackendPrefix,
  runStep,
  setCurrentVmState,
  trackMiniProgram,
  waitUntil
}
