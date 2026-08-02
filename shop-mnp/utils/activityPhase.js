/**
 * 活动展示阶段（与列表「已结束」筛选项同一套时间语义）
 * - ended：activityEndTime 已过（列表侧栏「已结束」）
 * - closed：活动未结束，但 signEndTime 已过（报名已截止）
 * - applying：可报名
 *
 * activityEndTime 为空视为未结束（与后端 signFilter=active 一致）
 * signEndTime 为空视为仍可报名
 */
export function parseActivityTime(value) {
	if (value === null || value === undefined || value === '') return null
	const raw = String(value).trim().replace(/-/g, '/')
	const ts = new Date(raw).getTime()
	return Number.isFinite(ts) ? ts : null
}

export function getActivityPhase(activity, now = Date.now()) {
	if (!activity) return 'applying'
	const endTs = parseActivityTime(activity.activityEndTime)
	if (endTs != null && now >= endTs) {
		return 'ended'
	}
	const signEndTs = parseActivityTime(activity.signEndTime)
	if (signEndTs != null && now >= signEndTs) {
		return 'closed'
	}
	return 'applying'
}

export function getActivityPhaseText(phase) {
	if (phase === 'ended') return '已结束'
	if (phase === 'closed') return '报名已截止'
	return '报名中'
}

export function canApplyActivity(activity, now = Date.now()) {
	return getActivityPhase(activity, now) === 'applying'
}
