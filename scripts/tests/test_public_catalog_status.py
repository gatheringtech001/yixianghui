import unittest
from pathlib import Path


CONTROLLER = (
    Path(__file__).resolve().parents[2]
    / "lankong-admin/src/main/java/com/ruoyi/web/controller/app/AppIndexController.java"
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


class PublicCatalogStatusTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = CONTROLLER.read_text(encoding="utf-8")

    def test_public_goods_lists_force_published_status(self):
        self.assertIn('private static final String PUBLISHED_STATUS = "1";', self.source)
        for signature in (
            "public AjaxResult goodsList(AppGoods appGoods)",
            "public AjaxResult queryGoodsList(@RequestBody AppGoods appGoods)",
        ):
            method = method_source(self.source, signature)
            self.assertIn("appGoods.setStatus(PUBLISHED_STATUS);", method)

    def test_public_activity_list_forces_published_status(self):
        method = method_source(
            self.source,
            "public TableDataInfo activity_list(AppActivity appActivity)",
        )
        self.assertIn("appActivity.setStatus(PUBLISHED_STATUS);", method)


if __name__ == "__main__":
    unittest.main()
