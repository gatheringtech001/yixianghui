const path = require('node:path')
const { defineConfig, devices } = require('@playwright/test')

const evidenceRoot = process.env.E2E_EVIDENCE_ROOT || path.resolve(
  __dirname,
  '../../outputs/ui-admin-e2e'
)
const authFile = path.join(__dirname, '.auth/admin.json')

module.exports = defineConfig({
  testDir: './tests',
  timeout: 45000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  outputDir: process.env.PLAYWRIGHT_OUTPUT_DIR || path.join(evidenceRoot, 'test-results'),
  reporter: [
    ['list'],
    ['html', {
      outputFolder: process.env.PLAYWRIGHT_HTML_REPORT || path.join(evidenceRoot, 'playwright-report'),
      open: 'never'
    }],
    ['json', {
      outputFile: process.env.PLAYWRIGHT_JSON_OUTPUT_NAME || path.join(evidenceRoot, 'playwright-results.json')
    }]
  ],
  use: {
    baseURL: process.env.ADMIN_BASE_URL || 'http://127.0.0.1:8081',
    headless: true,
    viewport: { width: 1440, height: 900 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'setup',
      testMatch: /auth\.setup\.cjs/
    },
    {
      name: 'public',
      testMatch: /public\.spec\.cjs/
    },
    {
      name: 'h5',
      testMatch: /h5\.spec\.cjs/,
      use: {
        ...devices['Desktop Chrome'],
        baseURL: process.env.H5_BASE_URL || 'http://127.0.0.1:8080'
      }
    },
    {
      name: 'chromium',
      testIgnore: [/auth\.setup\.cjs/, /public\.spec\.cjs/, /h5\.spec\.cjs/],
      use: {
        ...devices['Desktop Chrome'],
        storageState: authFile
      },
      dependencies: ['setup']
    }
  ]
})
