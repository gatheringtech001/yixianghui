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

async function assertBackendImagesAvailable(miniProgram) {
  const sources = await miniProgram.evaluate(() => {
    const pages = getCurrentPages()
    const vm = pages[pages.length - 1].$vm
    return [
      vm.brandLogoDisplay,
      vm.housekeeperAvatarDisplay,
      vm.heroImage,
      ...(vm.hotCardList || []).map(item => item.adImage),
      ...(vm.currentGoodsList || []).map(item => item.goodsCover),
      ...(vm.contact || []).map(item => item.adImage)
    ].map(source => (
      typeof source === 'string' && source.startsWith('/profile/')
        ? vm.host + source
        : source
    ))
  })
  const backendSources = sources.filter(source => (
    typeof source === 'string' &&
    source.startsWith(localBackendPrefix)
  ))
  await assertImageSourcesAvailable(backendSources, 'home rendered backend images')
}

test('home, education and product details use the local production snapshot', { timeout: 180000 }, async testContext => {
  const exceptions = []
  const miniProgram = await launchMiniProgram(testContext, exceptions)

  try {
    await runStep(testContext, {
      label: 'seed local site selection',
      action: () => miniProgram.callWxMethod('setStorageSync', 'site', {
        deptId: 108,
        deptName: '昆明'
      })
    })
    const home = await runStep(testContext, {
      label: 'open home page',
      action: () => miniProgram.reLaunch('/pages/home/home')
    })
    assert.ok(home, 'home page should open')
    assert.equal(home.path, 'pages/home/home')
    const homeState = await getCurrentPageState(miniProgram, ['host'])
    assert.equal(homeState.host, 'http://127.0.0.1:18080/api')

    await runStep(testContext, {
      label: 'wait for eligible travel cities',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['navList', 'hotCardList'])
        return Array.isArray(state.navList) && state.navList.length > 0 &&
          Array.isArray(state.hotCardList) && state.hotCardList.length === 2
      })
    })
    const cityState = await getCurrentPageState(miniProgram, ['hotCardList'])
    assert.deepEqual(
      cityState.hotCardList.map(item => item.adName).sort(),
      ['大理', '昆明']
    )
    assert.ok(cityState.hotCardList.every(item => Number(item.goodsCount) > 0))
    await runStep(testContext, {
      label: 'wait for backend category goods',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, [
          'currentCategoryId',
          'currentGoodsList'
        ])
        return state.currentCategoryId === 38 &&
          Array.isArray(state.currentGoodsList) &&
          state.currentGoodsList.length > 0
      })
    })
    const homeAssetState = await getCurrentPageState(miniProgram, [
      'brandLogoUrl',
      'housekeeperAvatarUrl'
    ])
    assert.equal(
      homeAssetState.brandLogoUrl,
      '/static/home-design/brand-logo-transparent.png'
    )
    await assertImageSourcesAvailable(
      [homeAssetState.housekeeperAvatarUrl],
      'home configured backend assets'
    )
    await runStep(testContext, {
      label: 'verify rendered backend images',
      action: () => assertBackendImagesAvailable(miniProgram)
    })
    await runStep(testContext, {
      label: 'capture home screenshot',
      timeoutMs: 40000,
      action: () => captureScreenshot(miniProgram, 'home.png')
    })

    await runStep(testContext, {
      label: 'open travel service categories',
      action: () => invokeCurrentVmMethod(miniProgram, 'goToServiceTab')
    })
    await runStep(testContext, {
      label: 'wait for empty travel cities to be removed',
      action: () => waitUntil(async () => {
        const page = await miniProgram.currentPage()
        if (!page || page.path !== 'pages/classify/classify') return false
        const state = await getCurrentPageState(miniProgram, ['goodsCatrgoryList'])
        return Array.isArray(state.goodsCatrgoryList) &&
          state.goodsCatrgoryList.map(item => item.categoryName).join(',') === '全部,昆明,大理'
      })
    })
    await runStep(testContext, {
      label: 'return home after travel category check',
      action: () => miniProgram.callWxMethod('switchTab', { url: '/pages/home/home' })
    })
    await waitUntil(async () => {
      const page = await miniProgram.currentPage()
      return page && page.path === 'pages/home/home'
    })

    await runStep(testContext, {
      label: 'open education from the home entry',
      action: () => invokeCurrentVmMethod(miniProgram, 'goClassify', '老年教育')
    })
    await runStep(testContext, {
      label: 'wait for the production education course',
      action: () => waitUntil(async () => {
        const page = await miniProgram.currentPage()
        if (!page || page.path !== 'pages/classify/classify') return false
        const classifyState = await getCurrentPageState(miniProgram, [
          'navbarSelect',
          'goodsList'
        ])
        return classifyState.navbarSelect === 58 &&
          Array.isArray(classifyState.goodsList) &&
          classifyState.goodsList.some(item => item.goodsId === 38)
      })
    })
    const educationListState = await getCurrentPageState(miniProgram, ['goodsList'])
    const courseListItem = educationListState.goodsList.find(item => item.goodsId === 38)
    assert.equal(courseListItem.goodsName, '水彩绘画')
    assert.ok(courseListItem.summary)
    assert.ok(courseListItem.courseTime)
    assert.ok(courseListItem.coursePlace)
    assert.ok(courseListItem.courseTeacher)
    await captureScreenshot(miniProgram, 'education-list.png')

    await runStep(testContext, {
      label: 'open education course detail',
      action: () => miniProgram.evaluate(() => {
        const pages = getCurrentPages()
        const vm = pages[pages.length - 1].$vm
        const course = vm.goodsList.find(item => item.goodsId === 38)
        vm.goodsFn(course)
      })
    })
    await runStep(testContext, {
      label: 'wait for complete education course detail',
      action: () => waitUntil(async () => {
        const page = await miniProgram.currentPage()
        if (!page || page.path !== 'packagesMall/GoodsDetails/EducationGoodsDetails') {
          return false
        }
        const detailState = await getCurrentPageState(miniProgram, ['courseData'])
        const course = detailState.courseData
        return course && course.id === 38 &&
          course.courseTime && course.coursePlace && course.courseTeacher &&
          Array.isArray(course.sections) && course.sections.length === 3
      })
    })
    const educationDetailState = await getCurrentPageState(miniProgram, [
      'host',
      'courseData'
    ])
    assert.equal(educationDetailState.courseData.name, '水彩绘画')
    assert.ok(educationDetailState.courseData.materialNote)
    await assertImageSourcesAvailable(
      [`${educationDetailState.host}${educationDetailState.courseData.cover}`],
      'education detail cover'
    )
    await captureScreenshot(miniProgram, 'education-detail.png')

    await runStep(testContext, {
      label: 'open production travel product detail',
      action: () => miniProgram.callWxMethod('redirectTo', {
        url: '/packagesMall/GoodsDetails/SojournGoodsDetails?id=32'
      })
    })
    await runStep(testContext, {
      label: 'wait for complete travel product detail',
      action: () => waitUntil(async () => {
        const detailState = await getCurrentPageState(miniProgram, [
          'hotelData',
          'bannerImages',
          'skuGroupList'
        ])
        return detailState.hotelData && detailState.hotelData.id === 32 &&
          detailState.hotelData.related.length === 7 &&
          detailState.bannerImages.length === 3 &&
          detailState.skuGroupList.length > 0
      })
    })
    const travelDetailState = await getCurrentPageState(miniProgram, [
      'host',
      'hotelData',
      'bannerImages',
      'skuGroupList'
    ])
    assert.equal(travelDetailState.hotelData.name, '昆明古滇基地')
    assert.ok(travelDetailState.hotelData.desc)
    assert.ok(travelDetailState.hotelData.tagList.length > 0)
    assert.ok(travelDetailState.skuGroupList.every(group => group.title))
    await assertImageSourcesAvailable(
      travelDetailState.bannerImages.map(image => `${travelDetailState.host}${image}`),
      'travel detail gallery'
    )
    await assertImageSourcesAvailable(
      travelDetailState.hotelData.related.flatMap(section => (
        extractRichTextImageSources(section.content)
      )),
      'travel detail rich text'
    )
    await captureScreenshot(miniProgram, 'travel-detail.png')

    await runStep(testContext, {
      label: 'return home before search checks',
      action: () => miniProgram.callWxMethod('switchTab', {
        url: '/pages/home/home'
      })
    })
    await waitUntil(async () => {
      const page = await miniProgram.currentPage()
      return page && page.path === 'pages/home/home'
    })

    await runStep(testContext, {
      label: 'invoke home search behavior',
      action: () => invokeCurrentVmMethod(miniProgram, 'onSearch')
    })

    await runStep(testContext, {
      label: 'wait for search page',
      action: () => waitUntil(async () => {
        const page = await miniProgram.currentPage()
        return page && page.path === 'packagesMall/search/search'
      })
    })
    const searchPage = await miniProgram.currentPage()
    assert.equal(searchPage.path, 'packagesMall/search/search')
    await runStep(testContext, {
      label: 'set search keyword',
      action: () => setCurrentVmState(miniProgram, { keyword: '旅居' })
    })
    await waitUntil(async () => {
      const state = await getCurrentPageState(miniProgram, ['keyword'])
      return state.keyword === '旅居'
    })
    const keywordState = await getCurrentPageState(miniProgram, ['keyword'])
    assert.equal(keywordState.keyword, '旅居')

    await runStep(testContext, {
      label: 'submit search behavior',
      action: () => invokeCurrentVmMethod(miniProgram, 'onSearch')
    })
    await runStep(testContext, {
      label: 'wait for search response',
      action: () => waitUntil(async () => {
        const state = await getCurrentPageState(miniProgram, ['searching'])
        return state.searching === false
      })
    })
    const state = await getCurrentPageState(miniProgram, [
      'keyword',
      'showSearchResult',
      'showSearchEmpty'
    ])
    assert.equal(state.keyword, '旅居')
    assert.equal(state.showSearchResult || state.showSearchEmpty, true)
    await runStep(testContext, {
      label: 'capture search screenshot',
      action: () => captureScreenshot(miniProgram, 'search.png')
    })

    assert.deepEqual(exceptions, [])
  } finally {
    await closeMiniProgram(testContext, miniProgram)
  }
})
