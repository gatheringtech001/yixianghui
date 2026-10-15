#!/usr/bin/env python3
"""Enable caching only for immutable, public catalog thumbnails (not originals/QRs)."""
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = '/www/server/nginx/conf/nginx.conf'
BLOCK = '''    # yxh immutable catalog card thumbnails; originals and QR config stay unchanged
    location ^~ /api/profile/upload/catalog-thumbs-v1/ {
        alias /home/lk-shzxj/uploadPath/upload/catalog-thumbs-v1/;
        autoindex off;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }

'''


def updated_config(source):
    if '/api/profile/upload/catalog-thumbs-v1/' in source:
        if BLOCK.strip() not in source:
            raise RuntimeError('Unexpected existing thumbnail configuration')
        return source
    marker = re.search(r'server_name\s+shzxj\.lk01\.cn\s*;', source)
    if not marker or len(re.findall(r'server_name\s+shzxj\.lk01\.cn\s*;', source)) != 1:
        raise RuntimeError('HTTPS host configuration is not uniquely identifiable')
    position = source.find('    location ^~ /api {', marker.end())
    if position < 0 or re.search(r'\bserver\s*\{', source[marker.end():position]):
        raise RuntimeError('Expected API location not found in target server')
    return source[:position] + BLOCK + source[position:]


def main():
    sys.path.insert(0, str(ROOT / 'codex-skills/manage-yixianghui/scripts'))
    from yxh_runtime import _ssh_script
    original = _ssh_script('test -d /home/lk-shzxj/uploadPath/upload/catalog-thumbs-v1 && '
                           'cat ' + CONFIG).stdout
    updated = updated_config(original.decode()).encode()
    if updated == original:
        print('Thumbnail cache configuration already present')
        return
    payload = json.dumps({'sha': hashlib.sha256(original).hexdigest(),
                          'original': original.decode(), 'updated': updated.decode()}).encode()
    remote = r'''
import hashlib,json,os,subprocess,sys,tempfile
from pathlib import Path
config=Path('/www/server/nginx/conf/nginx.conf')
data=json.load(sys.stdin)
if hashlib.sha256(config.read_bytes()).hexdigest()!=data['sha']:
    raise RuntimeError('Nginx config changed during deployment; stopped')
def replace(text):
    mode=config.stat().st_mode & 0o777
    fd,name=tempfile.mkstemp(prefix='.yxh-nginx-',dir=str(config.parent))
    try:
        with os.fdopen(fd,'w') as f: f.write(text)
        os.chmod(name,mode)
        os.replace(name,config)
    finally:
        if os.path.exists(name): os.unlink(name)
replace(data['updated'])
try:
    subprocess.run(['nginx','-t'],check=True)
    subprocess.run(['nginx','-s','reload'],check=True)
except Exception:
    if config.read_text()!=data['updated']:
        raise RuntimeError('Concurrent config edit; automatic rollback stopped')
    replace(data['original'])
    subprocess.run(['nginx','-t'],check=True)
    subprocess.run(['nginx','-s','reload'],check=True)
    raise
print('Immutable thumbnail cache enabled; original image/API routes unchanged')
'''
    import shlex
    result = _ssh_script('python3 -c ' + shlex.quote(remote), input_bytes=payload)
    print(result.stdout.decode().strip())


if __name__ == '__main__':
    main()
