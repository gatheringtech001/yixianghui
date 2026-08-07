const defaultHost = process.env.NODE_ENV === 'production'
	? 'https://shzxj.lk01.cn/api'
	: 'http://127.0.0.1:18080/api'
const host = process.env.VUE_APP_API_BASE_URL || defaultHost

export default {
	publicUrl: host + '/'
}
