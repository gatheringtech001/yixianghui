const fs = require('node:fs/promises')
const path = require('node:path')
const { test, expect } = require('@playwright/test')

const evidenceRoot = process.env.E2E_EVIDENCE_ROOT || path.resolve(
  __dirname,
  '../../../outputs/ui-admin-e2e'
)

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

test('H5-HOME-002 H5 首页展示后端上架商品', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('uni-page-body')).toBeVisible()
  await expect(page.locator('.product-card').first(),
    '首页城市卡片关联的分类应至少返回一个上架商品').toBeVisible()
})

test('H5-LOCATION-001 H5 首次访问无平台 API 兼容错误', async ({ page }) => {
  const consoleErrors = []
  page.on('console', message => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })

  await page.goto('/')
  await expect(page.locator('uni-page-body')).toBeVisible()
  expect(consoleErrors,
    'H5 首次访问不应调用仅微信小程序支持的定位 API').toEqual([])
})
