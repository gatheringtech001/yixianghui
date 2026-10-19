"""Narrow ARM transport for the existing knowledge publisher when az is absent.

Credentials stay in process memory and are freshly read from the personal vault.
Only the existing gatheringtech knowledge-publication command is allowed.
"""
import json
import re
import sys
import urllib.parse
import urllib.request
from pathlib import Path

SUB = '3475925c-a76c-4904-850e-8071a6bed970'
TENANT = 'a1418383-b34c-47cb-b2bf-f2f32ddaf5d8'
CLIENT = '3877704f-0f88-4b15-927a-dd827b07fa61'
URL = ('https://management.azure.com/subscriptions/' + SUB +
       '/resourceGroups/gatheringtech/providers/Microsoft.Compute/virtualMachines/'
       'gatheringtech/runCommands/yxh-media-sync?api-version=2024-11-01')


def main():
    request = json.load(sys.stdin)
    if request.get('method') not in ('get', 'put') or request.get('url') not in (URL, URL + '&$expand=instanceView'):
        raise RuntimeError('ARM command is outside knowledge publication scope')
    note = Path('/Users/kevin/Documents/Obsidian Vault/Bitwarden/Items/Logins/Azure SP - kevinDigital (openclaw-sp).md').read_text()
    blocks = re.findall(r'```json\s*(.*?)\s*```', note, re.S)
    item = json.loads(blocks[-1])
    fields = {row['name']: row['value'] for row in item['fields']}
    if (item['login']['password'], fields['tenant_id'], fields['subscription_id']) != (CLIENT, TENANT, SUB):
        raise RuntimeError('ARM identity mismatch')
    body = urllib.parse.urlencode({'grant_type': 'client_credentials', 'client_id': CLIENT,
        'client_secret': fields['client_secret'], 'scope': 'https://management.azure.com/.default'}).encode()
    with urllib.request.urlopen(urllib.request.Request('https://login.microsoftonline.com/' + TENANT + '/oauth2/v2.0/token', data=body), timeout=40) as response:
        token = json.load(response)['access_token']
    payload = request.get('body')
    data = None if payload is None else payload.encode()
    query = urllib.request.Request(request['url'], method=request['method'].upper(), data=data,
        headers={'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json'})
    with urllib.request.urlopen(query, timeout=80) as response:
        output = response.read().decode()
    if request.get('output') != 'none':
        # Protected transport parameters must never reach logs or the caller.
        result = json.loads(output) if output else {}
        result.get('properties', {}).pop('protectedParameters', None)
        print(json.dumps(result))


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print('Knowledge ARM transport failed: ' + type(error).__name__, file=sys.stderr)
        sys.exit(1)
