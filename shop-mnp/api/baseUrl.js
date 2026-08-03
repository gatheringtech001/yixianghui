const API_HOST = {
	development: 'http://127.0.0.1:18080/api',
	production: 'https://shzxj.lk01.cn/api'
}

const mode = process.env.NODE_ENV === 'production' ? 'production' : 'development'
const host = process.env.VUE_APP_API_BASE_URL || API_HOST[mode]

export default {
	publicUrl: host + '/'
}
