#!/usr/bin/env python3
"""Build immutable card thumbnails; never modify source images or database rows.

Run before a mini-program release with Pillow installed:
  python3 scripts/sync_catalog_thumbnails.py --publish
Unknown/new cover URLs remain usable via the frontend's original-image fallback.
"""
import argparse
import hashlib
import io
import json
import shlex
import sys
import tarfile
import tempfile
import urllib.parse
import threading
import http.client
import ssl
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
HOST = 'https://shzxj.lk01.cn/api'
PUBLIC_DIR = '/profile/upload/catalog-thumbs-v1'
REMOTE_DIR = '/home/lk-shzxj/uploadPath/upload/catalog-thumbs-v1'
Image.MAX_IMAGE_PIXELS = 24_000_000
SESSIONS = threading.local()


def request(method, route, body=None):
    # Reuse TLS connections per worker instead of negotiating one per image.
    for attempt in range(3):
        if not hasattr(SESSIONS, 'client'):
            SESSIONS.client = http.client.HTTPSConnection('shzxj.lk01.cn', timeout=30,
                                                        context=ssl.create_default_context())
        try:
            payload = None if body is None else json.dumps(body).encode()
            SESSIONS.client.request(method, '/api' + urllib.parse.quote(route, safe='/?=&'),
                                    payload, {'Content-Type': 'application/json'})
            response = SESSIONS.client.getresponse()
            if response.status in (502, 503, 504):
                response.read()
                raise ConnectionError('Transient HTTP ' + str(response.status))
            if response.status != 200:
                response.read()
                raise RuntimeError('HTTP ' + str(response.status) + ': ' + route)
            return response
        except (OSError, http.client.HTTPException):
            SESSIONS.client.close()
            del SESSIONS.client
            if attempt == 2:
                raise


def request_json(route, body=None):
    response = request('GET' if body is None else 'POST', route, body)
    result = json.loads(response.read())
    if result.get('code') != 200:
        raise RuntimeError('Public catalog request failed: ' + route)
    return result


def public_covers():
    paths = set()
    categories = request_json('/mnp/index/get_goods_category?status=1')['data']
    for row in categories:
        if row.get('parentId') == 0 and row['categoryName'].strip() in ('全国旅居', '老年教育'):
            goods = request_json('/mnp/index/queryGoodsList', {'categoryId': row['categoryId']})['data']
            for item in goods:
                paths.add(item.get('goodsCover') or '')
                paths.add((item.get('goodsImages') or '').split(',')[0])
    for page in range(1, 21):
        result = request_json(f'/mnp/index/activity_list?status=1&pageNum={page}&pageSize=10')
        for item in result.get('rows', []):
            paths.add(item.get('activityCover') or '')
        if page * 10 >= result.get('total', 0):
            break
    for item in request_json('/mnp/index/get_ad_content_list?positionId=6').get('data', []):
        paths.add(item.get('adImage') or '')
    # No remote third-party downloads or private avatars/QR assets.
    return sorted(path for path in paths if path.startswith('/profile/upload/')
                  and '..' not in path and '?' not in path and '#' not in path
                  and path.lower().endswith(('.jpg', '.jpeg', '.png')))


def thumbnail(data):
    with Image.open(io.BytesIO(data)) as image:
        image = ImageOps.exif_transpose(image)
        image.thumbnail((720, 1080), Image.Resampling.LANCZOS)
        rgb = Image.new('RGB', image.size, 'white')
        rgba = image.convert('RGBA')
        rgb.paste(rgba, mask=rgba.getchannel('A'))
        output = io.BytesIO()
        rgb.save(output, 'JPEG', quality=78, optimize=True, progressive=True)
        return output.getvalue()


def build_cover(path):
    with request('GET', path) as response:
        if not response.headers.get('Content-Type', '').startswith('image/'):
            raise RuntimeError('Not an image: ' + path)
        data = response.read(10_000_001)
    if len(data) > 10_000_000:
        raise RuntimeError('Source image exceeds limit: ' + path)
    result = thumbnail(data)
    if len(result) >= len(data):
        return path, None, None, len(data)
    return path, hashlib.sha256(result).hexdigest() + '.jpg', result, len(data)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publish', action='store_true', help='Upload only new immutable thumbnails and update release mapping')
    args = parser.parse_args()
    rows = list(ThreadPoolExecutor(max_workers=3).map(build_cover, public_covers()))
    mapping = {source: PUBLIC_DIR + '/' + name for source, name, data, size in rows if name}
    assets = {name: data for source, name, data, size in rows if name}
    original = sum(size for source, name, data, size in rows if name)
    optimized = sum(len(data) for source, name, data, size in rows if name)
    print(f'covers={len(rows)} optimized={len(mapping)} bytes={original}->{optimized}')
    if not args.publish:
        return
    sys.path.insert(0, str(ROOT / 'codex-skills/manage-yixianghui/scripts'))
    from yxh_runtime import _ssh_script
    with tempfile.TemporaryDirectory(prefix='yxh-thumbs-') as scratch:
        archive = Path(scratch) / 'thumbs.tar'
        with tarfile.open(archive, 'w') as tar:
            for name, data in assets.items():
                info = tarfile.TarInfo(name)
                info.size, info.mode = len(data), 0o644
                tar.addfile(info, io.BytesIO(data))
        # --keep-old-files prevents overwriting even these content-addressed assets.
        _ssh_script(f'install -d -m 0755 {shlex.quote(REMOTE_DIR)}; '
                    f'tar --skip-old-files -xf - -C {shlex.quote(REMOTE_DIR)}', input_bytes=archive.read_bytes())
    checks = '\n'.join(f'{name[:-4]}  {REMOTE_DIR}/{name}' for name in assets)
    _ssh_script('sha256sum -c --quiet -', input_bytes=checks.encode())
    def verify(name):
        with request('HEAD', PUBLIC_DIR + '/' + name) as response:
            response.read()
            if response.headers.get('Content-Type', '').split(';')[0] != 'image/jpeg':
                raise RuntimeError('Published thumbnail is not served as JPEG')
            if int(response.headers.get('Content-Length', -1)) != len(assets[name]):
                raise RuntimeError('Published thumbnail size mismatch')
    list(ThreadPoolExecutor(max_workers=3).map(verify, assets))
    # Generated runtime mapping, not a report. Only written after remote verification.
    target = ROOT / 'shop-mnp/utils/catalogThumbnails.js'
    target.write_text('// Generated by scripts/sync_catalog_thumbnails.py; immutable card images only.\n'
                      + 'export default ' + json.dumps(mapping, ensure_ascii=False, indent=2) + '\n')
    print(f'Published and verified {len(assets)} immutable assets; mapping updated')


if __name__ == '__main__':
    main()
