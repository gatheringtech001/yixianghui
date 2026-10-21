export const DISPLAY_MODE_KEY = 'yxh-display-mode-v1'

export function readableRichText(html, senior) {
	if (!senior || !html) return html || ''
	return String(html).replace(/<(p|div|section|span|li|td|th|font|strong|b|em|i|a|blockquote|h[1-6])\b([^>]*)>/gi, (match, tag, attrs) => {
		const size = /^h[1-6]$/i.test(tag) ? 22 : 18
		const color = tag.toLowerCase() === 'a' ? '#701018' : '#333333'
		const readable = `font-size:${size}px!important;line-height:1.8!important;color:${color}!important;`
		if (/\bstyle\s*=/i.test(attrs)) {
			attrs = attrs.replace(/\bstyle\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))/i, (_, double, single, unquoted) => {
				const style = double === undefined ? single === undefined ? unquoted : single : double
				const quote = single === undefined ? '"' : "'"
				const rest = style.split(';').filter(declaration => !/^\s*(font-size|line-height|color)\s*:/i.test(declaration)).join(';')
				return `style=${quote}${rest};${readable}${quote}`
			})
		} else attrs += ` style="${readable}"`
		return `<${tag}${attrs}>`
	})
}

// Device display preference is deliberately independent of login and account caches.
export function createDisplayModePreference(storage) {
	const state = { mode: 'standard', chosen: false }
	function refresh() {
		try {
			const saved = storage.getStorageSync(DISPLAY_MODE_KEY)
			state.chosen = saved === 'standard' || saved === 'senior'
			state.mode = saved === 'senior' ? 'senior' : 'standard'
		} catch (_) { /* Storage may be unavailable; browsing must remain usable. */ }
	}
	function select(mode) {
		if (mode !== 'standard' && mode !== 'senior') return false
		try {
			storage.setStorageSync(DISPLAY_MODE_KEY, mode)
		} catch (_) {
			return false
		}
		state.mode = mode
		state.chosen = true
		return true
	}
	refresh()
	return { state, refresh, select }
}

let preference
export function installDisplayMode(Vue) {
	preference = createDisplayModePreference(uni)
	preference.state = Vue.observable(preference.state)
	Vue.mixin({
		methods: {
			displayRichText(html) { return readableRichText(html, this.isSeniorMode) }
		},
		computed: {
			isSeniorMode() { return preference.state.mode === 'senior' },
			displayModeChosen() { return preference.state.chosen },
			displayModeClass() { return this.isSeniorMode ? 'senior-mode' : '' },
			displayModeLabel() { return this.isSeniorMode ? '长辈模式' : '标准模式' }
		},
		watch: {
			isSeniorMode() {
				// Re-measure existing scroll containers without navigation or losing form state.
				this.$nextTick(() => {
					;['setScrollViewHeight', 'setBodyScrollHeight', 'setPageScrollHeight'].forEach(name => {
						if (typeof this[name] === 'function') this[name]()
					})
				})
			}
		}
	})
}

export function selectDisplayMode(mode) {
	return !!preference && preference.select(mode)
}
