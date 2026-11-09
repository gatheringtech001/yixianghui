<template>
  <el-drawer title="聚英 AI" :visible.sync="visible" size="min(440px, 100vw)" append-to-body custom-class="guanjia-drawer">
    <div class="guanjia-content">
      <section v-if="context.id" class="guanjia-current">
        <span>{{ context.type === 'activity' ? '当前活动' : '当前商品' }} · #{{ context.id }}</span>
        <h3>{{ context.name }}</h3>
        <a v-if="canEdit" :href="link({task:'edit',type:context.type,id:context.id})" class="guanjia-action primary"><i class="el-icon-magic-stick" /> AI 修改现有内容 <i class="el-icon-right" /></a>
        <el-button v-if="canExportRoster" plain icon="el-icon-download" :loading="exporting" @click="exportRoster">导出该{{ context.type === 'activity' ? '活动' : '商品' }}报名名单</el-button>
      </section>
      <h3>智能上架</h3>
      <div class="guanjia-grid">
        <a v-for="item in listingKinds" :key="item.kind" :href="link({task:'create',kind:item.kind})" class="guanjia-tile"><i :class="item.icon" /><strong>{{ item.label }}</strong></a>
      </div>
      <p class="guanjia-hint">在聚英管家上传图片或文字，整理资料后统一核对。</p>
      <h3>业务与内容助手</h3>
      <div class="guanjia-grid">
        <a v-for="item in assistants" :key="item.scope" :href="link({task:'assistant',scope:item.scope})" class="guanjia-tile"><strong>{{ item.label }}</strong><small>{{ item.hint }}</small></a>
      </div>
      <div class="guanjia-shortcuts">
        <a :href="link({task:'page',page:'orders'})">订单、报名与名单 <i class="el-icon-right" /></a>
        <a :href="link({task:'page',page:'approvals'})">审批与待办 <i class="el-icon-right" /></a>
        <a :href="link({task:'page',page:'notifications'})">付款与报名通知 <i class="el-icon-right" /></a>
      </div>
      <small class="guanjia-hint">复用聚英管家登录与权限；返回后台可继续当前操作。</small>
    </div>
  </el-drawer>
</template>

<script>
import { checkPermi } from '@/utils/permission'
import { guanjiaUrl, rosterTarget } from '@/utils/guanjia.mjs'
export default {
  name: 'GuanjiaAssistant',
  data() {
    return {
      visible: false, exporting: false, context: {},
      listingKinds: [
        { kind: 'hotel', label: '旅居', icon: 'el-icon-house' },
        { kind: 'activity', label: '活动', icon: 'el-icon-date' },
        { kind: 'education', label: '课程', icon: 'el-icon-reading' },
        { kind: 'online', label: '商品', icon: 'el-icon-goods' }
      ],
      assistants: [
        { scope: 'all', label: '总助手', hint: '跨业务协作' },
        { scope: 'mini', label: '管理助手', hint: '查数据、改资料' },
        { scope: 'wechat', label: '公众号', hint: '撰写与排版' },
        { scope: 'creative', label: '创作中心', hint: '图片与视频' },
        { scope: 'knowledge', label: '企业知识', hint: '资料与问答' },
        { scope: 'finance', label: '财务助手', hint: '核算与报表' }
      ]
    }
  },
  computed: {
    canEdit() { return this.context.id && checkPermi(['system:app_' + this.context.type + ':edit']) },
    canExportRoster() { return this.context.id && checkPermi([rosterTarget(this.context).permission]) },
    from() {
      if (this.context.type) return this.context.type
      const path = this.$route.path
      if (path.includes('app_activity_order')) return 'activity-order'
      if (path.includes('app_goods_order')) return 'goods-order'
      if (path.includes('app_activity')) return 'activity'
      return path.includes('app_goods') ? 'goods' : 'home'
    }
  },
  mounted() { this.$root.$on('guanjia:open', this.open) },
  beforeDestroy() { this.$root.$off('guanjia:open', this.open) },
  methods: {
    open(context = {}) { this.context = context; this.visible = true },
    link(intent) { return guanjiaUrl({ ...intent, from: this.from }) },
    async exportRoster() {
      if (this.exporting || !this.canExportRoster) return
      this.exporting = true
      try {
        const target = rosterTarget(this.context)
        await this.download(target.url, target.query, '报名名单_' + this.context.id + '.xlsx')
      } finally { this.exporting = false }
    }
  }
}
</script>

<style scoped>
.guanjia-content{padding:0 20px 28px;color:#303133;overflow-wrap:anywhere;box-sizing:border-box;min-width:0}
.guanjia-content h3{margin:20px 0 12px;font-size:15px;line-height:1.6}
.guanjia-current{padding:16px;border:1px solid #d9e7fa;border-radius:8px;background:#f5f9ff}
.guanjia-current>span,.guanjia-hint{color:#757d8a;font-size:12px;line-height:1.7}
.guanjia-current h3{margin:8px 0 16px}.guanjia-current .el-button{margin-top:10px;width:100%;white-space:normal}
.guanjia-action{display:flex;align-items:center;justify-content:center;gap:8px;padding:13px;border-radius:4px;color:#fff;background:#409eff;font-size:14px}
.guanjia-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
.guanjia-tile{padding:14px;border:1px solid #e4e7ed;border-radius:6px;display:flex;flex-wrap:wrap;gap:8px;align-items:center;min-width:0;font-size:14px;background:#fff}
.guanjia-tile:hover{border-color:#409eff;color:#409eff}.guanjia-tile small{font-size:12px;color:#909399;flex-basis:100%}.guanjia-shortcuts{margin:20px 0}
.guanjia-shortcuts a{display:flex;justify-content:space-between;padding:15px 0;border-bottom:1px solid #ebeef5;font-size:14px}
</style>
<style>
.guanjia-drawer{max-width:100vw}.guanjia-drawer .el-drawer__body{overflow:auto;min-width:0}.guanjia-drawer .el-drawer__header{margin-bottom:20px}
</style>
