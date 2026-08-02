/**
 * 课程报名阶段
 * - not_start：未到报名开始日
 * - closed：已过报名截止日
 * - open：可报名
 *
 * 报名截止日为空时，回退用开课日 startDate（与业务文案「报名至开课」一致）
 */
function pad2(n) {
	return n < 10 ? '0' + n : '' + n
}

export function toDateKey(value) {
	if (value === null || value === undefined || value === '') return ''
	if (value instanceof Date && Number.isFinite(value.getTime())) {
		return `${value.getFullYear()}-${pad2(value.getMonth() + 1)}-${pad2(value.getDate())}`
	}
	const raw = String(value).trim()
	const matched = raw.match(/(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})/)
	if (!matched) return ''
	return `${matched[1]}-${pad2(Number(matched[2]))}-${pad2(Number(matched[3]))}`
}

export function getTodayKey(now = new Date()) {
	const d = now instanceof Date ? now : new Date(now)
	if (!Number.isFinite(d.getTime())) return ''
	return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

export function getCourseSignupPhase(ext = {}, now = new Date()) {
	const today = getTodayKey(now)
	if (!today) return 'open'

	const startKey = toDateKey(ext.signupStart)
	if (startKey && today < startKey) {
		return 'not_start'
	}

	const endKey = toDateKey(ext.signupEnd) || toDateKey(ext.startDate)
	if (endKey && today > endKey) {
		return 'closed'
	}
	return 'open'
}

export function getCourseSignupPhaseText(phase) {
	if (phase === 'not_start') return '报名尚未开始'
	if (phase === 'closed') return '报名已截止'
	return '报名中'
}

export function getCourseApplyButtonText(phase) {
	if (phase === 'not_start') return '报名尚未开始'
	if (phase === 'closed') return '报名已截止'
	return '立即报名'
}

export function canApplyCourse(ext = {}, now = new Date()) {
	return getCourseSignupPhase(ext, now) === 'open'
}
