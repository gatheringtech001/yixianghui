<template>
  <main class="guanjia-login">
    <section>
      <h1>微信登录</h1>
      <p role="status">{{ message }}</p>
      <el-button v-if="failed" type="primary" @click="start">重新微信登录</el-button>
      <router-link v-if="failed" to="/login">账号密码登录</router-link>
    </section>
  </main>
</template>
<script>
import { setToken } from '@/utils/auth'
export default {
  name: 'GuanjiaLogin',
  data() { return { message: '正在验证微信管理身份…', failed: false } },
  async created() {
    const assertion = new URLSearchParams(location.hash.slice(1)).get('assertion')
    history.replaceState(null, '', location.pathname)
    if (!assertion) { this.start(); return }
    try {
      // Do not use the normal request wrapper: its duplicate-submit cache stores bodies.
      const response = await fetch('/api/system/auth/guanjia/exchange', {
        method: 'POST', credentials: 'same-origin', cache: 'no-store',
        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ assertion })
      })
      const result = await response.json()
      if (!response.ok || result.code !== 200 || typeof result.token !== 'string') throw new Error(result.msg || '登录未完成')
      setToken(result.token)
      location.replace('/goods/app_goods')
    } catch (error) { this.message = error.message || '登录未完成，请重试'; this.failed = true }
  },
  methods: {
    start() { location.assign('/api/system/auth/guanjia/start') }
  }
}
</script>
<style scoped>
.guanjia-login { min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;background:#f5f7fa; }
.guanjia-login section { width:100%;max-width:400px;padding:32px;background:#fff;border-radius:12px; }
.guanjia-login h1 { font-size:24px; }
.guanjia-login p { line-height:1.7;overflow-wrap:anywhere; }
.guanjia-login a { display:block;margin-top:20px;color:#409eff; }
</style>
