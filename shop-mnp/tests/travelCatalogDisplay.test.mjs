import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import { travelCatalogOverview, catalogPriceParts, formatCatalogPrice } from '../utils/travelPresentation.js'

const overview = rows => ({ goodsType: 'hotel', tags: '旅居', features: [
    { sectionName: '基地介绍', content: '<p>标签：错误标签；海拔9999米</p>' },
    { sectionName: '基地概况', content: `<table><tbody>${rows.map(([key, value]) => `<tr><td><p><span>${key}</span></p></td><td><p><span>${value}</span></p></td></tr>`).join('')}</tbody></table>` }
] })

test('Kunming example reads all four source tags and both facts without using generic tags', () => {
    const goods = overview([['档次', '优选高档'], ['海拔', '1891 米'], ['开业 / 装修', '2019 年装修'], ['标签', '市中心 / 近商圈 / 花园庭院 / 全年不加价']])
    assert.deepEqual(travelCatalogOverview(goods), { tags: ['市中心', '近商圈', '花园庭院', '全年不加价'], facts: '2019年装修 | 海拔1891米' })
    assert.equal(goods.tags, '旅居')
})

test('overview rows tolerate rich text, entities, row headers and separators', () => {
    const goods = overview([['海拔高度', '0'], ['开业时间', '2020年开业 / 2024年装修'], ['标签', '市中心／近商圈，KTV &amp; 棋牌室 | 全年不加价；市中心']])
    goods.features[1].content = goods.features[1].content.replaceAll('<td>', '<th>').replaceAll('</td>', '</th>')
    assert.deepEqual(travelCatalogOverview(goods), { tags: ['市中心', '近商圈', 'KTV & 棋牌室', '全年不加价'], facts: '2020年开业 / 2024年装修 | 海拔0米' })
    assert.equal(travelCatalogOverview(overview([['海拔', '1&#56;91&nbsp;米']])).facts, '海拔1891米')
})

test('missing or placeholder facts and tags are omitted, never inferred from generic copy', () => {
    const empty = { tags: [], facts: '' }
    for (const goods of [null, {}, { goodsType: 'hotel', tags: '旅居,温泉', description: '1891米、2019年' }, overview([['海拔', '待补'], ['开业 / 装修', '暂无资料'], ['标签', '待确认']])]) {
        assert.deepEqual(travelCatalogOverview(goods), empty)
    }
    assert.deepEqual(travelCatalogOverview({ ...overview([['标签', '市中心']]), goodsType: 'education' }), empty)
    assert.deepEqual(travelCatalogOverview(overview([['标签', '花园庭院<script>ignore()</script>']])), { tags: ['花园庭院'], facts: '' })
})

test('price components match requested large integer plus small 起/人/晚 suffix', () => {
    assert.deepEqual(catalogPriceParts({ goodsType: 'hotel', price: 99.67, unit: '人/晚起' }), { amount: '100', suffix: '起/人/晚' })
    assert.equal(formatCatalogPrice({ goodsType: 'hotel', price: 58.49, unit: '人/晚起' }), '58起/人/晚')
    assert.deepEqual(catalogPriceParts({ goodsType: 'hotel', price: '745.05', unit: '人/7天' }), { amount: '745.05', suffix: '' })
    assert.equal(formatCatalogPrice({ goodsType: 'education', price: '320.50', unit: '人/晚起' }), '320.50')
})

test('both catalog cards use source overview with all tags and a compact price suffix', () => {
    const classify = readFileSync(new URL('../pages/classify/classify.vue', import.meta.url), 'utf8')
    const home = readFileSync(new URL('../pages/home/home.vue', import.meta.url), 'utf8')
    for (const source of [classify, home]) {
        assert.match(source, /travelCatalogOverview\(/)
        assert.match(source, /price-suffix/)
        assert.match(source, /catalogPriceParts\((item|goods)\)\.amount/)
        assert.match(source, /catalogPriceParts\((item|goods)\)\.suffix/)
    }
    assert.match(classify, /item\.catalogFacts/)
    assert.doesNotMatch(classify.split('class="stay-card travel-card"')[1].split('</block>')[0], /item\.tagList\.slice\(0, 3\)/)
    assert.match(home, /catalogTags\(goods\)/)
    const style = readFileSync(new URL('../pages/classify/classify.scss', import.meta.url), 'utf8')
    assert.match(style, /\.travel-mode[\s\S]*?flex-basis: 140rpx/)
})

test('travel details action uses a proportional height and larger single-line text without shrinking senior targets', () => {
    const style = readFileSync(new URL('../pages/classify/classify.scss', import.meta.url), 'utf8')
    const action = style.match(/\.travel-card \.price-row \.btn-primary\s*\{([^}]+)\}/)[1]
    assert.match(action, /height:\s*80rpx/)
    assert.match(action, /min-height:\s*80rpx/)
    assert.match(action, /font-size:\s*32rpx/)
    assert.match(action, /white-space:\s*nowrap/)
    assert.match(action, /box-sizing:\s*border-box/)
    assert.doesNotMatch(action, /!important/)
    const senior = readFileSync(new URL('../style/senior-mode.scss', import.meta.url), 'utf8')
    assert.match(senior, /min-height:\s*\$senior-touch !important/)
})


test('travel list card enlarges titles, facts, summaries and wrapping tags without affecting course cards', () => {
    const style = readFileSync(new URL('../pages/classify/classify.scss', import.meta.url), 'utf8')
    const travel = style.slice(style.lastIndexOf('.travel-card .listing-body {'))
    assert.match(travel, /\.card-title\s*\{[^}]*font-size:\s*34rpx/)
    assert.match(travel, /\.meta\s*\{[^}]*font-size:\s*28rpx/)
    assert.match(travel, /\.base-facts\s*\{[^}]*font-size:\s*28rpx/)
    assert.match(travel, /\.outline-chip\s*\{[^}]*min-height:\s*52rpx;[^}]*font-size:\s*28rpx;[^}]*box-sizing:\s*border-box/s)
    assert.match(style, /\.tags\s*\{[^}]*flex-wrap:\s*wrap/s)
})
