const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

module.exports = function loadPage(file, dependencies = {}) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../shop-mnp', file), 'utf8')
  const script = source.match(/<script[^>]*>([\s\S]*?)<\/script>/)[1]
    .replace(/import\s+[\s\S]*?from\s+['"][^'"]+['"];?/g, '')
    .replace('export default', 'module.exports =')
  const context = {module:{exports:{}}, console, setTimeout, clearTimeout, ...dependencies}
  vm.runInNewContext(script, context, {filename:file})
  const component = context.module.exports
  const page = {$host:'https://example.invalid', ...(component.data?.call({$host:'https://example.invalid'}) || {})}
  for (const [key, method] of Object.entries(component.methods || {})) page[key] = method.bind(page)
  for (const [key, getter] of Object.entries(component.computed || {})) {
    if (typeof getter === 'function') Object.defineProperty(page, key, {get:getter.bind(page)})
  }
  return {page, component, source}
}
