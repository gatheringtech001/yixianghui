import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import { travelDetailOverview, travelOverviewSection } from '../utils/travelDetailOverview.js'

const table = rows => `<table>${rows.map(cells => `<tr>${cells.map(cell => `<td><p>${cell}</p></td>`).join('')}</tr>`).join('')}</table>`

test('overview uses fixed source-backed fields and all source tags, not generic catalog tags', () => {
    const content = table([
        ['项目', '内容'], ['标签', '市中心 / 近商圈 / 花园庭院 / 全年不加价'],
        ['开业 / 装修', '2019 年装修'], ['详细地址', '盘龙区东风东路52号'],
        ['海拔', '1891 米'], ['所在城市', '昆明市'], ['品类', '酒店'], ['档次', '优选高档'],
        ['一句话特点', '东风广场与恒隆广场商圈内，花园庭院'], ['', '昆明二号基地·东风广场核心']
    ])
    const section = travelOverviewSection(content)
    assert.equal(section.structured, true)
    assert.deepEqual(section.fields.map(f => f.label), ['城市', '类型', '档次', '海拔', '开业 / 装修', '地址', '基地特色'])
    assert.deepEqual(section.notes, ['昆明二号基地·东风广场核心'])
    assert.equal(section.fields.find(f => f.label === '地址').wide, true)
    assert.equal(section.extraContent, '')
    const summary = travelDetailOverview({ tags: '旅居|错误标签', features: [{ sectionName: '基地概况', content }] })
    assert.equal(summary.hasSource, true)
    assert.deepEqual(summary.tags, ['市中心', '近商圈', '花园庭院', '全年不加价'])
    assert.deepEqual(summary.highlights.map(f => [f.label, f.value]), [['海拔', '1891米'], ['开业 / 装修', '2019年装修']])
})

test('missing facts are omitted and never inferred from description or generic tags', () => {
    assert.deepEqual(travelDetailOverview({ description: '1891米，2019年', tags: '温泉' }), { hasSource: false, tags: [], highlights: [] })
    const content = table([['海拔', '待补'], ['开业时间', '未知'], ['标签', '待确认'], ['地址', '暂无资料']])
    assert.equal(travelOverviewSection(content).structured, false)
    assert.deepEqual(travelDetailOverview({ features: [{ sectionName: '基地概况', content }] }).highlights, [])
})

test('preserves separate opening and renovation dates, zero elevation and supplementary material', () => {
    const content = '<p>基地补充说明</p>' + table([
        ['装修时间', '2024 年 5 月'], ['开业时间', '2020 年开业'], ['海拔高度', '0'],
        ['交通', '地铁旁 &amp; 公交直达'], ['', '<img src="https://example.test/room.jpg">']
    ])
    const section = travelOverviewSection(content)
    assert.deepEqual(section.fields.slice(0, 3).map(f => f.value), ['0米', '2020年开业', '2024年5月'])
    assert.equal(section.fields.at(-1).value, '地铁旁 & 公交直达')
    assert.match(section.extraContent, /基地补充说明/)
    assert.match(section.extraContent, /room\.jpg/)
    assert.doesNotMatch(section.extraContent, /<table/)
})

test('retains rich content fallback for legacy sections without a structured overview', () => {
    assert.equal(travelOverviewSection('<p>老基地的完整介绍</p>').structured, false)
    const source = readFileSync(new URL('../packagesMall/GoodsDetails/SojournGoodsDetails.vue', import.meta.url), 'utf8')
    assert.match(source, /hotelData\.overview\.highlights/)
    assert.match(source, /overview_field_wide/)
    assert.match(source, /v-if="item\.overview && item\.overview\.structured"/)
    assert.match(source, /<u-parse v-else :html="displayRichText\(item\.content\)"/)
    assert.match(source, /overview\.hasSource \? this\.hotelData\.overview\.tags/)
})

test('detail source tags use brand burgundy on a pale rose background without changing their layout', () => {
    const style = readFileSync(new URL('../packagesMall/GoodsDetails/SojournGoodsDetails.scss', import.meta.url), 'utf8')
    const tags = style.split('.base_tag_view {')[1].split('.base_facts_view {')[0]
    assert.match(tags, /color:\s*\$page-accent/)
    assert.match(tags, /background:\s*#fff2ef/)
    assert.match(tags, /border:\s*1rpx solid #e9c2bd/)
    assert.match(tags, /flex-wrap:\s*wrap/)
    assert.match(tags, /min-height:\s*52rpx/)
})
