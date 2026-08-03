const fs = require('node:fs/promises')
const path = require('node:path')
const { test, expect } = require('@playwright/test')
const { activeRoutes, unavailableRoutes } = require('./routes.cjs')

const evidenceRoot = process.env.E2E_EVIDENCE_ROOT || path.resolve(
  __dirname,
  '../../../outputs/ui-admin-e2e'
)
const screenshotRoutes = new Set([
  '/customer/app_customer',
  '/goods/app_goods',
  '/article/app_article',
  '/system/user',
  '/monitor/server'
])

function observeRuntime(page) {
  const pageErrors = []
  const serverErrors = []
  const pendingResponses = []
  page.on('pageerror', error => pageErrors.push(error.message))
  page.on('response', response => {
    if (!response.url().includes('/dev-api/')) return
    pendingResponses.push((async () => {
      if (response.status() >= 500) {
        serverErrors.push(`HTTP ${response.status()} ${response.url()}`)
        return
      }
      const contentType = response.headers()['content-type'] || ''
      if (!contentType.includes('application/json')) return
      const body = await response.json().catch(() => null)
      if (body && Number(body.code) >= 500) {
        serverErrors.push(`业务码 ${body.code} ${response.url()}: ${body.msg || ''}`)
      }
    })())
  })
  return {
    pageErrors,
    serverErrors,
    async settle() {
      await Promise.allSettled(pendingResponses)
    }
  }
}

for (const [index, [route, title]] of activeRoutes.entries()) {
  const id = `ADM-ROUTE-${String(index + 1).padStart(3, '0')}`
  test(`${id} ${title}页面可通过授权路由打开`, async ({ page }, testInfo) => {
    const runtime = observeRuntime(page)
    await page.goto(route)
    await expect(page).not.toHaveURL(/\/404(?:$|\?)/)
    await expect(page.locator('.app-main')).toBeVisible()
    await expect(page.locator('.tags-view-item.active')).toContainText(title)

    const loadingMask = page.locator('.app-main .el-loading-mask')
    if (await loadingMask.count()) {
      await loadingMask.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})
    }
    await runtime.settle()

    if (screenshotRoutes.has(route)) {
      const screenshotsDir = path.join(evidenceRoot, 'screenshots')
      await fs.mkdir(screenshotsDir, { recursive: true })
      await page.screenshot({
        path: path.join(screenshotsDir, `${id}-${title}.png`),
        fullPage: true
      })
    }

    await testInfo.attach('runtime-observation.json', {
      body: JSON.stringify({
        pageErrors: runtime.pageErrors,
        serverErrors: runtime.serverErrors
      }, null, 2),
      contentType: 'application/json'
    })
    expect(runtime.pageErrors).toEqual([])
    expect(runtime.serverErrors).toEqual([])
  })
}

for (const [index, [route, title, component]] of unavailableRoutes.entries()) {
  const id = `ADM-UNAVAILABLE-${String(index + 1).padStart(3, '0')}`
  test(`${id} ${title}未开放入口不会下发`, async ({ page }) => {
    await page.goto('/index')
    const routerResult = await page.evaluate(async targetComponent => {
      const token = document.cookie
        .split('; ')
        .find(item => item.startsWith('Admin-Token='))
        ?.split('=')
        .slice(1)
        .join('=')
      const response = await fetch('/dev-api/getRouters', {
        headers: { Authorization: `Bearer ${decodeURIComponent(token || '')}` }
      })
      const body = await response.json()
      const findComponent = items => (items || []).some(item => (
        item.component === targetComponent || findComponent(item.children)
      ))
      return { code: body.code, found: findComponent(body.data) }
    }, component)
    expect(routerResult.code).toBe(200)
    expect(routerResult.found).toBe(false)
    await expect(
      page.locator('.sidebar-container').getByText(title, { exact: true })
    ).toHaveCount(0)

    await page.goto(route)
    await expect(page).toHaveURL(/\/404(?:$|\?)/)
  })
}
