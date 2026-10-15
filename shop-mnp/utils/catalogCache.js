// Only public browsing lists use this cache. Details, stock and checkout stay live.
export function createCatalogCache({ ttl = 60000, maxEntries = 64, now = Date.now } = {}) {
	const entries = new Map()
	const clone = value => JSON.parse(JSON.stringify(value))
	return {
		clear() { entries.clear() },
		async get(key, loader) {
			const hit = entries.get(key)
			if (hit && hit.pending) return clone(await hit.pending)
			if (hit && hit.expires > now()) return clone(hit.value)
			const entry = { pending: null }
			const pending = Promise.resolve().then(loader).then(value => {
				if (entries.get(key) === entry) {
					entry.value = clone(value)
					entry.expires = now() + ttl
					entry.pending = null
				}
				return value
			}).catch(error => {
				if (entries.get(key) === entry) entries.delete(key)
				throw error
			})
			entry.pending = pending
			entries.delete(key)
			entries.set(key, entry)
			while (entries.size > maxEntries) entries.delete(entries.keys().next().value)
			return clone(await pending)
		}
	}
}
