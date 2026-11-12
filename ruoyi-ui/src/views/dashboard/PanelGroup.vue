<template>
  <div class="panel-group">
    <section v-for="card in cards" :key="card.title" class="metric-card">
      <div class="metric-title">{{ card.title }}<i :class="card.icon" /></div>
      <div class="metric-value" :class="{ money: card.money }">{{ loading ? '—' : formatMetric(card.value, card.money) }}<small v-if="card.money && !loading && card.value != null">元</small></div>
      <div class="metric-caption">{{ card.caption }}</div>
    </section>
  </div>
</template>
<script>
import { formatMetric, trafficMetric } from '@/utils/mini-dashboard.mjs'
export default {
  props: { totals: { type: Object, default: () => ({}) }, traffic: { type: Object, default: () => ({}) }, loading: Boolean },
  computed: {
    cards() {
      const trafficCaption = this.traffic.date ? `${this.traffic.date} · 微信统计` : '微信小程序数据分析'
      return [
        { title: '小程序访客', value: trafficMetric(this.traffic, 'visitors'), caption: trafficCaption, icon: 'el-icon-user' },
        { title: '浏览次数', value: trafficMetric(this.traffic, 'pageViews'), caption: trafficCaption, icon: 'el-icon-view' },
        { title: '累计收款', value: this.totals.receivedAmount, caption: '所选订单的成功付款金额 · 含已退款款项', money: true, icon: 'el-icon-wallet' },
        { title: '订单总数', value: this.totals.orderCount, caption: '商品订单 + 活动报名 · 含未付款', icon: 'el-icon-tickets' }
      ]
    }
  },
  methods: { formatMetric }
}
</script>
<style lang="scss" scoped>
.panel-group { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.metric-card { background: white; border: 1px solid #e8edf3; border-radius: 10px; padding: 20px; min-width: 0; }
.metric-title { font-size: 14px; color: #64748b; display: flex; justify-content: space-between; gap: 8px; i { color: #409eff; font-size: 18px; } }
.metric-value { font-size: 30px; line-height: 1.5; font-weight: 650; color: #172b4d; margin: 8px 0; overflow-wrap: anywhere; small { font-size: 12px; margin-left: 5px; font-weight: 400; } }
.metric-caption { color: #8a97a8; font-size: 12px; line-height: 1.6; }
@media (max-width: 1200px) { .panel-group { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 500px) { .panel-group { gap: 10px; } .metric-card { padding: 14px 12px; } .metric-value { font-size: 25px; } .metric-title { font-size: 13px; } }
</style>
