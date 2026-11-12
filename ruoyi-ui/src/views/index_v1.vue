<template>
  <div class="mini-dashboard">
    <header class="dashboard-header">
      <div><h2>小程序经营概览</h2><p>订单、收款与访问数据</p></div>
      <div class="dashboard-controls">
        <el-select v-model="period" size="small" aria-label="订单统计时间" @change="load">
          <el-option v-for="item in periods" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </header>
    <div v-if="error" class="load-error" role="alert">{{ error }}<el-button type="text" @click="load">重试</el-button></div>
    <panel-group :totals="totals" :traffic="traffic" :loading="loading" />
    <section class="traffic-note" :class="{ unavailable: traffic.status && traffic.status !== 'available' }">
      <template v-if="traffic.status === 'available'">
        <span><i class="el-icon-data-line" /> 昨日访问</span>
        <span>新访客 {{ formatMetric(traffic.newVisitors) }}</span>
        <span>打开次数 {{ formatMetric(traffic.openCount) }}</span>
        <span>人均停留 {{ formatMetric(traffic.averageStaySeconds, true) }} 秒</span>
      </template>
      <span v-else>{{ trafficLoading ? '正在读取微信访问数据…' : (traffic.reason || '访问数据暂不可用') }}。访问数据按微信已出具的昨日统计展示，不是实时在线人数。</span>
    </section>
    <section class="summary-grid">
      <div v-for="item in secondaryCards" :key="item.label" class="summary-item"><span>{{ item.label }}</span><strong>{{ loading ? '—' : formatMetric(totals[item.key], item.money) }}<small v-if="item.money"> 元</small></strong></div>
    </section>
    <section class="business-section">
      <div class="section-heading"><h3>业务分类</h3><span>{{ rangeLabel }}</span></div>
      <el-table :data="businesses" v-loading="loading" class="business-table" empty-text="暂无统计数据">
        <el-table-column prop="business" label="业务" min-width="110" />
        <el-table-column prop="orderCount" label="订单" min-width="70" />
        <el-table-column prop="paidCount" label="已付款" min-width="80" />
        <el-table-column prop="pendingCount" label="待付款" min-width="80" />
        <el-table-column label="累计收款（元）" min-width="130"><template slot-scope="scope">{{ formatMetric(scope.row.receivedAmount, true) }}</template></el-table-column>
        <el-table-column label="已退款（元）" min-width="120"><template slot-scope="scope">{{ formatMetric(scope.row.refundAmount, true) }}</template></el-table-column>
        <el-table-column label="净收款（元）" min-width="120"><template slot-scope="scope">{{ formatMetric(scope.row.netAmount, true) }}</template></el-table-column>
      </el-table>
      <div class="business-mobile">
        <article v-for="row in businesses" :key="row.business"><h4>{{ row.business }}<span>{{ row.orderCount }} 单</span></h4><div class="mobile-counts"><span>已付款 {{ row.paidCount }}</span><span>待付款 {{ row.pendingCount }}</span></div><div class="mobile-money"><span>收款<strong>¥{{ formatMetric(row.receivedAmount, true) }}</strong></span><span>退款<strong>¥{{ formatMetric(row.refundAmount, true) }}</strong></span><span>净收款<strong>¥{{ formatMetric(row.netAmount, true) }}</strong></span></div></article>
      </div>
    </section>
    <div v-if="warnings.length" class="data-warning" role="status">{{ warnings.join('；') }}。历史数据缺失不补造。</div>
    <footer class="data-source">
      <p>按下单日期筛选，金额为这些订单当前累计成功收款、成功退款及两者差额，不是该期间现金流水。商品仅统计小程序来源，活动统计现有报名订单；不含充值、会员卡及线下导入订单。</p>
      <p v-if="updatedAt">更新于 {{ updatedAt }} · 访问数据来自微信，订单与退款来自业务系统</p>
    </footer>
  </div>
</template>
<script>
import PanelGroup from './dashboard/PanelGroup'
import { getUserStaticCount, getMiniProgramTraffic } from '@/api/monitor/online'
import { formatMetric, businessRows } from '@/utils/mini-dashboard.mjs'
export default {
  name: 'Index',
  components: { PanelGroup },
  data() {
    return {
      period: '30d', periods: [{ label: '今日下单', value: 'today' }, { label: '昨日下单', value: 'yesterday' }, { label: '近7天下单', value: '7d' }, { label: '近30天下单', value: '30d' }, { label: '全部订单', value: 'all' }],
      loading: false, trafficLoading: false, error: '', totals: {}, traffic: {}, businesses: [], startDate: null, endDate: null, updatedAt: '', requestId: 0,
      secondaryCards: [{ label: '已付款订单', key: 'paidCount' }, { label: '待付款订单', key: 'pendingCount' }, { label: '已取消订单', key: 'cancelledCount' }, { label: '已退款订单', key: 'refundedCount' }, { label: '成功退款', key: 'refundAmount', money: true }, { label: '净收款', key: 'netAmount', money: true }]
    }
  },
  computed: {
    rangeLabel() { return this.startDate ? `${this.startDate} 至 ${this.endDate}` : (this.period === 'all' ? '全部订单' : '') },
    warnings() {
      return [['unknownStatusCount', '单付款状态待核对'], ['missingPayTimeCount', '单缺少付款时间'], ['missingRefundCount', '单已退款但缺少成功退款记录']].filter(([key]) => Number(this.totals[key]) > 0).map(([key, text]) => `${this.totals[key]} ${text}`)
    }
  },
  mounted() { this.load() },
  beforeDestroy() { this.requestId++ },
  methods: {
    formatMetric,
    async loadTraffic(request) {
      this.trafficLoading = true
      try {
        const response = await getMiniProgramTraffic()
        if (request === this.requestId) this.traffic = response.data || { status: 'unavailable', reason: '微信访问数据暂不可用' }
      } catch (_) {
        if (request === this.requestId) this.traffic = { status: 'unavailable', reason: '微信访问数据读取失败，请稍后刷新' }
      } finally { if (request === this.requestId) this.trafficLoading = false }
    },
    async load() {
      const request = ++this.requestId
      this.loading = true; this.error = ''; this.totals = {}; this.traffic = {}; this.businesses = []; this.updatedAt = ''; this.startDate = null; this.endDate = null
      this.loadTraffic(request)
      try {
        const response = await getUserStaticCount({ period: this.period })
        if (request !== this.requestId) return
        const data = response.data
        if (!data || !data.totals || !Array.isArray(data.businesses)) throw new Error('invalid dashboard')
        this.totals = data.totals; this.businesses = businessRows(data.businesses); this.startDate = data.startDate; this.endDate = data.endDate
        this.updatedAt = data.updatedAt ? new Date(data.updatedAt).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false }) : ''
      } catch (_) {
        if (request === this.requestId) this.error = '统计数据读取失败，请重试。'
      } finally { if (request === this.requestId) this.loading = false }
    }
  }
}
</script>
<style lang="scss" scoped>
.mini-dashboard { padding: 24px; background: #f5f7fb; min-height: calc(100vh - 84px); min-width: 0; color: #243650; }
.dashboard-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; margin-bottom: 20px; h2 { font-size: 22px; margin: 0 0 8px; font-weight: 600; } p { font-size: 13px; color: #8390a3; margin: 0; } }
.dashboard-controls { display: flex; gap: 10px; .el-select { width: 145px; } }
.traffic-note { display: flex; flex-wrap: wrap; gap: 10px 24px; padding: 16px 2px; color: #69798f; font-size: 12px; line-height: 1.8; &.unavailable { color: #936c22; } }
.summary-grid { display: grid; grid-template-columns: repeat(6,minmax(0,1fr)); gap: 12px; margin: 0 0 20px; }
.summary-item { border-radius: 8px; background: #fff; padding: 16px; min-width: 0; span { color: #7b8a9e; font-size: 12px; display: block; } strong { display: block; margin-top: 12px; font-size: 20px; font-weight: 600; overflow-wrap: anywhere; } small { font-size: 11px; font-weight: 400; } }
.business-section { border: 1px solid #e8edf3; background: #fff; padding: 20px; border-radius: 10px; min-width: 0; }
.section-heading { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; h3 { margin: 0; font-size: 16px; font-weight: 600; } span { font-size: 12px; color: #8390a3; } }
.business-table { width: 100%; }
.business-mobile { display: none; }
.data-source { font-size: 12px; line-height: 1.8; color: #8793a4; margin-top: 18px; p { margin: 5px 0; } }
.data-warning, .load-error { font-size: 12px; padding: 12px 16px; margin: 16px 0; border-radius: 6px; background: #fff7e8; color: #996b21; line-height: 1.8; }
.load-error { background: #fff1f0; color: #bf4545; }
@media(max-width: 1200px) { .summary-grid { grid-template-columns: repeat(3,minmax(0,1fr)); } }
@media(max-width: 600px) {
  .mini-dashboard { padding: 16px 12px; } .dashboard-header h2 { font-size: 20px; } .dashboard-controls { width: 100%; .el-select { flex: 1; width: auto; } }
  .summary-grid { grid-template-columns: repeat(2,minmax(0,1fr)); gap: 10px; } .summary-item { padding: 14px 12px; }
  .business-section { padding: 16px 12px; } .business-table { display: none; } .business-mobile { display: block; article { border-top: 1px solid #edf1f6; padding: 16px 0; } h4 { display: flex; justify-content: space-between; margin: 0 0 12px; font-size: 14px; span { font-weight: 400; color: #8390a3; } } }
  .mobile-counts { display: flex; gap: 18px; font-size: 12px; color: #8390a3; margin-bottom: 12px; }
  .mobile-money { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 8px; font-size: 11px; color: #8390a3; strong { display: block; font-size: 12px; font-weight: 500; color: #243650; margin-top: 6px; overflow-wrap: anywhere; } }
}
</style>
