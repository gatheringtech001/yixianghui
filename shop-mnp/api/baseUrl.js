// 环境切换：local 本地联调 | prod 线上
const ENV = 'prod'

const API_HOST = {
	local: 'http://192.168.1.7:8080/api',
	prod: 'https://shzxj.lk01.cn/api'
}

const host = API_HOST[ENV] || API_HOST.prod

export default {
	publicUrl: host + '/'
}
