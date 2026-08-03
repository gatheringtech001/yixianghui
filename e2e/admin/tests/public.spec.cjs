const { test, expect } = require('@playwright/test')

test.describe('管理端公开访问与认证边界', () => {
  test('ADM-AUTH-001 未登录访问业务页会跳转登录', async ({ page }) => {
    await page.goto('/goods/app_goods')
    await expect(page).toHaveURL(/\/login\?redirect=/)
    await expect(page.getByRole('heading', { name: '智享居后台管理系统' })).toBeVisible()
  })

  test('ADM-AUTH-002 登录表单阻止空账号和空密码', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('账号').fill('')
    await page.getByPlaceholder('密码').fill('')
    await page.getByRole('button', { name: '登 录', exact: true }).click()
    await expect(page.getByText('请输入您的账号', { exact: true })).toBeVisible()
    await expect(page.getByText('请输入您的密码', { exact: true })).toBeVisible()
  })

  test('ADM-AUTH-003 错误密码显示错误且不进入系统', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('账号').fill('admin')
    await page.getByPlaceholder('密码').fill('E2E_INVALID_PASSWORD')
    await page.getByRole('button', { name: '登 录', exact: true }).click()
    await expect(page.locator('.el-message--error')).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
  })
})
