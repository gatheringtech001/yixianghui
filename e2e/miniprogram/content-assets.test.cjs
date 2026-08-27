const assert = require('node:assert/strict')
const test = require('node:test')
const {
  assertImageSourcesAvailable,
  captureScreenshot,
  closeMiniProgram,
  extractRichTextImageSources,
  getCurrentPageState,
  invokeCurrentVmMethod,
  launchMiniProgram,
  localBackendPrefix,
  runStep,
  setCurrentVmState,
  waitUntil
} = require('./test-helpers.cjs')

test('customer, profile and rich text assets render from the local backend', {
  timeout: 120000
}, async testContext => {
  const exceptions = []
  const miniProgram = await launchMiniProgram(testContext, exceptions)
  try {
    await runStep(testContext, {
      label: 'open home before tab checks',
      action: () => miniProgram.reLaunch('/pages/home/home')
    })
    await miniProgram.callWxMethod('setStorageSync', 'housekeeperQrCursor', 0)
    await runStep(testContext, {
      label: 'wait for home housekeepers',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['contact'])
        return Array.isArray(state.contact) && state.contact.length === 2
      })
    })
    await invokeCurrentVmMethod(miniProgram, 'openHousekeeper')
    let housekeeperState = await getCurrentPageState(miniProgram, [
      'selectedContact',
      'showContact'
    ])
    assert.equal(housekeeperState.selectedContact.adName, '媛媛')
    assert.equal(housekeeperState.showContact, true)
    assert.ok(housekeeperState.selectedContact.adImage.startsWith('/profile/'))
    await new Promise(resolve => setTimeout(resolve, 700))
    await captureScreenshot(miniProgram, 'home-housekeeper-yuanyuan.png')
    await setCurrentVmState(miniProgram, { showContact: false })

    await runStep(testContext, {
      label: 'open customer service page',
      action: () => miniProgram.callWxMethod('switchTab', {
        url: '/pages/MembersOpened/MembersOpened'
      })
    })
    await runStep(testContext, {
      label: 'wait for customer service assets',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['customerData'])
        const customerData = state.customerData || {}
        const staffList = customerData.staffList || []
        return typeof customerData.qrCode === 'string' &&
          customerData.qrCode.startsWith(localBackendPrefix) &&
          staffList.length >= 2 &&
          staffList.every(staff => (
            typeof staff.qrCode === 'string' && staff.qrCode.startsWith(localBackendPrefix)
          ))
      })
    })
    const customerState = await getCurrentPageState(miniProgram, ['customerData'])
    await assertImageSourcesAvailable([
      customerState.customerData.qrCode,
      customerState.customerData.headerBg,
      ...customerState.customerData.staffList.map(staff => staff.qrCode)
    ], 'customer service configured assets')
    await captureScreenshot(miniProgram, 'customer-service.png')

    await runStep(testContext, {
      label: 'open profile page',
      action: () => miniProgram.callWxMethod('switchTab', {
        url: '/pages/my/my'
      })
    })
    await runStep(testContext, {
      label: 'wait for profile steward asset',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['stewardImageUrl'])
        return typeof state.stewardImageUrl === 'string' &&
          state.stewardImageUrl.startsWith(localBackendPrefix)
      })
    })
    const profileState = await getCurrentPageState(miniProgram, ['stewardImageUrl'])
    await assertImageSourcesAvailable(
      [profileState.stewardImageUrl],
      'profile steward asset'
    )
    await runStep(testContext, {
      label: 'wait for profile housekeepers',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['contact'])
        return Array.isArray(state.contact) && state.contact.length === 2
      })
    })
    await invokeCurrentVmMethod(miniProgram, 'openHousekeeper')
    housekeeperState = await getCurrentPageState(miniProgram, [
      'selectedContact',
      'showContact'
    ])
    assert.equal(housekeeperState.selectedContact.adName, '曼曼')
    assert.equal(housekeeperState.showContact, true)
    assert.ok(housekeeperState.selectedContact.adImage.startsWith('/profile/'))
    await new Promise(resolve => setTimeout(resolve, 700))
    await captureScreenshot(miniProgram, 'profile-housekeeper-manman.png')

    await runStep(testContext, {
      label: 'open activity rich text page',
      action: () => miniProgram.reLaunch('/packagesMall/Activity/detail/index?id=1')
    })
    await runStep(testContext, {
      label: 'wait for activity rich text',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['detailInfo'])
        return state.detailInfo && state.detailInfo.content
      })
    })
    const activityState = await getCurrentPageState(miniProgram, ['detailInfo'])
    await assertImageSourcesAvailable(
      extractRichTextImageSources(activityState.detailInfo.content),
      'activity rich text assets'
    )
    await captureScreenshot(miniProgram, 'activity-rich-text.png')

    await runStep(testContext, {
      label: 'open single page rich text',
      action: () => miniProgram.reLaunch('/packagesPublic/Article/index?id=3')
    })
    await runStep(testContext, {
      label: 'wait for single page rich text',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['detailInfo'])
        return state.detailInfo && state.detailInfo.content
      })
    })
    const articleState = await getCurrentPageState(miniProgram, ['detailInfo'])
    await assertImageSourcesAvailable(
      extractRichTextImageSources(articleState.detailInfo.content),
      'single page rich text assets'
    )
    await captureScreenshot(miniProgram, 'single-page-rich-text.png')
    assert.deepEqual(exceptions, [])
  } finally {
    await closeMiniProgram(testContext, miniProgram)
  }
})
