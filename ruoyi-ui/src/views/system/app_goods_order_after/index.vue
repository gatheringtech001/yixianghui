<template>
  <div class="app-container review-operations">
    <el-tabs v-model="reviewTab" class="review-tabs" @tab-click="handleQuery"><el-tab-pane :label="'待处理' + (pendingTotal == null ? '' : ' · ' + pendingTotal)" name="pending"/><el-tab-pane label="已处理" name="processed"/><el-tab-pane label="退款异常" name="exception"/><el-tab-pane label="全部" name="all"/></el-tabs>
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="订单类型" prop="businessType">
        <el-select v-model="queryParams.businessType" clearable placeholder="全部类型">
          <el-option label="商品 / 旅居 / 课程" value="2" />
          <el-option label="活动" value="3" />
          <el-option label="会员卡" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属订单" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入所属订单"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退款订单编号" prop="outOrderNo">
        <el-input
          v-model="queryParams.outOrderNo"
          placeholder="请输入退款订单编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="商品小计金额" prop="orderMoney">-->
<!--        <el-input-->
<!--          v-model="queryParams.orderMoney"-->
<!--          placeholder="请输入商品小计金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="商品ID" prop="goodsId">-->
<!--        <el-input-->
<!--          v-model="queryParams.goodsId"-->
<!--          placeholder="请输入商品ID"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="商品数量" prop="goodsCount">-->
<!--        <el-input-->
<!--          v-model="queryParams.goodsCount"-->
<!--          placeholder="请输入商品数量"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="商品售价" prop="goodsMoney">-->
<!--        <el-input-->
<!--          v-model="queryParams.goodsMoney"-->
<!--          placeholder="请输入商品售价"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="快递金额" prop="expressMoney">-->
<!--        <el-input-->
<!--          v-model="queryParams.expressMoney"-->
<!--          placeholder="请输入快递金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="退款金额" prop="refundMoney">-->
<!--        <el-input-->
<!--          v-model="queryParams.refundMoney"-->
<!--          placeholder="请输入退款金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="快递名称" prop="backExpressName">-->
<!--        <el-input-->
<!--          v-model="queryParams.backExpressName"-->
<!--          placeholder="请输入快递名称"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="快递单号" prop="backExpressNo">-->
<!--        <el-input-->
<!--          v-model="queryParams.backExpressNo"-->
<!--          placeholder="请输入快递单号"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="重发快递名称" prop="resendExpressName">-->
<!--        <el-input-->
<!--          v-model="queryParams.resendExpressName"-->
<!--          placeholder="请输入重发快递名称"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="重发快递单号" prop="resendExpressNo">-->
<!--        <el-input-->
<!--          v-model="queryParams.resendExpressNo"-->
<!--          placeholder="请输入重发快递单号"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:app_goods_order_after:export']">导出</el-button></el-col><right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/></el-row>

    <el-table class="listing-desktop" v-loading="loading" :data="app_goods_order_afterList">
      <el-table-column label="申请" min-width="250"><template slot-scope="{row}"><strong>{{ row.goodsName || businessLabel(row.businessType) }}</strong><small class="listing-secondary">{{ row.outOrderNo || '申请 #' + row.afterId }}</small></template></el-table-column>
      <el-table-column label="类型" width="145"><template slot-scope="{row}">{{ businessLabel(row.businessType) }}</template></el-table-column>
      <el-table-column label="申请退款" width="115"><template slot-scope="{row}">¥{{ money(row.appRefundMoney == null ? row.goodsMoney : row.appRefundMoney) }}</template></el-table-column>
      <el-table-column label="状态" width="120"><template slot-scope="{row}"><el-tag :type="statusColor(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="申请顺序" width="100"><template slot-scope="{row}">#{{ row.afterId }}</template></el-table-column>
      <el-table-column label="操作" width="140" align="right"><template slot-scope="{row}"><el-button :type="String(row.status) === '0' ? 'primary' : 'default'" size="mini" @click="handleInfo(row)" v-hasPermi="['system:app_goods_order_after:edit']">{{ String(row.status) === '0' ? '处理申请' : '查看详情' }}</el-button></template></el-table-column>
    </el-table>
    <div v-loading="loading" class="listing-mobile"><article v-for="row in app_goods_order_afterList" :key="row.afterId" class="listing-mobile-card"><div class="listing-mobile-heading"><strong>{{ row.goodsName || businessLabel(row.businessType) }}</strong><el-tag :type="statusColor(row.status)" size="mini">{{ statusText(row.status) }}</el-tag></div><p>{{ row.outOrderNo || '申请 #' + row.afterId }}</p><p>申请退款 ¥{{ money(row.appRefundMoney == null ? row.goodsMoney : row.appRefundMoney) }} · {{ businessLabel(row.businessType) }}</p><div class="listing-mobile-actions"><el-button type="primary" size="mini" @click="handleInfo(row)" v-hasPermi="['system:app_goods_order_after:edit']">{{ String(row.status) === '0' ? '处理申请' : '查看详情' }}</el-button></div></article><el-empty v-if="!loading && !app_goods_order_afterList.length" :description="reviewTab === 'pending' ? '暂无待处理申请' : '暂无记录'"/></div>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改订单商品售后对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="所属订单" prop="orderId">
          <el-input v-model="form.orderId" placeholder="请输入所属订单" />
        </el-form-item>
        <el-form-item label="退款订单编号" prop="outOrderNo">
          <el-input v-model="form.outOrderNo" placeholder="请输入退款订单编号" />
        </el-form-item>
        <el-form-item label="商品小计金额" prop="orderMoney">
          <el-input v-model="form.orderMoney" placeholder="请输入商品小计金额" />
        </el-form-item>
        <el-form-item label="商品ID" prop="goodsId">
          <el-input v-model="form.goodsId" placeholder="请输入商品ID" />
        </el-form-item>
        <el-form-item label="商品数量" prop="goodsCount">
          <el-input v-model="form.goodsCount" placeholder="请输入商品数量" />
        </el-form-item>
        <el-form-item label="商品售价" prop="goodsMoney">
          <el-input v-model="form.goodsMoney" placeholder="请输入商品售价" />
        </el-form-item>
        <el-form-item label="快递金额" prop="expressMoney">
          <el-input v-model="form.expressMoney" placeholder="请输入快递金额" />
        </el-form-item>
        <el-form-item label="退款金额" prop="refundMoney">
          <el-input v-model="form.refundMoney" placeholder="请输入退款金额" />
        </el-form-item>
        <el-form-item label="快递名称" prop="backExpressName">
          <el-input v-model="form.backExpressName" placeholder="请输入快递名称" />
        </el-form-item>
        <el-form-item label="快递单号" prop="backExpressNo">
          <el-input v-model="form.backExpressNo" placeholder="请输入快递单号" />
        </el-form-item>
        <el-form-item label="重发快递名称" prop="resendExpressName">
          <el-input v-model="form.resendExpressName" placeholder="请输入重发快递名称" />
        </el-form-item>
        <el-form-item label="重发快递单号" prop="resendExpressNo">
          <el-input v-model="form.resendExpressNo" placeholder="请输入重发快递单号" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 售后信息查看审批 -->
    <el-dialog :title="'售后申请 #' + selrow.afterId" :visible.sync="info" width="640px" custom-class="listing-editor" append-to-body :close-on-click-modal="false">
      <div class="review-detail-summary"><strong>{{ selrow.goodsName || businessLabel(selrow.businessType) }}</strong><p>订单 {{ selrow.outOrderNo || selrow.orderId }} · 用户 #{{ selrow.userId }}</p><p v-if="selrow.reasonDescription">申请原因：{{ selrow.reasonDescription }}</p><p v-if="selrow.remark && String(selrow.status) !== '0'">处理意见：{{ selrow.remark }}</p></div>
      <div class="audit-container">
        <!-- 基本信息 -->
        <div class="audit-section">
          <h3>基本信息</h3>
          <div class="audit-row">
            <span class="label">售后类型：</span>
            <dict-tag :options="dict.type.after_type" :value="selrow.afterType"/>
          </div>
          <div class="audit-row">
            <span class="label">当前状态：</span>
            <el-tag :type="statusColor(selrow.status)" size="medium">
              {{ statusText(selrow.status) }}
            </el-tag>
          </div>
        </div>

        <!-- 金额信息 -->
        <div class="audit-section">
          <h3>金额信息</h3>
          <div class="audit-row">
            <span class="label">商品金额：</span>
            <span>¥{{ money(selrow.goodsMoney) }}</span>
          </div>
          <div class="audit-row">
            <span class="label">退款申请金额：</span>
            <span class="highlight">¥{{ money(selrow.appRefundMoney) }}</span>
          </div>
        </div>

        <el-form v-if="String(selrow.status) === '0'" ref="refundform" :model="refundform" :rules="refundrules" label-width="80px">
          <el-form-item label="退款金额" prop="refundMoney">
            <el-input v-model="refundform.refundMoney" :readonly="!!selrow.bookingAdjustmentId" placeholder="请输入退款金额" />
            <div v-if="selrow.bookingAdjustmentId">{{ selrow.reasonDescription }}；按本次申请差价退款，不同意可拒绝申请。</div>
          </el-form-item>
          <el-form-item label="审批意见" prop="remark">
            <el-input v-model="refundform.remark" type="textarea" placeholder="请输入内容" />
          </el-form-item>
        </el-form>
        <el-checkbox v-if="selrow.afterType === '1' && String(selrow.status) === '0'" v-model="refundform.returnReceived">已收到退货并核验无误</el-checkbox>

        <!-- 审核操作 -->
        <div class="audit-actions" v-if="selrow.status === '0'">
          <el-button type="success" :disabled="auditBusy" @click="approveAudit(selrow.afterId)">同意并退款 ¥{{ money(refundform.refundMoney) }}</el-button>
          <el-button type="danger" :disabled="auditBusy" @click="rejectAudit(selrow.afterId)">拒绝</el-button>
        </div>
        <div class="audit-actions" v-if="selrow.status === '1' || selrow.status === '5'">
          <el-button :disabled="auditBusy" @click="recoverRefund('sync')">同步微信退款结果</el-button>
          <el-button :disabled="auditBusy" @click="recoverRefund('retry')">继续原退款</el-button>
        </div>
      </div>
      <el-table :data="[selrow]" width="600">
        <el-table-column label="售后详情" width="550">
          <template slot-scope="{row}">
            <!-- 第一行：基础信息 -->
            <div class="info-line">
              <el-tag size="mini" effect="plain">{{ row.afterId }}</el-tag>
              <dict-tag :options="dict.type.after_type" :value="row.afterType"/>
              <el-tag :type="statusColor(row.status)" size="mini">
                {{ statusText(row.status) }}
              </el-tag>
            </div>
            <!-- 第二行：订单信息 -->
            <div class="info-line">
              <span class="info-label">订单：</span>
              <span>{{ row.orderId }}</span>
              <span class="info-label">用户：</span>
              <span>{{ row.userId }}</span>
            </div>
            <!-- 第三行：金额信息 -->
            <div class="money-line">
              <span>商品：¥{{ money(row.goodsMoney) }}</span>
              <span>运费：¥{{ money(row.expressMoney) }}</span>
              <span class="refund-money">退款：¥{{ money(row.refundMoney) }}</span>
            </div>
            <!-- 第四行：物流信息（条件显示） -->
            <div class="express-line" v-if="row.backExpressName">
              <i class="el-icon-truck"></i>
              <span>{{ row.backExpressName }}：{{ row.backExpressNo }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>
<script>
import { reviewQuery } from '@/utils/listing-operations.mjs'
import '@/assets/styles/listing-operations.css'
import { listApp_goods_order_after, getApp_goods_order_after, delApp_goods_order_after, addApp_goods_order_after, updateApp_goods_order_after,approvalApp_goods_order_after } from "@/api/system/app_goods_order_after";

export default {
  name: "App_goods_order_after",
  dicts: ['after_type'],
  data() {
    return {
      reviewTab: 'pending', pendingTotal: null,
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 订单商品售后表格数据
      app_goods_order_afterList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 售后信息查看审批
      info: false,
      auditBusy: false,
      //选择行
      selrow: {},
      // 查询参数
      queryParams: {
        businessType: null,
        pageNum: 1,
        pageSize: 10,
        afterType: null,
        userId: null,
        orderId: null,
        outOrderNo: null,
        orderMoney: null,
        goodsId: null,
        goodsCount: null,
        goodsMoney: null,
        expressMoney: null,
        refundMoney: null,
        backExpressName: null,
        backExpressNo: null,
        resendExpressName: null,
        resendExpressNo: null,
        status: null
      },
      // 表单参数
      form: {},
      // 退款表单参数
      refundform: {},
      // 表单校验
      rules: {
        userId: [
          { required: true, message: "用户ID不能为空", trigger: "blur" }
        ],
        goodsId: [
          { required: true, message: "商品ID不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "售后状态不能为空", trigger: "change" }
        ]
      },
      refundrules: {
        refundMoney: [
          { required: true, message: "退款金额不能为空", trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              if (!/^\d+(\.\d{1,2})?$/.test(value)) { // 允许整数或最多两位小数
                callback(new Error("请输入有效数字（如 100 或 99.99）"));
              } else if (value <= 0) {
                callback(new Error("退款金额必须大于0"));
              } else {
                callback();
              }
            },
            trigger: "blur"
          },
          {
            validator: (rule, value, callback) => {
              if (value > this.selrow.goodsMoney) {
                callback(new Error("退款金额不能大于商品金额"));
              } else {
                callback();
              }
            },
            trigger: "blur"
          }
        ],
        remark: [
          { required: true, message: "请输入审批意见", trigger: "blur" }
        ]
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    money(value) { const amount = Number(value); return Number.isFinite(amount) ? amount.toFixed(2) : '0.00' },
    businessLabel(value) { return ({'1':'会员卡','2':'商品 / 旅居 / 课程','3':'活动'})[value] || '商品 / 旅居 / 课程' },
    /** 查询订单商品售后列表 */
    getList() {
      const requestId = this.reviewRequestId = (this.reviewRequestId || 0) + 1;
      const tab = this.reviewTab;
      this.loading = true;
      listApp_goods_order_after(reviewQuery(this.queryParams, this.reviewTab)).then(response => {
        if (requestId !== this.reviewRequestId) return;
        this.app_goods_order_afterList = response.rows;
        this.total = response.total;
        if (tab === 'pending') this.pendingTotal = response.total
      }).finally(() => { if (requestId === this.reviewRequestId) this.loading = false });
      if (this.reviewTab !== 'pending') {
        listApp_goods_order_after(reviewQuery({ ...this.queryParams, pageNum: 1, pageSize: 1 }, 'pending')).then(response => { this.pendingTotal = response.total })
      }
    },
    statusColor(status) {
      const map = { '0': 'warning', '1': 'primary', '2': 'info', '5': 'danger', '6': 'success' }
      return map[status] || 'info'
    },
    statusText(status) {
      const map = { '0': '待审核', '1': '已同意退款', '2': '已拒绝', '5': '退款异常', '6': '退款完成' }
      return map[status] || status
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        afterId: null,
        afterType: null,
        userId: null,
        orderId: null,
        outOrderNo: null,
        orderMoney: null,
        goodsId: null,
        goodsCount: null,
        goodsMoney: null,
        expressMoney: null,
        refundMoney: null,
        backExpressName: null,
        backExpressNo: null,
        resendExpressName: null,
        resendExpressNo: null,
        remark: null,
        status: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.afterId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加订单商品售后";
    },
    /** 审核按钮操作 */
    async handleInfo(row) {
      const afterId = row && row.afterId || this.ids[0]
      if (!afterId) return
      const detail = (await getApp_goods_order_after(afterId)).data
      this.selrow = { ...detail, goodsName: detail.goodsName || (row && row.goodsName) }
      row = this.selrow
      this.refundform = {
        refundMoney: row.appRefundMoney != null ? row.appRefundMoney : (row.refundMoney != null ? row.refundMoney : row.goodsMoney),
        remark: '', returnReceived: false
      }
      this.info = true // 确保info变量已定义在data中
    },
    /** 通过审核 */
    approveAudit(afterId) {
      if (this.auditBusy) return
      this.$modal.confirm(`将向原支付渠道发起 ¥${this.money(this.refundform.refundMoney)} 退款，确认执行？`, '确认退款', { confirmButtonText: '确认退款', cancelButtonText: '取消', type: 'warning' }).then(() => {
        this.$refs["refundform"].validate().then(validated => {
          if (!validated) return
          this.auditBusy = true
          approvalApp_goods_order_after({
            afterId: afterId,
            status: '1',
            refundMoney: this.refundform.refundMoney,
            remark: this.refundform.remark,
            returnReceived: this.refundform.returnReceived,
            outOrderNo: this.selrow.outOrderNo,
            orderMoney: this.selrow.orderMoney || this.selrow.goodsMoney,
            orderId: this.selrow.orderId
          }).then(response => {
            this.$modal.msgSuccess((response && response.msg) || "审核通过，已发起微信退款")
            this.info = false
            this.getList()
          }).catch(err => {
            const msg = (err && (err.msg || err.message)) || '退款失败'
            this.$modal.msgError(msg)
            this.getList()
          }).finally(() => { this.auditBusy = false })
        })
      }).catch(() => {})
    },
    /** 拒绝审核 */
    rejectAudit(afterId) {
      if (this.auditBusy) return
      this.$modal.confirm('确认拒绝该售后申请？').then(() => {
        if(this.refundform.remark=='' || this.refundform.remark==undefined){
          this.$modal.msgError("请输入审批意见")
          return
        }
        this.auditBusy = true
        approvalApp_goods_order_after({
          afterId: afterId,
          status: '2',
          remark: this.refundform.remark,
          orderId: this.selrow.orderId,
          outOrderNo: this.selrow.outOrderNo
        }).then(response => {
          this.$modal.msgSuccess("已拒绝")
          this.info = false
          this.getList()
        }).catch(err => {
          const msg = (err && (err.msg || err.message)) || '操作失败'
          this.$modal.msgError(msg)
        }).finally(() => { this.auditBusy = false })
      }).catch(() => {})
    },
    async recoverRefund(refundAction) {
      if (this.auditBusy) return
      try {
        await this.$modal.confirm(refundAction === 'sync' ? '查询微信并同步原退款结果？' : '仅继续已批准的原退款单号，不新建退款。确认继续？')
        this.auditBusy = true
        const response = await approvalApp_goods_order_after({ afterId: this.selrow.afterId, refundAction, remark: this.refundform.remark || '管理员核对原退款结果' })
        this.$modal.msgSuccess(response.msg || '退款结果已同步')
        await this.handleInfo(this.selrow); this.getList()
      } catch (error) { if (error !== 'cancel' && error !== 'close' && error && error.message) this.$modal.msgError(error.message) }
      finally { this.auditBusy = false }
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const afterId = row.afterId || this.ids
      getApp_goods_order_after(afterId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改订单商品售后";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.afterId != null) {
            updateApp_goods_order_after(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_order_after(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const afterIds = row.afterId || this.ids;
      this.$modal.confirm('是否确认删除订单商品售后编号为"' + afterIds + '"的数据项？').then(function() {
        return delApp_goods_order_after(afterIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_order_after/export', {
        ...reviewQuery(this.queryParams, this.reviewTab)
      }, `app_goods_order_after_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
<style>
.info-line {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.info-line > * {
  margin-right: 8px;
}
.info-label {
  color: #909399;
  margin-left: 8px;
}
.money-line {
  color: #666;
  margin-bottom: 6px;
}
.money-line > span {
  margin-right: 12px;
}
.refund-money {
  color: #F56C6C;
  font-weight: bold;
}
.express-line {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
}
.express-line i {
  margin-right: 5px;
  color: #409EFF;
}
</style>
