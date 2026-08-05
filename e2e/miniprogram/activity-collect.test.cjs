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

test('activity detail compiles collect as a borderless star', async () => {
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

  const collectStarStyles = styles.match(/\.collect-star\s*\{([^}]*)\}/)
  assert.ok(collectStarStyles, 'collect star styles should exist')
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
