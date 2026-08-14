import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
AUTH_CONTROLLER = (
    REPO_ROOT
    / "lankong-admin/src/main/java/com/ruoyi/web/controller/system/SysAuthController.java"
)
APP_USER_CONTROLLER = (
    REPO_ROOT
    / "lankong-admin/src/main/java/com/ruoyi/web/controller/app/AppUserController.java"
)


def method_source(source, signature):
    start = source.index(signature)
    body_start = source.index("{", start)
    depth = 0
    for index in range(body_start, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
    raise AssertionError(f"Unclosed method body: {signature}")


class WechatFirstLoginSessionTest(unittest.TestCase):
    def test_new_user_session_reloads_persisted_roles(self):
        source = AUTH_CONTROLLER.read_text(encoding="utf-8")
        method = method_source(
            source,
            "public AjaxResult socialLogin(@PathVariable(\"source\") String source",
        )

        insert_index = method.index("userService.insertUser(newUser);")
        reload_index = method.index(
            "SysUser sessionUser = userService.selectUserByUserName(newUser.getUserName());"
        )
        token_index = method.index(
            "new LoginUser(sessionUser.getUserId(), sessionUser.getDeptId(), "
            "sessionUser, permissionService.getMenuPermission(sessionUser))"
        )

        self.assertLess(insert_index, reload_index)
        self.assertLess(reload_index, token_index)

    def test_profile_endpoint_does_not_apply_admin_data_scope(self):
        source = APP_USER_CONTROLLER.read_text(encoding="utf-8")
        method = method_source(source, "public AjaxResult getInfo()")

        self.assertNotIn("checkUserDataScope", method)


if __name__ == "__main__":
    unittest.main()
