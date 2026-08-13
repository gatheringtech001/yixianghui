import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
AUTH_CONTROLLER = (
    REPO_ROOT
    / "lankong-admin/src/main/java/com/ruoyi/web/controller/system/SysAuthController.java"
)
WECHAT_SERVICE = (
    REPO_ROOT
    / "ruoyi-system/src/main/java/com/ruoyi/system/service/impl/WeChatMiniProgramServiceImpl.java"
)


class WechatAuthConfigTest(unittest.TestCase):
    def test_social_login_uses_the_shared_mini_program_config(self):
        controller = AUTH_CONTROLLER.read_text(encoding="utf-8")
        service = WECHAT_SERVICE.read_text(encoding="utf-8")

        app_id_config = '@Value("${wx.mnp.appId:${wx.pay.appId:}}")'
        app_secret_config = '@Value("${wx.mnp.appSecret:}")'

        self.assertIn(app_id_config, service)
        self.assertIn(app_secret_config, service)
        self.assertIn(app_id_config, controller)
        self.assertIn(app_secret_config, controller)
        self.assertNotIn("${weixin.appid:}", controller)
        self.assertNotIn("${weixin.appsecret:}", controller)


if __name__ == "__main__":
    unittest.main()
