const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const loadPage = require('./vue-page-harness.cjs')

function popup(uploadUserAvatar = async () => '/profile/avatar.jpg') {
  const toasts = []
  const loaded = loadPage('components/AuthProfilePopup/AuthProfilePopup.vue', {
    uploadUserAvatar,
    uni: {showToast: item => toasts.push(item.title)}
  })
  loaded.page.step = 2
  return {...loaded, toasts}
}

function loginHarness({expired = false, existingAvatar = ''} = {}) {
  const storage = {token: 'test-token', userInfo: {nickName: '微信小程序用户', avatar: existingAvatar}}
  const updates = []
  const uploads = []
  const phones = []
  const source = fs.readFileSync(path.resolve(__dirname, '../../shop-mnp/utils/login.js'), 'utf8')
    .replace(/^import .*$/gm, '').replace(/export default initAuthorization/, '')
    .replace(/export /g, '')
  const context = {
    module: {exports: {}}, console,
    uni: {
      getStorageSync: key => storage[key],
      setStorageSync: (key, value) => { storage[key] = value },
      removeStorageSync: key => { delete storage[key] },
      showLoading() {}, hideLoading() {}, showToast() {}
    },
    isTokenExpired: () => expired,
    getInviteParentUserId: () => null,
    normalizeAvatarPath: value => value,
    uploadUserAvatar: async value => { uploads.push(value); return '/profile/uploaded.jpg' },
    updateInfo: async value => {
      updates.push(JSON.parse(JSON.stringify(value)))
      storage.userInfo = {...storage.userInfo, ...value}
      return {code: 200}
    },
    getInfo: async () => ({data: {...storage.userInfo}}),
    wxProfileAuth: async value => { phones.push(value); return {code: 200} }
  }
  vm.runInNewContext(source + '\nmodule.exports = {isProfileComplete, isAuthorizedUser, saveProfile, ensureLogin}', context)
  return {...context.module.exports, storage, updates, uploads, phones}
}

test('nickname alone enables continue; blur does not force a transition before optional avatar selection', () => {
  const {page, source} = popup()
  page.onNicknameBlur({detail: {value: ' 测试称呼 '}})
  assert.equal(page.profileReady, true)
  assert.equal(page.step, 2)
  page.goPhoneStep()
  assert.equal(page.step, 3)
  let result
  page.resolveFn = value => { result = value }
  page.onSkipPhone()
  assert.equal(result.nickName, '测试称呼')
  assert.equal(result.avatar, '')
  assert.equal(result.phoneCode, '')
  assert.match(source, /设置头像（选填）/)
  assert.match(source, /称呼（必填）/)
  assert.doesNotMatch(source, /请先设置头像|需要授权头像才能继续|微信要求头像和昵称分别确认/)
})

test('missing, cleared, placeholder and overlong nicknames still cannot continue', () => {
  for (const value of ['', '   ', '微信小程序用户', '长'.repeat(31)]) {
    const {page, toasts} = popup()
    page.onNicknameBlur({detail: {value}})
    assert.equal(page.profileReady, false)
    page.goPhoneStep()
    assert.equal(page.step, 2)
    assert.equal(toasts.length, 1)
  }
  const {page} = popup()
  page.nickName = '之前的称呼'
  page.onNicknameBlur({detail: {value: ''}})
  assert.equal(page.nickName, '')
  assert.equal(page.profileReady, false)
})

test('avatar cancellation or upload failure does not block continuation or discard a saved avatar', async () => {
  const {page, toasts} = popup(async () => { throw new Error('上传失败') })
  page.nickName = '测试称呼'
  page.onChooseAvatar({detail: {errMsg: 'chooseAvatar:fail deny'}})
  assert.equal(page.profileReady, true)
  assert.match(toasts[0], /仍可继续/)
  page.avatarUrl = '/profile/existing.jpg'
  page.onChooseAvatar({detail: {avatarUrl: 'wxfile://temporary'}})
  assert.equal(page.profileReady, false)
  page.goPhoneStep()
  assert.equal(page.step, 2)
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(page.avatarUrl, '/profile/existing.jpg')
  assert.equal(page.avatarUploading, false)
  assert.equal(page.step, 2)
  page.goPhoneStep()
  assert.equal(page.step, 3)
})

test('selected avatars still upload and wait for an explicit continue', async () => {
  const {page} = popup()
  page.nickName = '测试称呼'
  page.onChooseAvatar({detail: {avatarUrl: 'wxfile://temporary'}})
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(page.avatarUrl, '/profile/avatar.jpg')
  assert.equal(page.step, 2)
  page.goPhoneStep()
  assert.equal(page.step, 3)
})

test('profile completeness needs a valid nickname, not an avatar; token expiry still blocks authorization', () => {
  const login = loginHarness()
  assert.equal(login.isProfileComplete({nickName: '测试称呼'}), true)
  for (const value of [null, {}, {nickName: ' '}, {nickName: '微信小程序用户'}, {nickName: '长'.repeat(31)}]) {
    assert.equal(login.isProfileComplete(value), false)
  }
  login.storage.userInfo = {nickName: '测试称呼', avatar: ''}
  assert.equal(login.isAuthorizedUser(), true)
  const expired = loginHarness({expired: true})
  expired.storage.userInfo = {nickName: '测试称呼'}
  assert.equal(expired.isAuthorizedUser(), false)
})

test('saving only nickname omits avatar from API payload, preserves existing avatar, and does not prompt again', async () => {
  for (const existingAvatar of ['', '/profile/existing.jpg']) {
    const login = loginHarness({existingAvatar})
    await login.saveProfile({nickName: ' 测试称呼 ', avatar: '', phoneCode: ''})
    assert.deepEqual(login.updates, [{nickName: '测试称呼'}])
    assert.deepEqual(login.uploads, [])
    assert.deepEqual(login.phones, [])
    assert.equal(login.storage.userInfo.avatar, existingAvatar)
    assert.equal(login.isAuthorizedUser(), true)
    assert.equal(await login.ensureLogin({}), true, 'a persisted nickname must bypass the profile popup')
  }
})

test('optional avatar does not break uploaded avatars or optional phone binding', async () => {
  const login = loginHarness()
  await login.saveProfile({nickName: '测试称呼', avatar: 'wxfile://temporary', phoneCode: 'test-phone-code'})
  assert.deepEqual(login.uploads, ['wxfile://temporary'])
  assert.deepEqual(login.updates, [{nickName: '测试称呼', avatar: '/profile/uploaded.jpg'}])
  assert.equal(login.phones[0].phoneCode, 'test-phone-code')
  assert.equal(login.isAuthorizedUser(), true)
})

test('invalid nickname never reaches the profile update API', async () => {
  const login = loginHarness()
  for (const nickName of ['', ' ', '微信小程序用户', '长'.repeat(31)]) {
    await assert.rejects(() => login.saveProfile({nickName}))
  }
  assert.deepEqual(login.updates, [])
})
