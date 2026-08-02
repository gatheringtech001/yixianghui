export default class WxApi {
    // 获取设备信息
    static getSystemInfoSync() {
        return wx.getSystemInfoSync()
    }

    // 检测微信登录是否过期
    static checkSession() {
        return new Promise((resolve, reject) => {
            wx.checkSession({
                success(res) { // 未失效
                    resolve(res)
                },
                fail(err) { // 已失效
                    reject(err)
                }
            });
        })
    }

    // 获取微信的临时登录凭证
    static wxLogin() {
        return new Promise((resolve, reject) => {
            uni.login({
                provider: 'weixin',
                success(res) {
                    resolve(res)
                },
                fail(err) {
                    reject(err)
                }
            });
        })
    }

    // 获取微信用户信息
    static wxUserInfo() {
        return new Promise((resolve, reject) => {
            uni.getUserProfile({
                desc: '用于完善个人资料',
                lang: 'zh_CN',
                success(res) {
                    resolve(res)
                },
                fail(err) {
                    reject(err)
                }
            });
        })
    }
}
