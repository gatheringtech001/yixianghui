<template>
  <div class="app-container">
    <el-alert title="所有退款申请均须管理员审核。旅居、商品和课程在「订单售后」审核；活动和会员卡在下方审核。" type="info" :closable="false" show-icon />
    <h3>活动 / 会员卡退款审核</h3>
    <el-form inline size="small">
      <el-form-item label="申请状态">
        <el-select v-model="reviewState" clearable @change="loadChanges">
          <el-option v-for="(label, state) in changeStates" :key="state" :label="label" :value="state" />
        </el-select>
      </el-form-item>
      <el-button size="small" icon="el-icon-refresh" @click="loadChanges">刷新申请</el-button>
    </el-form>
    <el-table :data="changes" v-loading="reviewLoading">
      <el-table-column label="申请编号" prop="adjustmentId" width="100" />
      <el-table-column label="类型" width="100"><template slot-scope="s">{{ s.row.businessType === '3' ? '活动报名' : '会员卡' }}</template></el-table-column>
      <el-table-column label="订单编号" prop="orderId" width="100" />
      <el-table-column label="人数变更" width="120"><template slot-scope="s">{{ s.row.fromCount }} → {{ s.row.toCount }}</template></el-table-column>
      <el-table-column label="申请金额（元）" width="130"><template slot-scope="s">{{ money(s.row.amount) }}</template></el-table-column>
      <el-table-column label="状态" width="160"><template slot-scope="s">{{ changeStates[s.row.state] || s.row.state }}</template></el-table-column>
      <el-table-column label="审核说明" prop="reviewNote" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" min-width="220">
        <template slot-scope="s">
          <template v-if="s.row.state === 'REVIEW'">
            <el-button type="text" :disabled="busy" @click="review(s.row, 'approve')" v-hasPermi="['system:app_pay_refund_log:edit']">批准并退款</el-button>
            <el-button type="text" :disabled="busy" @click="review(s.row, 'reject')" v-hasPermi="['system:app_pay_refund_log:edit']">拒绝申请</el-button>
          </template>
          <template v-if="s.row.state === 'PROCESSING'">
            <el-button type="text" :disabled="busy" @click="review(s.row, 'sync')" v-hasPermi="['system:app_pay_refund_log:edit']">同步微信结果</el-button>
            <el-button type="text" :disabled="busy" @click="review(s.row, 'retry')" v-hasPermi="['system:app_pay_refund_log:edit']">继续原退款</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
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
import request from '@/utils/request'
import { listApp_pay_refund_log } from '@/api/system/app_pay_refund_log'

export default {
  name: 'App_pay_refund_log',
  data() {
    return {
      loading: false, reviewLoading: false, busy: false, total: 0, logs: [], changes: [], reviewState: 'REVIEW',
      queryParams: { pageNum: 1, pageSize: 10, orderId: null, agentRefundNo: null },
      changeStates: { REVIEW: '待管理员审核', PROCESSING: '已批准 / 结果待核对', PAYMENT: '待补款（无需审核）', COMPLETED: '已完成', REJECTED: '已拒绝', CANCELLED: '补款已取消' }
    }
  },
  created() { this.getList(); this.loadChanges() },
  methods: {
    money(value) { const amount = Number(value); return Number.isFinite(amount) ? amount.toFixed(2) : '-' },
    async getList() {
      this.loading = true
      try { const result = await listApp_pay_refund_log(this.queryParams); this.logs = result.rows; this.total = result.total }
      finally { this.loading = false }
    },
    search() { this.queryParams.pageNum = 1; this.getList() },
    async loadChanges() {
      this.reviewLoading = true
      try { const result = await request({ url: '/system/booking_adjustment/list', method: 'get', params: { state: this.reviewState } }); this.changes = result.data || [] }
      finally { this.reviewLoading = false }
    },
    async review(row, decision) {
      if (this.busy) return
      const title = ({approve: '批准并退款', reject: '拒绝退款申请', sync: '同步微信结果', retry: '继续原退款'})[decision]
      try {
        const { value } = await this.$prompt(
          decision === 'approve' ? '将按申请金额 ¥' + this.money(row.amount) + ' 退款。退款全部成功后更新人数/权益。请输入审核说明：'
            : decision === 'retry' ? '仅继续已批准的原退款单号，不会创建新的退款。请输入说明：' : '请输入本次操作说明：',
          title, { inputType: 'textarea', inputValidator: text => !!text && !!text.trim() && text.length <= 500 || '请输入500字以内的说明', confirmButtonText: title })
        this.busy = true
        const result = await request({ url: '/system/booking_adjustment/' + row.adjustmentId + '/review', method: 'post', data: { decision, note: value.trim() } })
        this.$modal.msgSuccess(result.data.state === 'COMPLETED' ? '已完成' : result.data.state === 'REJECTED' ? '已拒绝，原预约保留' : '已批准，微信退款结果待确认；请同步结果核对')
        await this.loadChanges(); await this.getList()
      } catch (error) { if (error !== 'cancel' && error !== 'close' && error && error.message) this.$modal.msgError(error.message) }
      finally { this.busy = false }
    },
    handleExport() { this.download('system/app_pay_refund_log/export', { ...this.queryParams }, 'refund-logs.xlsx') }
  }
}
</script>
