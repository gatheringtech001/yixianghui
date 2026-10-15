import importlib.util
import io
import unittest
from pathlib import Path
from PIL import Image

SCRIPTS = Path(__file__).resolve().parents[1]


def module(name):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS / (name + '.py'))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


class CatalogThumbnailTests(unittest.TestCase):
    def test_thumbnail_keeps_aspect_ratio_and_source_bytes(self):
        thumbnail = module('sync_catalog_thumbnails').thumbnail
        source = io.BytesIO()
        Image.new('RGB', (1800, 2400), 'red').save(source, 'PNG')
        data = source.getvalue()
        optimized = thumbnail(data)
        with Image.open(io.BytesIO(optimized)) as image:
            self.assertEqual(image.size, (720, 960))
            self.assertEqual(image.format, 'JPEG')
        self.assertEqual(source.getvalue(), data)

    def test_transparent_images_use_white_background(self):
        source = io.BytesIO()
        Image.new('RGBA', (50, 100), (0, 0, 0, 0)).save(source, 'PNG')
        data = module('sync_catalog_thumbnails').thumbnail(source.getvalue())
        with Image.open(io.BytesIO(data)) as image:
            self.assertEqual(image.size, (50, 100))
            self.assertTrue(all(channel >= 250 for channel in image.getpixel((0, 0))))

    def test_cache_route_is_narrow_idempotent_and_rejects_unknown_configuration(self):
        cache = module('deploy_catalog_thumbnail_cache')
        source = 'server {\n    server_name shzxj.lk01.cn;\n    location ^~ /api {\n proxy_pass http://localhost:8080;\n}\n}'
        result = cache.updated_config(source)
        self.assertIn('/api/profile/upload/catalog-thumbs-v1/', result)
        self.assertIn('immutable', result)
        self.assertEqual(cache.updated_config(result), result)
        self.assertEqual(result.replace(cache.BLOCK, ''), source)
        with self.assertRaises(RuntimeError):
            cache.updated_config('server_name other.example;')
        with self.assertRaises(RuntimeError):
            cache.updated_config(source + '\nlocation /api/profile/upload/catalog-thumbs-v1/ {}')


if __name__ == '__main__':
    unittest.main()
