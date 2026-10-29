<template>
  <div class="app-container">
    <el-alert title="所有退款统一在原有「订单售后」审核。本页仅查看微信退款流水。" type="info" :closable="false" show-icon />
    <h3>微信退款流水（只读）</h3>
    <el-form :model="queryParams" inline size="small">
      <el-form-item label="订单编号"><el-input v-model="queryParams.orderId" clearable /></el-form-item>
      <el-form-item label="退款单号"><el-input v-model="queryParams.agentRefundNo" clearable /></el-form-item>
      <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
      <el-button icon="el-icon-download" @click="handleExport" v-hasPermi="['system:app_pay_refund_log:export']">导出</el-button>
    </el-form>
    <el-table v-loading="loading" :data="logs">
      <el-table-column label="流水编号" prop="logId" width="100" />
      <el-table-column label="订单编号" prop="orderId" width="100" />
      <el-table-column label="订单类型" width="100"><template slot-scope="s">{{ ({'1':'会员卡','2':'商品/旅居/课程','3':'活动'})[s.row.orderType] || s.row.orderType }}</template></el-table-column>
      <el-table-column label="微信退款编号" prop="payNo" min-width="180" show-overflow-tooltip />
      <el-table-column label="商户退款单号" prop="agentRefundNo" min-width="180" show-overflow-tooltip />
      <el-table-column label="退款金额（元）" width="140"><template slot-scope="s">{{ money(Number(s.row.refundMoney) / 100) }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template slot-scope="s">{{ ({'0':'处理中','1':'已退款','2':'已关闭','3':'异常'})[s.row.status] || '待核对' }}</template></el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="170" />
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listApp_pay_refund_log } from '@/api/system/app_pay_refund_log'

export default {
  name: 'App_pay_refund_log',
  data() {
    return {
      loading: false, total: 0, logs: [],
      queryParams: { pageNum: 1, pageSize: 10, orderId: null, agentRefundNo: null }
    }
  },
  created() { this.getList() },
  methods: {
    money(value) { const amount = Number(value); return Number.isFinite(amount) ? amount.toFixed(2) : '-' },
    async getList() {
      this.loading = true
      try { const result = await listApp_pay_refund_log(this.queryParams); this.logs = result.rows; this.total = result.total }
      finally { this.loading = false }
    },
    search() { this.queryParams.pageNum = 1; this.getList() },
    handleExport() { this.download('system/app_pay_refund_log/export', { ...this.queryParams }, 'refund-logs.xlsx') }
  }
}
</script>
