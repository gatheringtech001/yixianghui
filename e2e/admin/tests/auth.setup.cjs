const fs = require('node:fs/promises')
const path = require('node:path')
const { test: setup, expect } = require('@playwright/test')

const authFile = path.resolve(__dirname, '../.auth/admin.json')
const evidenceRoot = process.env.E2E_EVIDENCE_ROOT || path.resolve(
  __dirname,
  '../../../outputs/ui-admin-e2e'
)

function requireEnvironment(name) {
  const value = process.env[name]
  if (!value) throw new Error(`Missing required environment variable: ${name}`)
  return value
}

setup('ADM-AUTH-004 管理员可登录并取得动态菜单', async ({ page }) => {
  await fs.mkdir(path.dirname(authFile), { recursive: true })
  await fs.mkdir(path.join(evidenceRoot, 'screenshots'), { recursive: true })

  await page.goto('/login')
  await expect(page.getByRole('heading', { name: '智享居后台管理系统' })).toBeVisible()
  await page.getByPlaceholder('账号').fill(requireEnvironment('E2E_ADMIN_USER'))
  await page.getByPlaceholder('密码').fill(requireEnvironment('E2E_ADMIN_PASSWORD'))
  await page.screenshot({
    path: path.join(evidenceRoot, 'screenshots/login.png'),
    fullPage: true
  })

  await page.getByRole('button', { name: '登 录', exact: true }).click()
  await page.waitForURL(url => !url.pathname.startsWith('/login'))
  await expect(page.locator('.app-main')).toBeVisible()
  await expect(page.locator('.tags-view-item.active')).toContainText('首页')
  await page.screenshot({
    path: path.join(evidenceRoot, 'screenshots/dashboard.png'),
    fullPage: true
  })
  await page.context().storageState({ path: authFile })
})
