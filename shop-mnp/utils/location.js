/**
 * 微信小程序定位服务
 * 支持获取当前位置经纬度及地址信息
 */
export default class LocationService {
  // 检查并获取位置授权
  static async authorizeLocation() {
    return new Promise((resolve, reject) => {
      // 检查授权状态
      uni.getSetting({
        success: (res) => {
          if (res.authSetting['scope.userLocation']) {
            // 已授权，直接返回
            resolve(true);
          } else if (res.authSetting['scope.userLocation'] === undefined) {
            // 未授权，请求授权
            uni.authorize({
              scope: 'scope.userLocation',
              success: () => resolve(true),
              fail: (err) => reject(err)
            });
          } else {
            // 拒绝过授权，引导用户打开设置
            uni.showModal({
              title: '需要位置权限',
              content: '请在设置中打开位置权限以获取您的位置',
              success: (res) => {
                if (res.confirm) {
                  uni.openSetting({
                    success: (res) => {
                      resolve(res.authSetting['scope.userLocation']);
                    }
                  });
                } else {
                  reject(new Error('用户拒绝授予位置权限'));
                }
              }
            });
          }
        },
        fail: (err) => reject(err)
      });
    });
  }

  // 获取当前位置经纬度
  static async getLocation() {
    try {
      await this.authorizeLocation();
      return new Promise((resolve, reject) => {
        uni.getLocation({
          type: 'wgs84', // 返回 gps 坐标
          success: (res) => {
            resolve({
              latitude: res.latitude,
              longitude: res.longitude
            });
          },
          fail: (err) => reject(err)
        });
      });
    } catch (error) {
      throw new Error(`获取位置失败: ${error.message}`);
    }
  }
}