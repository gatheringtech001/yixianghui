const FIELD_LABELS = {
	time: '\u4e0a\u8bfe\u65f6\u95f4',
	place: '\u6388\u8bfe\u5730\u70b9',
	teacher: '\u6388\u8bfe\u8001\u5e08',
	note: '\u6750\u6599\u5907\u6ce8'
}

function extractFieldValue(line, label) {
	if (!line.startsWith(label)) return ''
	return line.slice(label.length).replace(/^[\uFF1A:]\s*/, '').trim()
}

export function parseCourseMeta(description, options = {}) {
	const includeNote = options.includeNote === true
	const meta = {
		time: '',
		place: '',
		teacher: '',
		summary: ''
	}
	if (includeNote) {
		meta.note = ''
	}
	if (!description) return meta

	const labels = includeNote
		? FIELD_LABELS
		: {
			time: FIELD_LABELS.time,
			place: FIELD_LABELS.place,
			teacher: FIELD_LABELS.teacher
		}

	const lines = String(description).split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
	const otherLines = []

	lines.forEach((line) => {
		let matched = false
		for (const key of Object.keys(labels)) {
			const label = labels[key]
			if (line.startsWith(label)) {
				meta[key] = extractFieldValue(line, label)
				matched = true
				break
			}
		}
		if (!matched) {
			otherLines.push(line)
		}
	})

	meta.summary = otherLines.join(' ')
	return meta
}
