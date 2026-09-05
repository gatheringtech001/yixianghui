<template>
  <el-dialog :title="supplier.supplierName + ' · 发货协作'" :visible.sync="visible" width="92%" append-to-body>
    <el-alert :closable="false" :type="config.enabled && config.configured ? 'success' : 'warning'"
      :title="config.enabled && config.configured ? '内部运营群通知开关已开启、地址格式校验通过；实际发送需定时任务运行，尚不代表群消息送达' : '内部运营群自动通知未启用或地址无效；仍可手动导出发货清单、确认接单和回填物流'" />
    <p>接收方为我们自己的企业微信内部群，不是供应商微信群。群内添加“消息推送”获取 Webhook，由服务器安全配置；运营核对订单后转发给供应商。</p>
    <el-tabs v-model="tab">
      <el-tab-pane label="待发货与通知记录" name="orders">
        <el-button size="small" @click="load">刷新</el-button>
        <el-button size="small" :disabled="!selectedOrders.length" @click="exportOrders" v-hasPermi="['system:app_goods_order:export']">导出选中发货清单</el-button>
        <el-table v-loading="loading" :data="orders" @selection-change="selectedOrders = $event" row-key="orderId">
          <el-table-column type="selection" width="45" :selectable="eligible" />
          <el-table-column prop="orderNo" label="订单号" width="110" />
          <el-table-column label="商品清单" min-width="210"><template slot-scope="scope"><div v-for="item in scope.row.items" :key="item.goodsId">{{ item.goodsName }} × {{ item.count }}</div></template></el-table-column>
          <el-table-column label="收货信息" min-width="210"><template slot-scope="scope">{{ scope.row.address.linkPerson }} {{ scope.row.address.linkMobile }}<br>{{ addressText(scope.row.address) }}</template></el-table-column>
          <el-table-column label="付款/运营通知" min-width="140"><template slot-scope="scope"><div>{{ scope.row.payStatus === '1' ? '已付款' : '未付款' }}</div><div>{{ states[scope.row.noticeStatus] || scope.row.noticeStatus }}</div><div v-if="scope.row.orderStatus !== '1'">未付款、取消或售后中，不发送通知</div><small>{{ scope.row.lastError }}</small></template></el-table-column>
          <el-table-column label="物流" min-width="120"><template slot-scope="scope">{{ scope.row.expressName }} {{ scope.row.expressNo }}</template></el-table-column>
          <el-table-column label="操作" width="170"><template slot-scope="scope">
            <el-button v-if="eligible(scope.row) && !['sending','confirmed','shipped'].includes(scope.row.noticeStatus)" size="mini" type="text" @click="confirmOrder(scope.row)" v-hasPermi="['system:app_goods_order:edit']">确认供应商接单</el-button>
            <el-button v-if="eligible(scope.row) && scope.row.noticeStatus === 'confirmed'" size="mini" type="text" @click="openShipping(scope.row)" v-hasPermi="['system:app_goods_order:edit']">回填物流</el-button>
            <el-button v-if="eligible(scope.row) && ['failed','uncertain'].includes(scope.row.noticeStatus)" size="mini" type="text" @click="retry(scope.row)" v-hasPermi="['system:app_supplier:edit']">重试通知</el-button>
          </template></el-table-column>
        </el-table>
        <p>通知成功仅代表企业微信接口已接受内部群消息，不代表运营已读、已转发或供应商已接单。群通知不包含客户电话和地址；完整清单仅交给对应供应商。核对接单后确认，实际发货后再回填物流。每次最多显示最近 200 单。</p>
      </el-tab-pane>
      <el-tab-pane label="关联商品" name="goods">
        <el-button size="small" type="primary" :disabled="!selectedGoods.length" @click="bindGoods" v-hasPermi="['system:app_supplier:edit']">关联选中商品</el-button>
        <el-table :data="goods" @selection-change="selectedGoods = $event">
          <el-table-column type="selection" width="45" :selectable="row => !row.supplierId" />
          <el-table-column prop="goodsId" label="商品ID" width="100" />
          <el-table-column prop="goodsName" label="商品名称" />
          <el-table-column label="关联状态"><template slot-scope="scope">{{ scope.row.supplierId ? '已关联当前供应商' : '未关联' }}</template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
    <el-dialog title="回填发货物流" :visible.sync="shippingVisible" append-to-body width="440px">
      <el-form label-width="90px"><el-form-item label="快递公司"><el-input v-model="shipping.expressName" maxlength="50" /></el-form-item><el-form-item label="快递单号"><el-input v-model="shipping.expressNo" maxlength="60" /></el-form-item></el-form>
      <span slot="footer"><el-button @click="shippingVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="ship">确认已发货</el-button></span>
    </el-dialog>
  </el-dialog>
</template>
<script>
import request from '@/utils/request'
const base = '/system/app_supplier/fulfillment'
export default {
  data() { return {visible: false, supplier: {}, config: {}, orders: [], goods: [], selectedOrders: [], selectedGoods: [], loading: false, saving: false, tab: 'orders', shippingVisible: false, shipping: {},
    states: {pending: '待通知运营', sending: '通知运营中', sent: '已通知运营（需人工转发）', failed: '运营通知失败', uncertain: '发送结果待核查', confirmed: '供应商已接单', shipped: '已发货'}} },
  methods: {
    async open(supplier) { this.supplier = supplier; this.visible = true; this.selectedOrders = []; this.tab = 'orders'; await this.load() },
    async load() { this.loading = true; try {
      const [config, orders, goods] = await Promise.all([request({url: base + '/config'}), request({url: base + '/list', params: {supplierId: this.supplier.supplierId}}), request({url: base + '/goods/' + this.supplier.supplierId})])
      this.config = config.data; this.orders = orders.data; this.goods = goods.data
    } finally { this.loading = false } },
    eligible(row) { return row.payStatus === '1' && row.orderStatus === '1' && Number(row.isApplyCancel) !== 1 && !row.expressNo },
    addressText(a) { return [a.provinceName, a.cityName, a.countyName, a.streetName, a.addressDetail].filter(Boolean).join('') },
    async confirmOrder(row) { await this.$confirm('确认已与供应商核对该订单并接单？', '确认接单'); await request({url: `${base}/${row.orderId}/confirm`, method: 'post'}); await this.load() },
    async retry(row) { await this.$confirm('请先核对内部运营群是否已收到消息，避免重复转发给供应商。确认重新发送到内部运营群？', '重试运营通知'); await request({url: `${base}/${row.orderId}/retry`, method: 'post'}); await this.load() },
    openShipping(row) { this.shipping = {orderId: row.orderId, expressName: '', expressNo: ''}; this.shippingVisible = true },
    async ship() { this.saving = true; try { await request({url: `${base}/${this.shipping.orderId}/ship`, method: 'post', data: this.shipping}); this.shippingVisible = false; await this.load() } finally { this.saving = false } },
    async bindGoods() { await request({url: `${base}/goods/${this.supplier.supplierId}`, method: 'post', data: this.selectedGoods.map(row => row.goodsId)}); this.selectedGoods = []; await this.load() },
    exportOrders() {
      this.download(base + '/export', {orderIds: this.selectedOrders.map(row => row.orderId).join(',')}, '供应商发货清单.xlsx')
    }
  }
}
</script>
