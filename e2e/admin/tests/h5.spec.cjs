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

test('H5-HOME-002 H5 热门城市按分类展示后端上架商品', async ({ page }) => {
  await seedH5Site(page)
  await page.goto('/')
  await expect(page.locator('uni-page-body')).toBeVisible()
  const responsePromise = page.waitForResponse(response => {
    if (!response.url().includes('/queryGoodsList')) return false
    const body = response.request().postDataJSON()
    return body && body.categoryId === 38
  })
  await page.getByText('昆明', { exact: true }).click()
  const response = await responsePromise
  const requestBody = response.request().postDataJSON()
  const responseBody = await response.json()
  expect(requestBody.categoryId).toBe(38)
  expect(requestBody.deptId).toBeUndefined()
  expect(responseBody.code).toBe(200)
  expect(responseBody.data.length).toBeGreaterThan(0)
  await expect(page.locator('.product-card').first(),
    '昆明分类应至少展示一个上架商品').toBeVisible()
})

test('H5-HOME-003 腾冲空链接按城市名映射分类且不提示站点错误', async ({ page }) => {
  await seedH5Site(page)
  await page.goto('/')
  await expect(page.locator('uni-page-body')).toBeVisible()
  const responsePromise = page.waitForResponse(response => {
    if (!response.url().includes('/queryGoodsList')) return false
    const body = response.request().postDataJSON()
    return body && body.categoryId === 28
  })
  await page.getByText('腾冲', { exact: true }).click()
  const response = await responsePromise
  const requestBody = response.request().postDataJSON()
  expect(requestBody.categoryId).toBe(28)
  expect(requestBody.deptId).toBeUndefined()
  await expect(page.getByText('未配置站点，请在广告链接填写站点ID')).toHaveCount(0)
})

test('H5-HOME-004 首页入口图片铺满且文字标签保持高对比', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await seedH5Site(page)
  await page.goto('/')
  await expect(page.locator('.entry-card')).toHaveCount(3)

  const cards = await page.locator('.entry-card').evaluateAll(nodes => nodes.map(card => {
    const background = card.querySelector('.entry-bg')
    const copy = card.querySelector('.entry-copy')
    const title = card.querySelector('.entry-title')
    const description = card.querySelector('.entry-desc')
    const cardRect = card.getBoundingClientRect()
    const backgroundRect = background.getBoundingClientRect()
    const titleStyle = getComputedStyle(title)
    const descriptionStyle = getComputedStyle(description)
    return {
      backgroundWidthDelta: Math.abs(cardRect.width - backgroundRect.width),
      backgroundHeightDelta: Math.abs(cardRect.height - backgroundRect.height),
      copyBackground: getComputedStyle(copy).backgroundColor,
      titleColor: titleStyle.color,
      titleLines: title.getBoundingClientRect().height / parseFloat(titleStyle.lineHeight),
      descriptionColor: descriptionStyle.color,
      descriptionLines: description.getBoundingClientRect().height /
        parseFloat(descriptionStyle.lineHeight)
    }
  }))

  for (const card of cards) {
    expect(card.backgroundWidthDelta).toBeLessThanOrEqual(2)
    expect(card.backgroundHeightDelta).toBeLessThanOrEqual(2)
    expect(card.copyBackground).toBe('rgba(17, 17, 17, 0.72)')
    expect(card.titleColor).toBe('rgb(255, 255, 255)')
    expect(card.descriptionColor).toBe('rgba(255, 255, 255, 0.88)')
    expect(card.titleLines).toBeLessThanOrEqual(1.1)
    expect(card.descriptionLines).toBeLessThanOrEqual(1.1)
  }
})

test('H5-ASSET-001 H5 热门城市图片可由本地后端访问', async ({ page, request }) => {
  const cardsResponse = await request.get(
    'http://127.0.0.1:18080/api/mnp/index/get_ad_content_list?positionId=6'
  )
  const cardsBody = await cardsResponse.json()
  const cityImagePaths = new Set((cardsBody.data || []).map(item => item.adImage))
  const imageResponses = await Promise.all([...cityImagePaths].map(async pathname => {
    const response = await request.get(`http://127.0.0.1:18080/api${pathname}`)
    return {
      pathname,
      status: response.status(),
      contentType: response.headers()['content-type'] || ''
    }
  }))
  expect(imageResponses).toEqual([...cityImagePaths].map(pathname => ({
    pathname,
    status: 200,
    contentType: expect.stringMatching(/^image\//)
  })))

  await seedH5Site(page)
  await page.goto('/')
  await expect(page.locator('.city-image')).toHaveCount(cityImagePaths.size)
  await expect.poll(() => page.locator('.city-image > div').evaluateAll(nodes => (
    nodes.every(node => getComputedStyle(node).backgroundImage !== 'none')
  ))).toBe(true)
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
