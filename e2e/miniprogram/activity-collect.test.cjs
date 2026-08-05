const assert = require('node:assert/strict')
const fs = require('node:fs/promises')
const path = require('node:path')
const test = require('node:test')
const {
  captureScreenshot,
  closeMiniProgram,
  launchMiniProgram,
  runStep
} = require('./test-helpers.cjs')

const activityDetailDir = path.resolve(
  __dirname,
  '../../shop-mnp/packagesMall/Activity/detail'
)
const compiledActivityDetailDir = path.resolve(
  __dirname,
  '../../shop-mnp/unpackage/dist/dev/mp-weixin/packagesMall/Activity/detail'
)

test('activity detail compiles collect as a star over the cover', async () => {
  const [template, styles, compiledWxml] = await Promise.all([
    fs.readFile(path.join(activityDetailDir, 'index.vue'), 'utf8'),
    fs.readFile(path.join(activityDetailDir, 'index.scss'), 'utf8'),
    fs.readFile(path.join(compiledActivityDetailDir, 'index.wxml'), 'utf8')
  ])

  assert.match(template, /class="collect-star"/)
  assert.match(template, /:name="collectId \? 'star-fill' : 'star'"/)
  assert.doesNotMatch(template, /class="btn-collect"/)
  assert.doesNotMatch(template, />\s*\{\{ collectId \? '已收藏' : '收藏' \}\}/)
  assert.match(compiledWxml, /name="\{\{collectId\?'star-fill':'star'\}\}"/)

  const activitySheetStart = template.indexOf('<view class="activity-sheet"')
  const compiledActivitySheetStart = compiledWxml.indexOf('<view class="activity-sheet')
  assert.ok(activitySheetStart > 0, 'activity sheet should exist')
  assert.ok(compiledActivitySheetStart > 0, 'compiled activity sheet should exist')
  assert.match(
    template.slice(0, activitySheetStart),
    /class="collect-star"/,
    'collect star should be inside the cover before the activity sheet'
  )
  assert.match(
    compiledWxml.slice(0, compiledActivitySheetStart),
    /collect-star/,
    'compiled collect star should be inside the cover'
  )

  const collectStarStyles = styles.match(/\.collect-star\s*\{([^}]*)\}/)
  assert.ok(collectStarStyles, 'collect star styles should exist')
  assert.match(collectStarStyles[1], /position:\s*absolute/)
  assert.match(collectStarStyles[1], /top:/)
  assert.match(collectStarStyles[1], /right:/)
  assert.doesNotMatch(collectStarStyles[1], /\bborder\b|\bbackground\b/)
  assert.doesNotMatch(styles, /\.btn-collect/)
})

test('activity detail renders in WeChat DevTools', {
  timeout: 90000
}, async testContext => {
  const exceptions = []
  const miniProgram = await launchMiniProgram(testContext, exceptions)
  try {
    const activityPage = await runStep(testContext, {
      label: 'open activity detail',
      action: () => miniProgram.reLaunch('/packagesMall/Activity/detail/index?id=1')
    })
    await activityPage.waitFor(3000)
    await captureScreenshot(miniProgram, 'activity-collect-star.png')
    assert.deepEqual(exceptions, [])
  } finally {
    await closeMiniProgram(testContext, miniProgram)
  }
})
