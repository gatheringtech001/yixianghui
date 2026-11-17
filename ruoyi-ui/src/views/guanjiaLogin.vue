<template>
  <main class="guanjia-login">
    <section>
      <h1>微信登录</h1>
      <p role="status">{{ message }}</p>
      <img v-if="image && !failed" :src="image" width="256" height="256" alt="使用管理员微信扫码登录" />
      <el-button :loading="loading" type="success" @click="start">{{ image || failed ? '刷新二维码' : '生成二维码' }}</el-button>
      <router-link to="/login">账号密码登录</router-link>
    </section>
  </main>
</template>
<script>
import QRCode from 'qrcode'
import { setToken } from '@/utils/auth'
const ROOT = '/api/system/auth/guanjia'
export default {
  name: 'GuanjiaLogin',
  data() { return { message: '正在生成二维码…', image: '', ticket: '', expires: 0, failed: false, loading: false, timer: null, generation: 0, disposed: false } },
  created() {
    // Old bridge assertions are deliberately ignored after cutover.
    const ticket = new URLSearchParams(location.search).get('ticket')
    if (location.hash || location.search) history.replaceState(null, '', location.pathname)
    if (ticket && /^[a-f0-9]{64}$/.test(ticket)) {
      this.ticket = ticket; this.expires = Date.now() + 300000; this.message = '正在登录…'
      this.poll(++this.generation)
    } else this.start()
  },
  beforeDestroy() {
    this.disposed = true; this.generation++; clearTimeout(this.timer)
    this.cancel(this.ticket)
  },
  methods: {
    async request(path, body) {
      const response = await fetch(ROOT + path, { method: 'POST', credentials: 'same-origin', cache: 'no-store',
        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
      const result = await response.json()
      if (!response.ok || result.code !== 200) throw new Error(result.msg || '微信登录暂不可用，请重试')
      return result
    },
    cancel(ticket) { if (ticket) return this.request('/cancel', { ticket }).catch(() => {}) },
    async start() {
      if (this.loading || this.disposed) return
      const generation = ++this.generation
      clearTimeout(this.timer); this.loading = true; this.failed = false; this.image = ''; this.message = '正在生成二维码…'
      try {
        await this.cancel(this.ticket); this.ticket = ''
        const result = await this.request('/qr', {})
        if (this.disposed || generation !== this.generation) { this.cancel(result.ticket); return }
        this.ticket = result.ticket; this.expires = result.expires
        if (/MicroMessenger/i.test(navigator.userAgent)) { location.assign(result.url); return }
        this.image = await QRCode.toDataURL(result.url, { width: 256, margin: 3, errorCorrectionLevel: 'M' })
        if (this.disposed || generation !== this.generation) return
        this.message = '请用管理员微信扫码，并在手机上确认'
        this.timer = setTimeout(() => this.poll(generation), 1500)
      } catch (error) { if (generation === this.generation) { this.failed = true; this.message = error.message } }
      finally { this.loading = false }
    },
    async poll(generation) {
      if (this.disposed || generation !== this.generation) return
      if (Date.now() >= this.expires) { this.failed = true; this.message = '二维码已过期，请刷新'; return }
      try {
        const result = await this.request('/poll', { ticket: this.ticket })
        if (this.disposed || generation !== this.generation) return
        if (result.status === 'signed-in' && typeof result.token === 'string' && result.token) {
          this.ticket = ''; setToken(result.token); location.replace('/index'); return
        }
        this.timer = setTimeout(() => this.poll(generation), 1500)
      } catch (error) { if (generation === this.generation) { this.failed = true; this.message = error.message } }
    }
  }
}
</script>
<style scoped>
.guanjia-login { min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;background:#f5f7fa;box-sizing:border-box; }
.guanjia-login section { width:100%;max-width:400px;padding:32px 20px;background:#fff;border-radius:12px;box-sizing:border-box;text-align:center; }
.guanjia-login h1 { font-size:24px;margin-top:0; }
.guanjia-login p { line-height:1.7;overflow-wrap:anywhere;color:#626b7b; }
.guanjia-login img { display:block;max-width:100%;height:auto;margin:8px auto 20px; }
.guanjia-login a { display:block;margin-top:20px;color:#409eff; }
</style>
