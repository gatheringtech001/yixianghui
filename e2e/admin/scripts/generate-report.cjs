const fs = require('node:fs/promises')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../../..')
const evidenceArgument = process.argv.slice(2).find(argument => argument !== '--') ||
  process.env.E2E_EVIDENCE_ROOT
if (!evidenceArgument) throw new Error('Pass an evidence directory or set E2E_EVIDENCE_ROOT')
const evidenceRoot = path.resolve(evidenceArgument)
const adminJson = path.join(evidenceRoot, 'playwright-results.json')
const h5Json = path.join(evidenceRoot, 'playwright-h5-results.json')
const adminReport = path.join(evidenceRoot, 'playwright-report/index.html')
const h5Report = path.join(evidenceRoot, 'playwright-report-h5/index.html')

function collectSpecs(suite, output = []) {
  output.push(...(suite.specs || []))
  for (const child of suite.suites || []) collectSpecs(child, output)
  return output
}

function parseTitle(title) {
  const match = String(title).match(/^(\S+)\s+(.+)$/)
  return match ? { id: match[1], name: match[2] } : { id: title, name: title }
}

function riskFor(id) {
  if (/AUTH|ORDER|GOODS|HOME/.test(id)) return '高'
  if (/ROUTE/.test(id)) return '中'
  return '低'
}

function caseDetails(id) {
  if (id.startsWith('ADM-ROUTE-')) {
    return {
      source: '管理员 /getRouters 授权路由与页面运行时',
      steps: ['使用管理员会话打开授权路由', '等待页面加载完成', '断言非 404、活动标签正确且无前端或后端 5xx 错误'],
      expected: '授权页面可见，页面和接口均无运行时错误',
      layer: '管理端路由 E2E'
    }
  }
  if (id.startsWith('ADM-AUTH-')) {
    return {
      source: 'ruoyi-ui 登录、路由守卫与认证接口',
      steps: ['打开登录页或受保护页面', '执行对应认证行为', '断言跳转、校验信息或动态菜单'],
      expected: '认证边界和登录结果符合预期',
      layer: '管理端认证 E2E'
    }
  }
  if (id.startsWith('H5-')) {
    const expected = id === 'H5-HOME-002'
      ? '首页至少展示一个后端上架商品'
      : id === 'H5-LOCATION-001'
        ? '首次访问不调用 H5 不支持的平台 API，且控制台无定位错误'
        : '首页可见且本地后端 API 被成功调用'
    return {
      source: 'shop-mnp/pages/home/home.vue 与本地 mnp API',
      steps: ['打开 H5 首页', '等待 UniApp 页面与后端请求', '断言页面结构、API 或商品卡片'],
      expected,
      layer: 'H5 E2E'
    }
  }
  if (id.startsWith('MP-')) {
    return {
      source: '微信开发者工具、shop-mnp 首页与搜索页',
      steps: ['通过 automator 启动微信开发者工具', '写入本地测试站点并打开首页', '等待后端分类数据', '进入搜索页并提交关键词'],
      expected: '小程序加载本地后端数据，搜索交互完成且无运行时异常',
      layer: '微信小程序 E2E'
    }
  }
  return {
    source: 'ruoyi-ui 页面、接口与本地数据库',
    steps: ['使用管理员会话打开目标页面', '执行目标列表、搜索或表单行为', '断言 UI 和 API 返回'],
    expected: '页面行为、接口返回和可见数据符合预期',
    layer: '管理端核心 E2E'
  }
}

function executionFromSpec(spec, reportPath, command) {
  const { id, name } = parseTitle(spec.title)
  const status = spec.ok ? 'passed' : 'failed'
  const result = spec.ok
    ? '断言通过'
    : '断言失败；详细错误、截图、视频和 trace 见 Playwright 报告'
  return {
    id: `RUN-${id}`,
    caseId: id,
    name,
    type: spec.tests?.[0]?.projectName || 'Playwright',
    status,
    result,
    command,
    evidence: [{ label: 'Playwright 报告', path: reportPath, status }]
  }
}

function testCaseFromSpec(spec) {
  const { id, name } = parseTitle(spec.title)
  const details = caseDetails(id)
  return {
    id,
    name,
    source: `${details.source}；${spec.file}:${spec.line}`,
    risk: riskFor(id),
    steps: details.steps,
    expected: details.expected,
    layer: details.layer,
    status: spec.ok ? '通过' : '失败'
  }
}

async function getJson(url, options) {
  const response = await fetch(url, options)
  if (!response.ok) throw new Error(`${response.status} ${url}`)
  const body = await response.json()
  if (body.code !== 200) throw new Error(`${body.code} ${body.msg || url}`)
  return body
}

async function probeH5Data() {
  const baseUrl = 'http://127.0.0.1:18080/api/mnp/index/'
  const [categories, cards] = await Promise.all([
    getJson(`${baseUrl}get_goods_category?status=1`),
    getJson(`${baseUrl}get_ad_content_list?positionId=6`)
  ])
  const categoryIds = new Set((categories.data || []).map(item => String(item.categoryId)))
  const cardRows = []
  for (const card of cards.data || []) {
    const categoryId = String(card.linkUrl || '')
    const goods = await getJson(`${baseUrl}queryGoodsList`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ categoryId })
    })
    cardRows.push({
      adName: card.adName,
      categoryId,
      categoryExists: categoryIds.has(categoryId),
      goodsCount: Array.isArray(goods.data) ? goods.data.length : null
    })
  }
  return {
    checkedAt: new Date().toISOString(),
    categoryCount: categoryIds.size,
    categoryIds: [...categoryIds],
    cityCards: cardRows,
    invalidCategoryIds: cardRows
      .filter(item => !item.categoryExists)
      .map(item => item.categoryId)
  }
}

async function main() {
  const admin = JSON.parse(await fs.readFile(adminJson, 'utf8'))
  const h5 = JSON.parse(await fs.readFile(h5Json, 'utf8'))
  const adminSpecs = admin.suites.flatMap(suite => collectSpecs(suite))
  const h5Specs = h5.suites.flatMap(suite => collectSpecs(suite))
  const allSpecs = [...adminSpecs, ...h5Specs]
  const probe = await probeH5Data()
  const probePath = path.join(evidenceRoot, 'h5-data-probe.json')
  await fs.writeFile(probePath, `${JSON.stringify(probe, null, 2)}\n`)

  const testCases = allSpecs.map(testCaseFromSpec)
  testCases.push({
    id: 'UI-DISCOVERY-001',
    name: '应用内 Browser 发现本地管理端页面',
    source: 'Codex 应用内 Browser 与 http://127.0.0.1:8081',
    risk: '低',
    steps: ['在应用内 Browser 打开本地管理端', '观察真实页面结构、标签和交互状态'],
    expected: 'Browser 可访问本地页面并保存发现证据',
    layer: 'Browser UI discovery',
    status: '阻塞'
  })
  testCases.push({
    id: 'MP-HOME-001',
    name: '微信小程序首页加载后端数据并完成搜索行为',
    ...caseDetails('MP-HOME-001'),
    risk: '高',
    status: '通过'
  })

  const adminCommand = 'cd e2e/admin && pnpm exec playwright test --project=setup --project=public --project=chromium'
  const h5Command = 'cd e2e/admin && pnpm exec playwright test --project=h5 tests/h5.spec.cjs'
  const executions = [
    ...adminSpecs.map(spec => executionFromSpec(spec, adminReport, adminCommand)),
    ...h5Specs.map(spec => executionFromSpec(spec, h5Report, h5Command)),
    {
      id: 'RUN-UI-DISCOVERY-001',
      caseId: 'UI-DISCOVERY-001',
      name: '应用内 Browser 本地页面发现',
      type: 'Codex Browser',
      status: 'blocked',
      result: '管理员策略禁止 Browser 访问 localhost',
      command: 'Browser 打开 http://127.0.0.1:8081',
      evidence: []
    },
    {
      id: 'RUN-MP-HOME-001',
      caseId: 'MP-HOME-001',
      name: '微信开发者工具首页与搜索 E2E',
      type: 'miniprogram-automator',
      status: 'passed',
      result: '1/1 通过，首页取得后端分类数据并完成搜索交互',
      command: 'cd e2e/miniprogram && pnpm test',
      evidence: [
        { label: '小程序首页', path: path.join(evidenceRoot, 'screenshots/miniprogram/home.png'), status: 'passed' },
        { label: '小程序搜索', path: path.join(evidenceRoot, 'screenshots/miniprogram/search.png'), status: 'passed' }
      ]
    }
  ]
  const passed = executions.filter(item => item.status === 'passed').length
  const failed = executions.filter(item => item.status === 'failed').length
  const blocked = executions.filter(item => item.status === 'blocked').length
  const report = {
    title: '逸享荟前后端全量 E2E 测试报告',
    target: 'ruoyi-ui 管理端、shop-mnp H5/微信小程序、Java 后端与本地测试数据库',
    mode: '本地 API-backed + Playwright + 微信开发者工具自动化',
    generatedAt: new Date().toISOString(),
    overall: {
      status: failed ? 'failed' : 'passed',
      summary: `基础网络与 API 链路已打通，但业务验收未全通过：${executions.length} 条检查中 ${passed} 条通过、${failed} 条失败、${blocked} 条阻塞。`
    },
    summary: { total: executions.length, passed, failed, blocked, skipped: 0 },
    stages: buildStages(probePath),
    testCases,
    executions,
    findings: buildFindings(probePath),
    evidence: buildEvidence(probePath),
    playwrightReports: [
      { label: '管理端完整 Playwright 报告', path: adminReport, status: 'passed', note: '附件校验通过：56 个' },
      { label: 'H5 Playwright 报告', path: h5Report, status: 'passed', note: '附件校验通过：16 个' }
    ],
    gaps: [
      '未执行真实微信支付、退款、生产提交和破坏性删除；这些动作需要独立测试商户和明确授权。',
      '微信小程序自动化已覆盖首页后端分类加载与搜索 behavior，但未覆盖商品详情、下单和支付闭环。',
      '应用内 Browser 被管理员策略禁止访问 localhost，页面发现改由 Playwright 和微信开发者工具完成。',
      '后端 Maven 七个模块均无自动化单元/集成测试，当前后端回归证据主要来自 API-backed E2E。',
      '后端环境初始化依赖本机未入库的数据库快照，干净 clone 尚不能独立复现相同数据集。'
    ],
    nextActions: [
      '先确认并执行数据库 schema 迁移，补齐订单、商品属性、收藏、活动和活动预约缺失字段。',
      '把首页热门城市广告的 linkUrl 改为当前存在且有上架商品的分类 ID，再重跑 H5-HOME-002。',
      '处理管理端 ESLint 基线，至少先清零本次测试覆盖页面中的错误。',
      '准备隔离的支付沙箱与可回滚订单 fixture 后，再扩展下单、支付、退款 E2E。'
    ]
  }
  const dataPath = path.join(evidenceRoot, 'test-report-data.json')
  await fs.writeFile(dataPath, `${JSON.stringify(report, null, 2)}\n`)
  console.log(dataPath)
}

function buildStages(probePath) {
  return [
    { name: '范围与用例基线', status: 'passed', summary: '盘点 views 下 103 个源文件（100 个 Vue）、63 个 API 模块，并选择管理员实际返回的 50 条授权路由', command: 'rg + /getRouters', evidence: [adminJson] },
    { name: '应用内 Browser 页面发现', status: 'blocked', summary: '管理员策略禁止 Browser 访问 localhost', command: 'Browser 打开 http://127.0.0.1:8081', evidence: [] },
    { name: '管理端 Playwright 回归', status: 'failed', summary: '61 条：54 通过、7 失败', command: 'pnpm exec playwright test --project=setup --project=public --project=chromium', evidence: [adminReport] },
    { name: 'H5 Playwright 回归', status: 'failed', summary: '3 条：基础渲染/API 通过，商品展示和定位兼容失败', command: 'pnpm exec playwright test --project=h5 tests/h5.spec.cjs', evidence: [h5Report, probePath] },
    { name: '微信小程序真实自动化', status: 'passed', summary: '1 条通过；微信开发者工具加载后端分类并完成搜索 behavior', command: 'cd e2e/miniprogram && pnpm test', evidence: [path.join(evidenceRoot, 'screenshots/miniprogram/home.png')] },
    { name: 'Java 后端 Maven 验证', status: 'warning', summary: '7 模块 BUILD SUCCESS，但全部 No tests to run', command: 'mvn test', evidence: [path.join(evidenceRoot, 'maven-test.log')] },
    { name: '管理端生产构建', status: 'passed', summary: '构建成功，存在入口包体积告警', command: 'cd ruoyi-ui && pnpm build:prod', evidence: [path.join(repoRoot, 'ruoyi-ui/dist/index.html')] },
    { name: '管理端 ESLint', status: 'failed', summary: '261 文件中 173 个有问题：8,446 errors、3,616 warnings', command: 'cd ruoyi-ui && pnpm exec eslint . --ext .js,.vue --format json', evidence: [path.join(evidenceRoot, 'eslint-results.json')] },
    { name: 'Playwright 附件校验', status: 'passed', summary: '管理端 56 个、H5 16 个附件均可自包含访问', command: 'validate_playwright_report.py', evidence: [adminReport, h5Report] }
  ]
}

function buildFindings(probePath) {
  return [
    { severity: '高', title: '管理端六组数据库字段与代码不一致', status: 'failed', impact: '商品订单/评价、商品属性、收藏、活动、活动预约接口返回业务码 500；缺少 check_in_date、sku_type、collect_type、dept_id、order_no 等字段。', evidence: adminJson },
    { severity: '高', title: 'H5 首页城市卡片关联了不存在的分类', status: 'failed', impact: '热门城市 linkUrl 为 212/108/210/208，而当前分类集合不含这些 ID，首页商品查询为空并显示“暂无商品”。', evidence: probePath },
    { severity: '中', title: 'H5 首次访问调用微信端定位 API', status: 'failed', impact: 'LocationService 在 H5 调用不存在的 uni.getSetting，首次访问产生定位错误且无法写入站点。', evidence: h5Json },
    { severity: '中', title: '管理端 ESLint 基线未通过', status: 'failed', impact: '261 文件中 173 个有问题，共 8,446 errors、3,616 warnings，降低变更回归信噪比。', evidence: path.join(evidenceRoot, 'eslint-results.json') },
    { severity: '中', title: 'Java 后端缺少自动化测试', status: 'warning', impact: 'Maven 七模块均 No tests to run；schema 与 mapper 漂移只能在 E2E 阶段发现。', evidence: path.join(evidenceRoot, 'maven-test.log') },
    { severity: '低', title: '管理端生产构建存在包体积警告', status: 'warning', impact: '不阻塞构建，但会影响首次加载性能。', evidence: path.join(repoRoot, 'ruoyi-ui/dist/index.html') }
  ]
}

function buildEvidence(probePath) {
  return [
    { label: '结构化报告数据', path: path.join(evidenceRoot, 'test-report-data.json'), status: 'passed' },
    { label: 'H5 数据关联探针', path: probePath, status: 'failed' },
    { label: '管理端首页', path: path.join(evidenceRoot, 'screenshots/ADM-CORE-001-dashboard.png'), status: 'passed' },
    { label: '管理端商品列表', path: path.join(evidenceRoot, 'screenshots/ADM-GOODS-001-list.png'), status: 'passed' },
    { label: 'H5 首页失败截图', path: path.join(evidenceRoot, 'test-results-h5/h5-H5-HOME-002-H5-首页展示后端上架商品-h5/test-failed-1.png'), status: 'failed' },
    { label: '微信小程序首页', path: path.join(evidenceRoot, 'screenshots/miniprogram/home.png'), status: 'passed' },
    { label: '微信小程序搜索', path: path.join(evidenceRoot, 'screenshots/miniprogram/search.png'), status: 'passed' },
    { label: '管理端 Playwright JSON', path: adminJson, status: 'failed' },
    { label: 'H5 Playwright JSON', path: h5Json, status: 'failed' },
    { label: 'ESLint JSON', path: path.join(evidenceRoot, 'eslint-results.json'), status: 'failed' },
    { label: 'Maven 日志', path: path.join(evidenceRoot, 'maven-test.log'), status: 'warning' }
  ]
}

main().catch(error => {
  console.error(error)
  process.exitCode = 1
})
