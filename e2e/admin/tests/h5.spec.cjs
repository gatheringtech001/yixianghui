const fs = require('node:fs/promises')
const path = require('node:path')
const { test, expect } = require('@playwright/test')

const evidenceRoot = process.env.E2E_EVIDENCE_ROOT || path.resolve(
  __dirname,
  '../../../outputs/ui-admin-e2e'
)

async function seedH5Site(page, site = { deptId: 108, deptName: '昆明' }) {
  await page.addInitScript(value => {
    localStorage.setItem('site', JSON.stringify({ type: 'object', data: value }))
  }, site)
}

test('H5-HOME-001 H5 首页可渲染并连接本地后端', async ({ page }, testInfo) => {
  const pageErrors = []
  const serverErrors = []
  const apiResponses = []
  page.on('pageerror', error => pageErrors.push(error.message))
  page.on('response', async response => {
    const url = response.url()
    if (url.startsWith('http://127.0.0.1') && response.status() >= 500) {
      serverErrors.push(`${response.status()} ${url}`)
    }
    if (url.startsWith('http://127.0.0.1:18080/api/')) {
      apiResponses.push({
        method: response.request().method(),
        status: response.status(),
        url
      })
    }
  })

  await seedH5Site(page)
  await page.goto('/')
  await expect(page).toHaveTitle('逸享荟')
  await expect(page.locator('uni-page-body')).toBeVisible()
  await expect(page.getByText('逸享荟康养', { exact: true })).toBeVisible()
  await expect.poll(() => apiResponses.some(
    item => item.url.includes('/get_goods_category')
  )).toBe(true)
  await expect.poll(() => apiResponses.some(
    item => item.url.includes('/queryGoodsList')
  )).toBe(true)
  const screenshotsDir = path.join(evidenceRoot, 'screenshots')
  await fs.mkdir(screenshotsDir, { recursive: true })
  await page.screenshot({
    path: path.join(screenshotsDir, 'H5-HOME-001-home.png'),
    fullPage: true
  })

  await testInfo.attach('runtime-observation.json', {
    body: JSON.stringify({ pageErrors, serverErrors, apiResponses }, null, 2),
    contentType: 'application/json'
  })
  expect(pageErrors).toEqual([])
  expect(serverErrors).toEqual([])
})

test('H5-HOME-002 H5 热门城市按站点展示后端上架商品', async ({ page }) => {
  await seedH5Site(page)
  await page.goto('/')
  await expect(page.locator('uni-page-body')).toBeVisible()
  const responsePromise = page.waitForResponse(response => {
    if (!response.url().includes('/queryGoodsList')) return false
    const body = response.request().postDataJSON()
    return body && (body.deptId === 210 || body.categoryId === 210)
  })
  await page.getByText('广州', { exact: true }).click()
  const response = await responsePromise
  const requestBody = response.request().postDataJSON()
  const responseBody = await response.json()
  expect(requestBody.deptId).toBe(210)
  expect(responseBody.code).toBe(200)
  expect(responseBody.data.length).toBeGreaterThan(0)
  await expect(page.locator('.product-card').first(),
    '广州站点应至少展示一个上架商品').toBeVisible()
})

test('H5-SITE-001 H5 使用固定测试站点且不触发微信定位授权', async ({ page }) => {
  const consoleErrors = []
  page.on('console', message => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })

  await seedH5Site(page)
  await page.goto('/')
  await expect(page.locator('uni-page-body')).toBeVisible()
  expect(consoleErrors,
    'H5 辅助测试使用固定站点时不应触发微信定位 API').toEqual([])
})
