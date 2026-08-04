const fs = require('node:fs/promises')
const path = require('node:path')
const { test, expect } = require('@playwright/test')

const evidenceRoot = process.env.E2E_EVIDENCE_ROOT || path.resolve(
  __dirname,
  '../../../outputs/ui-admin-e2e'
)

async function saveScreenshot(page, name) {
  const screenshotsDir = path.join(evidenceRoot, 'screenshots')
  await fs.mkdir(screenshotsDir, { recursive: true })
  await page.screenshot({ path: path.join(screenshotsDir, name), fullPage: true })
}

async function openListPage(page, route, apiPath) {
  const responsePromise = page.waitForResponse(response => (
    response.url().includes(apiPath) && response.request().method() === 'GET'
  ))
  await page.goto(route)
  const response = await responsePromise
  expect(response.status()).toBe(200)
  const body = await response.json()
  expect(body.code).toBe(200)
  await expect(page.locator('.app-main')).toBeVisible()
  return body
}

test('ADM-CORE-001 首页仪表盘可见且无登录回退', async ({ page }) => {
  await page.goto('/index')
  await expect(page.locator('.app-main')).toBeVisible()
  await expect(page).not.toHaveURL(/\/login/)
  await expect(page.locator('.tags-view-item.active')).toContainText('首页')
  await saveScreenshot(page, 'ADM-CORE-001-dashboard.png')
})

test('ADM-CORE-002 管理端代理可访问本地后端', async ({ page }) => {
  await page.goto('/index')
  const result = await page.evaluate(async () => {
    const response = await fetch('/dev-api/captchaImage')
    return { status: response.status, body: await response.json() }
  })
  expect(result.status).toBe(200)
  expect(result.body.code).toBe(200)
  expect(result.body.captchaEnabled).toBe(false)
})

test('ADM-GOODS-001 商品列表显示本地已上架商品', async ({ page }) => {
  const body = await openListPage(page, '/goods/app_goods', '/system/app_goods/list')
  expect(body.total).toBeGreaterThan(0)
  expect(body.rows.some(goods => goods.status === '1')).toBe(true)
  await expect(page.locator('.el-table__body-wrapper tbody tr')).not.toHaveCount(0)
  await expect(
    page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: '酒店预定' })
  ).not.toHaveCount(0)
  await saveScreenshot(page, 'ADM-GOODS-001-list.png')
})

test('ADM-GOODS-002 商品快捷搜索可筛选酒店预定', async ({ page }) => {
  await openListPage(page, '/goods/app_goods', '/system/app_goods/list')
  const responsePromise = page.waitForResponse(response => (
    response.url().includes('/system/app_goods/list') &&
    response.request().method() === 'GET'
  ))
  const search = page.getByPlaceholder('商品名称 / 标签 / 简介')
  await search.fill('酒店预定')
  await search.press('Enter')
  const response = await responsePromise
  const body = await response.json()
  expect(body.code).toBe(200)
  expect(body.rows.length).toBeGreaterThan(0)
  expect(body.rows.every(goods => goods.goodsName.includes('酒店预定'))).toBe(true)
  await expect(
    page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: '酒店预定' })
  ).not.toHaveCount(0)
})

test('ADM-GOODS-003 新增商品表单字段完整但不提交', async ({ page }) => {
  await openListPage(page, '/goods/app_goods', '/system/app_goods/list')
  const addButton = page.locator('.app-main button').filter({ hasText: '新增' })
  await expect(addButton).toHaveCount(1)
  await addButton.click()
  const dialog = page.locator('.el-dialog:visible')
  await expect(dialog).toContainText('添加商品')
  await expect(dialog.getByPlaceholder('请输入商品名称')).toBeVisible()
  const skuFormItem = dialog.locator('.el-form-item').filter({ hasText: '是否多规格' })
  await expect(skuFormItem).toHaveCount(1)
  const singleSkuOption = skuFormItem.getByText('否', { exact: true })
  await expect(singleSkuOption).toHaveCount(1)
  await singleSkuOption.click()
  await expect(dialog.getByPlaceholder('请输入价格')).toBeVisible()
  await expect(dialog.getByPlaceholder('请输入库存')).toBeVisible()
  await saveScreenshot(page, 'ADM-GOODS-003-add-form.png')
  await dialog.getByRole('button', { name: '取 消', exact: true }).click()
  await expect(dialog).toBeHidden()
})

test('ADM-ORDER-001 商品订单列表接口和页面联通', async ({ page }) => {
  const body = await openListPage(
    page,
    '/order/app_goods_order',
    '/system/app_goods_order/list'
  )
  expect(Array.isArray(body.rows)).toBe(true)
  await expect(page.locator('.el-table')).toBeVisible()
  await saveScreenshot(page, 'ADM-ORDER-001-list.png')
})

test('ADM-CUSTOMER-001 客户资料列表接口和页面联通', async ({ page }) => {
  const body = await openListPage(
    page,
    '/customer/app_customer',
    '/system/app_customer/list'
  )
  expect(Array.isArray(body.rows)).toBe(true)
  await expect(page.locator('.el-table')).toBeVisible()
})
