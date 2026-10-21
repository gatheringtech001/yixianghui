// All account-specific caches must expire together; public browsing preferences stay intact.
export function clearAuthStorage() {
	;['token', 'userInfo', 'userData', 'userCard', 'consultant', 'setTokenTime'].forEach(key => {
		uni.removeStorageSync(key)
	})
}
