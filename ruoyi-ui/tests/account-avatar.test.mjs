import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const Vue = require('vue');
const compiler = require('vue-template-compiler');
const source = readFileSync(new URL('../src/components/AccountAvatar/index.vue', import.meta.url), 'utf8');
const sfc = compiler.parseComponent(source);
const compiled = compiler.compile(sfc.template.content);
const options = new Function(sfc.script.content.replace('export default', 'return'))();
const Avatar = Vue.extend({ ...options, render: new Function(compiled.render), staticRenderFns: compiled.staticRenderFns.map(s => new Function(s)) });
const renderChild = vm => vm._render().children.find(node => node.tag);

test('missing avatar renders an accessible local fallback without image requests', () => {
  assert.deepEqual(compiled.errors, []);
  for (const src of ['', undefined]) {
    const vm = new Avatar({ propsData: { src } });
    const child = renderChild(vm);
    assert.equal(child.tag, 'span');
    assert.equal(child.data.attrs.role, 'img');
    assert.equal(child.data.attrs['aria-label'], '默认头像');
    vm.$destroy();
  }
});
test('valid avatar is preserved; load error replaces the image rather than retrying a broken URL', () => {
  const vm = new Avatar({ propsData: { src: '/api/profile/avatar/valid.png' } });
  const child = renderChild(vm);
  assert.equal(child.tag, 'img');
  assert.equal(child.data.attrs.src, '/api/profile/avatar/valid.png');
  child.data.on.error();
  assert.equal(renderChild(vm).tag, 'span');
  assert.equal(vm.failed, true);
  vm.$destroy();
});
test('new avatar after a failed request retries the new source and may fall back again', async () => {
  const vm = new Avatar({ propsData: { src: '/missing.png' } });
  renderChild(vm).data.on.error();
  vm.src = '/new.png';
  await Vue.nextTick();
  assert.equal(vm.failed, false);
  assert.equal(renderChild(vm).data.attrs.src, '/new.png');
  renderChild(vm).data.on.error();
  vm.src = '';
  await Vue.nextTick();
  assert.equal(renderChild(vm).data.attrs['aria-label'], '默认头像');
  vm.$destroy();
});
