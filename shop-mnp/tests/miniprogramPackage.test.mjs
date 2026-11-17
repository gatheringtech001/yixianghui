import assert from 'node:assert/strict'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import test from 'node:test'

const root = new URL('../', import.meta.url)

test('unused legacy brand mark stays out of the constrained WeChat main package', () => {
    const manifest = JSON.parse(readFileSync(new URL('manifest.json', root), 'utf8'))
    const asset = 'static/home-design/brand-mark.png'
    assert.ok(manifest['mp-weixin'].packOptions.ignore.some(item => item.type === 'file' && item.value === asset))
    assert.ok(statSync(new URL(asset, root)).size > 9 * 1024)
    function check(directory) {
        for (const entry of readdirSync(directory, { withFileTypes: true })) {
            const path = join(directory, entry.name)
            if (entry.isDirectory()) check(path)
            else if (/\.(vue|js|scss|css|json)$/.test(entry.name)) {
                assert.ok(!readFileSync(path, 'utf8').includes('brand-mark'), `Excluded asset is referenced by ${path}`)
            }
        }
    }
    for (const directory of ['pages', 'packagesMall', 'packagesMember', 'packagesPublic', 'components', 'utils', 'api']) {
        check(new URL(directory, root).pathname)
    }
})
