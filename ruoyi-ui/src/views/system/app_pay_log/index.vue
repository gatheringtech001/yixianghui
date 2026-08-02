<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户id" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="业务订单id" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入业务订单id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="唯一识别单号：10开通为充值；20开头为商品；命名方式为：标识+日期+业务单号" prop="payNo">
        <el-input
          v-model="queryParams.payNo"
          placeholder="请输入唯一识别单号：10开通为充值；20开头为商品；命名方式为：标识+日期+业务单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付名称" prop="payName">
        <el-input
          v-model="queryParams.payName"
          placeholder="请输入支付名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付说明" prop="payDescription">
        <el-input
          v-model="queryParams.payDescription"
          placeholder="请输入支付说明"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付金额" prop="payMoney">
        <el-input
          v-model="queryParams.payMoney"
          placeholder="请输入支付金额"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付方式" prop="payMethod">
        <el-select v-model="queryParams.payMethod" placeholder="请选择支付方式" clearable>
          <el-option
            v-for="dict in dict.type.pay_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="支付机构名称" prop="agentName">
        <el-input
          v-model="queryParams.agentName"
          placeholder="请输入支付机构名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="机构订单号" prop="agentPayNo">
        <el-input
          v-model="queryParams.agentPayNo"
          placeholder="请输入机构订单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择支付状态" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
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
          v-hasPermi="['system:app_pay_log:add']"
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
          v-hasPermi="['system:app_pay_log:edit']"
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
          v-hasPermi="['system:app_pay_log:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_pay_log:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_pay_logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键id" align="center" prop="logId" />
      <el-table-column label="用户id" align="center" prop="userId" />
      <el-table-column label="业务订单id" align="center" prop="orderId" />
      <el-table-column label="订单类型" align="center" prop="orderType" />
      <el-table-column label="唯一识别单号：10开通为充值；20开头为商品；命名方式为：标识+日期+业务单号" align="center" prop="payNo" />
      <el-table-column label="支付名称" align="center" prop="payName" />
      <el-table-column label="支付说明" align="center" prop="payDescription" />
      <el-table-column label="支付金额" align="center" prop="payMoney" />
      <el-table-column label="支付方式" align="center" prop="payMethod">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.pay_type" :value="scope.row.payMethod"/>
        </template>
      </el-table-column>
      <el-table-column label="支付机构名称" align="center" prop="agentName" />
      <el-table-column label="机构订单号" align="center" prop="agentPayNo" />
      <el-table-column label="回调内容" align="center" prop="notifyContent" />
      <el-table-column label="支付状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_pay_log:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_pay_log:remove']"
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

    <!-- 添加或修改支付记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户id" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户id" />
        </el-form-item>
        <el-form-item label="业务订单id" prop="orderId">
          <el-input v-model="form.orderId" placeholder="请输入业务订单id" />
        </el-form-item>
        <el-form-item label="唯一识别单号：10开通为充值；20开头为商品；命名方式为：标识+日期+业务单号" prop="payNo">
          <el-input v-model="form.payNo" placeholder="请输入唯一识别单号：10开通为充值；20开头为商品；命名方式为：标识+日期+业务单号" />
        </el-form-item>
        <el-form-item label="支付名称" prop="payName">
          <el-input v-model="form.payName" placeholder="请输入支付名称" />
        </el-form-item>
        <el-form-item label="支付说明" prop="payDescription">
          <el-input v-model="form.payDescription" placeholder="请输入支付说明" />
        </el-form-item>
        <el-form-item label="支付金额" prop="payMoney">
          <el-input v-model="form.payMoney" placeholder="请输入支付金额" />
        </el-form-item>
        <el-form-item label="支付方式" prop="payMethod">
          <el-select v-model="form.payMethod" placeholder="请选择支付方式">
            <el-option
              v-for="dict in dict.type.pay_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="支付机构名称" prop="agentName">
          <el-input v-model="form.agentName" placeholder="请输入支付机构名称" />
        </el-form-item>
        <el-form-item label="机构订单号" prop="agentPayNo">
          <el-input v-model="form.agentPayNo" placeholder="请输入机构订单号" />
        </el-form-item>
        <el-form-item label="回调内容" prop="notifyContent">
          <el-input v-model="form.notifyContent" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="支付状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.common_is_not"
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
import { listApp_pay_log, getApp_pay_log, delApp_pay_log, addApp_pay_log, updateApp_pay_log } from "@/api/system/app_pay_log";

export default {
  name: "App_pay_log",
  dicts: ['common_is_not', 'pay_type'],
  data() {
    return {
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
      // 支付记录表格数据
      app_pay_logList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        orderId: null,
        orderType: null,
        payNo: null,
        payName: null,
        payDescription: null,
        payMoney: null,
        payMethod: null,
        agentName: null,
        agentPayNo: null,
        notifyContent: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        orderId: [
          { required: true, message: "业务订单id不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "支付状态不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询支付记录列表 */
    getList() {
      this.loading = true;
      listApp_pay_log(this.queryParams).then(response => {
        this.app_pay_logList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        logId: null,
        userId: null,
        orderId: null,
        orderType: null,
        payNo: null,
        payName: null,
        payDescription: null,
        payMoney: null,
        payMethod: null,
        agentName: null,
        agentPayNo: null,
        notifyContent: null,
        createTime: null,
        updateTime: null,
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
      this.ids = selection.map(item => item.logId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加支付记录";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const logId = row.logId || this.ids
      getApp_pay_log(logId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改支付记录";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.logId != null) {
            updateApp_pay_log(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_pay_log(this.form).then(response => {
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
      const logIds = row.logId || this.ids;
      this.$modal.confirm('是否确认删除支付记录编号为"' + logIds + '"的数据项？').then(function() {
        return delApp_pay_log(logIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_pay_log/export', {
        ...this.queryParams
      }, `app_pay_log_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
