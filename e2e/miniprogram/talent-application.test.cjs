const test = require('node:test')
const assert = require('node:assert/strict')
const loadPage = require('./vue-page-harness.cjs')
const fs = require('node:fs')
const path = require('node:path')

function setup(options = {}, file = 'packagesMember/retail/apply/index.vue') {
  const calls = { apply: [], navigate: [], cache: [] }
  const result = loadPage(file, {
    AuthProfilePopup: {}, bindPageAuthPopup() {},
    isAuthorizedUser: () => options.authorized !== false,
    runWithAuth: (_page, done) => done(options.authorized !== false),
    syncConsultantStorage: value => calls.cache.push(value),
    getInfo: async () => {
      if (options.infoError) throw new Error('状态获取失败')
      return { code: 200, consultant: options.consultant }
    },
    applyConsultant: async payload => {
      calls.apply.push(payload)
      if (options.applyError) throw new Error('申请暂时不可用')
      if (options.waitApply) await options.waitApply
      return { code: 200 }
    },
    uni: {
      redirectTo: args => {
        calls.navigate.push(args.url)
        if (options.navigationError) args.fail?.({ errMsg: '跳转失败' })
        else args.success?.()
      },
      navigateTo() {}, getStorageSync: () => null, showToast() {}
    }
  })
  return { ...result, calls }
}

test('application requires explicit consent and valid name and mobile', async () => {
  const { page, calls, source } = setup()
  page.form = { name: '张三', mobile: '13800000000' }
  await page.submit()
  assert.match(page.error, /同意/)
  page.checked = true
  page.form.name = ' '
  await page.submit()
  assert.match(page.error, /姓名/)
  page.form.name = '张三'
  page.form.mobile = '123'
  await page.submit()
  assert.match(page.error, /手机号/)
  assert.equal(calls.apply.length, 0)
  assert.match(source, /旅居达人/)
  assert.match(source, /康养顾问/)
  assert.match(source, /隐私政策/)
  assert.doesNotMatch(source, /提交后自动审核|自动审核通过|请勿重复提交/)
  assert.match(source, /class="consent-check"/)
  assert.match(source, /v-if="checked" name="checkmark"/)
  assert.doesNotMatch(source, /: 'checkmark-circle'/)
})

test('recruitment redesign uses a lightweight local photo and independently readable terms', () => {
  const { page, source } = setup()
  assert.equal(page.showTerms, false)
  page.toggleTerms()
  assert.equal(page.showTerms, true)
  assert.equal(page.checked, false)
  page.toggleTerms()
  assert.equal(page.showTerms, false)
  assert.match(source, /class="hero-image"/)
  assert.match(source, /packagesMember\/static\/talent-join-hero\.jpg/)
  assert.doesNotMatch(source, /class="join-icon"|class="eyebrow"/)
  assert.equal((source.match(/class="term"/g) || []).length, 5)
  assert.match(source, /@click\.stop="toggleTerms"/)
  assert.match(source, /@click\.stop="openPrivacy"/)
  const asset = path.resolve(__dirname, '../../shop-mnp/packagesMember/static/talent-join-hero.jpg')
  assert.ok(fs.statSync(asset).size < 120000)
  if (process.env.MINIPROGRAM_PROJECT_PATH) {
    const compiledAsset = path.join(process.env.MINIPROGRAM_PROJECT_PATH, 'packagesMember/static/talent-join-hero.jpg')
    assert.ok(fs.readFileSync(asset).equals(fs.readFileSync(compiledAsset)))
  }
})

test('submission trims input, records consent and waits for server approval before registration', async () => {
  const { page, calls } = setup({ consultant: { consultantId: 9, status: '01' } })
  page.checked = true
  page.form = { name: ' 张三 ', mobile: ' 13800000000 ' }
  await page.submit()
  assert.deepEqual(JSON.parse(JSON.stringify(calls.apply[0])), {
    consultantName: '张三', mobile: '13800000000', acceptedTerms: true, termsVersion: '2026-09-v1'
  })
  assert.equal(calls.navigate[0], '/packagesPublic/TalentCenter/index')
  assert.equal(page.pageMode, 'approved')
  assert.equal(page.submitting, false)
})

test('pending, failed and absent approval never navigate as success', async () => {
  for (const options of [{ consultant: { status: '00' } }, {}, { infoError: true }, { applyError: true }]) {
    const { page, calls } = setup(options)
    page.checked = true
    page.form = { name: '张三', mobile: '13800000000' }
    await page.submit()
    assert.equal(calls.navigate.length, 0)
    assert.ok(page.error)
    assert.equal(page.submitting, false)
  }
})

test('duplicate submit is locked and cancelled login never writes an application', async () => {
  let release
  const waitApply = new Promise(resolve => { release = resolve })
  const { page, calls } = setup({ waitApply, consultant: { status: '01', consultantId: 9 } })
  page.checked = true
  page.form = { name: '张三', mobile: '13800000000' }
  const first = page.submit()
  await page.submit()
  release()
  await first
  assert.equal(calls.apply.length, 1)
  const guest = setup({ authorized: false })
  guest.page.checked = true
  guest.page.form = page.form
  await guest.page.submit()
  assert.equal(guest.calls.apply.length, 0)
})

test('approved applicants can retry navigation without resubmitting', async () => {
  const { page, calls } = setup({ consultant: { consultantId: 9, status: '01' }, navigationError: true })
  await page.checkConsultantStatus()
  assert.equal(page.pageMode, 'approved')
  assert.ok(page.error)
  await page.openCenter()
  assert.equal(calls.apply.length, 0)
  assert.equal(calls.navigate.length, 2)
})

test('center gate only opens the fixed registration URL after confirmed approval', async () => {
  const file = 'packagesPublic/TalentCenter/index.vue'
  const approved = setup({ consultant: { consultantId: 9, status: '01' } }, file)
  assert.equal(approved.page.talentCenterUrl, '')
  await approved.page.checkAccess()
  assert.equal(approved.page.talentCenterUrl, 'https://gatheringtech.com/talent/register?return_to=%2F')
  assert.doesNotMatch(approved.page.talentCenterUrl, /mobile|name=|token/)
  for (const options of [{ authorized: false }, {}, { consultant: { status: '00' } }]) {
    const gate = setup(options, file)
    await gate.page.checkAccess()
    assert.equal(gate.page.talentCenterUrl, '')
    assert.equal(gate.calls.navigate[0], '/packagesMember/retail/apply/index')
  }
  const failed = setup({ infoError: true }, file)
  await failed.page.checkAccess()
  assert.ok(failed.page.error)
  assert.equal(failed.page.talentCenterUrl, '')
  assert.equal(failed.calls.navigate.length, 0)
})
