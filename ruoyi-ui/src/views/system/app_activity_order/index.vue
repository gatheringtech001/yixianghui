<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="订单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="请输入订单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属用户" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="活动ID" prop="activityId">
        <el-input
          v-model="queryParams.activityId"
          placeholder="请输入活动ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="报名人姓名" prop="signName">
        <el-input
          v-model="queryParams.signName"
          placeholder="请输入报名人姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="报名人电话" prop="signMobile">
        <el-input
          v-model="queryParams.signMobile"
          placeholder="请输入报名人电话"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付状态" prop="payStatus">
        <el-select v-model="queryParams.payStatus" placeholder="请选择支付状态" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="报名状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择报名状态" clearable>
          <el-option
            v-for="dict in dict.type.activity_order_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:app_activity_order:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:app_activity_order:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:app_activity_order:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_activity_order:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_activity_orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="预约ID" align="center" prop="orderId" width="80" />
      <el-table-column label="订单号" align="center" prop="orderNo" min-width="170" show-overflow-tooltip />
      <el-table-column label="活动名称" align="center" min-width="140" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.activityInfo ? scope.row.activityInfo.activityName : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户ID" align="center" prop="userId" width="80" />
      <el-table-column label="报名人" align="center" prop="signName" width="100" />
      <el-table-column label="联系电话" align="center" prop="signMobile" width="120" />
      <el-table-column label="人数" align="center" prop="signCount" width="70" />
      <el-table-column label="应付金额" align="center" prop="moneyPayable" width="90">
        <template slot-scope="scope">
          <span>{{ formatMoney(scope.row.moneyPayable) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实付金额" align="center" prop="payMoney" width="90">
        <template slot-scope="scope">
          <span>{{ formatMoney(scope.row.payMoney) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" align="center" prop="payStatus" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.payStatus === '0'" type="warning" size="mini">待支付</el-tag>
          <el-tag v-else-if="scope.row.payStatus === '1'" type="success" size="mini">已支付</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="报名状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.activity_order_type" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="140">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_activity_order:query']"
          >详情</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_activity_order:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="订单号">
          <el-input v-model="form.orderNo" disabled />
        </el-form-item>
        <el-form-item label="活动名称">
          <el-input :value="form.activityInfo ? form.activityInfo.activityName : ''" disabled />
        </el-form-item>
        <el-form-item label="所属用户" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入所属用户" />
        </el-form-item>
        <el-form-item label="活动ID" prop="activityId">
          <el-input v-model="form.activityId" placeholder="请输入活动ID" />
        </el-form-item>
        <el-form-item label="报名人姓名" prop="signName">
          <el-input v-model="form.signName" placeholder="请输入报名人姓名" />
        </el-form-item>
        <el-form-item label="报名人电话" prop="signMobile">
          <el-input v-model="form.signMobile" placeholder="请输入报名人电话" />
        </el-form-item>
        <el-form-item label="预约人数" prop="signCount">
          <el-input v-model="form.signCount" placeholder="请输入预约人数" />
        </el-form-item>
        <el-form-item label="应付金额">
          <el-input :value="formatMoney(form.moneyPayable)" disabled />
        </el-form-item>
        <el-form-item label="实付金额">
          <el-input :value="formatMoney(form.payMoney)" disabled />
        </el-form-item>
        <el-form-item label="支付状态">
          <el-input :value="payStatusText(form.payStatus)" disabled />
        </el-form-item>
        <el-form-item label="支付时间">
          <el-input :value="parseTime(form.payTime) || '-'" disabled />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="报名状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.activity_order_type"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApp_activity_order, getApp_activity_order, delApp_activity_order, addApp_activity_order, updateApp_activity_order } from "@/api/system/app_activity_order";

export default {
  name: "App_activity_order",
  dicts: ['activity_order_type', 'common_is_not'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      app_activity_orderList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderNo: null,
        userId: null,
        activityId: null,
        signName: null,
        signMobile: null,
        payStatus: null,
        status: null
      },
      form: {},
      rules: {
        status: [
          { required: true, message: "报名状态不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    formatMoney(value) {
      const num = Number(value);
      if (!Number.isFinite(num)) return '0.00';
      return num.toFixed(2);
    },
    payStatusText(value) {
      if (value === '0' || value === 0) return '待支付';
      if (value === '1' || value === 1) return '已支付';
      return '-';
    },
    getList() {
      this.loading = true;
      listApp_activity_order(this.queryParams).then(response => {
        this.app_activity_orderList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        orderId: null,
        orderNo: null,
        userId: null,
        activityId: null,
        signName: null,
        signMobile: null,
        signCount: null,
        moneyPayable: null,
        payMoney: null,
        payStatus: null,
        payTime: null,
        remark: null,
        orderNum: null,
        createTime: null,
        status: null,
        activityInfo: null
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.orderId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加活动预约";
    },
    handleUpdate(row) {
      this.reset();
      const orderId = row.orderId || this.ids
      getApp_activity_order(orderId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "活动预约详情";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.orderId != null) {
            updateApp_activity_order(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_activity_order(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const orderIds = row.orderId || this.ids;
      this.$modal.confirm('是否确认删除活动预约编号为"' + orderIds + '"的数据项？').then(function() {
        return delApp_activity_order(orderIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('system/app_activity_order/export', {
        ...this.queryParams
      }, `app_activity_order_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
