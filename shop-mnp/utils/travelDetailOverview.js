import { travelCatalogOverview, travelOverviewRows } from './travelPresentation.js'

const emptyValue = value => !value || /待补|待确认|待核实|暂无|未提供|未注明|未知|^[-—/]+$/.test(value)
const compactKey = key => key.replace(/[\s：:]/g, '').replace(/／/g, '/')
const fieldDefinitions = [
	['城市', ['所在城市', '城市']],
	['类型', ['品类', '类型']],
	['档次', ['档次']],
	['海拔', ['海拔', '海拔高度']],
	['开业 / 装修', ['开业/装修']],
	['开业时间', ['开业时间', '开业']],
	['装修时间', ['装修时间', '装修']],
	['地址', ['详细地址', '地址']],
	['基地特色', ['一句话特点', '基地特色']]
]

// Render source facts as native, wrapping fields, never as a spreadsheet.
// Unknown labelled facts and unlabelled source notes are retained, not guessed.
export function travelOverviewSection(content) {
	const values = new Map(), notes = []
	for (const cells of travelOverviewRows(content)) {
		if (cells.length === 1 && !emptyValue(cells[0])) notes.push(cells[0])
		if (cells.length !== 2) continue
		const [label, value] = cells, key = compactKey(label)
		if (emptyValue(value) || (key === '项目' && value === '内容') || key === '标签') continue
		if (!key) { notes.push(value); continue }
		if (!values.has(key)) values.set(key, { label, value })
	}
	const fields = []
	function addField(label, key) {
		let { value } = values.get(key)
		if (label === '海拔' && /\d/.test(value)) {
			value = value.replace(/^海拔[：:\s]*/, '').replace(/\s*米/g, '米') + (/米|m\b/i.test(value) ? '' : '米')
		}
		if (/开业|装修/.test(label)) value = value.replace(/\s*(年|月|日)\s*/g, '$1')
		fields.push({ label, value, wide: /地址|特色/.test(label) || value.length > 18 })
		values.delete(key)
	}
	for (const [label, keys] of fieldDefinitions) {
		for (const key of keys) if (values.has(key)) { addField(label, key); break }
	}
	for (const [key, field] of values) addField(field.label, key)
	const tags = travelCatalogOverview({ goodsType: 'hotel', features: [{ sectionName: '基地概况', content }] }).tags
	const structured = fields.length > 0 || notes.length > 0 || tags.length > 0
	// Keep supplementary prose and photos when replacing a source table.
	const extraContent = structured ? String(content || '').replace(/<table\b[^>]*>[\s\S]*?<\/table>/gi,
		table => (table.match(/<img\b[^>]*>/gi) || []).join('')) : ''
	return { structured, fields, notes: [...new Set(notes)], tags, extraContent }
}

export function travelDetailOverview(goods) {
	const sections = (Array.isArray(goods && goods.features) ? goods.features : []).filter(section => /基地概况/.test(section.sectionName || ''))
	const fields = sections.flatMap(section => travelOverviewSection(section.content).fields)
	return {
		hasSource: sections.length > 0,
		tags: travelCatalogOverview({ ...goods, goodsType: 'hotel' }).tags,
		highlights: fields.filter(field => /^(海拔|开业 \/ 装修|开业时间|装修时间)$/.test(field.label))
	}
}
