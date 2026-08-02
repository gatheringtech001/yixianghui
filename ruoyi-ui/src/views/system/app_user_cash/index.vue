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
      <el-form-item label="提现类型" prop="cashType">
        <el-select v-model="queryParams.cashType" placeholder="请选择提现类型" clearable>
          <el-option
            v-for="dict in dict.type.cash_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="支付机构单号" prop="payNo">
        <el-input
          v-model="queryParams.payNo"
          placeholder="请输入支付机构单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="金额" prop="money">
        <el-input
          v-model="queryParams.money"
          placeholder="请输入金额"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手续费" prop="cashFee">
        <el-input
          v-model="queryParams.cashFee"
          placeholder="请输入手续费"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户银行id" prop="bankId">
        <el-input
          v-model="queryParams.bankId"
          placeholder="请输入用户银行id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户支付宝id" prop="alipayId">
        <el-input
          v-model="queryParams.alipayId"
          placeholder="请输入用户支付宝id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户微信id" prop="weixinId">
        <el-input
          v-model="queryParams.weixinId"
          placeholder="请输入用户微信id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="提现状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择提现状态" clearable>
          <el-option
            v-for="dict in dict.type.cash_status"
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
          v-hasPermi="['system:app_user_cash:add']"
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
          v-hasPermi="['system:app_user_cash:edit']"
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
          v-hasPermi="['system:app_user_cash:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_user_cash:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_user_cashList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="提现id" align="center" prop="cashId" />
      <el-table-column label="用户id" align="center" prop="userId" />
      <el-table-column label="提现类型" align="center" prop="cashType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.cash_type" :value="scope.row.cashType"/>
        </template>
      </el-table-column>
      <el-table-column label="支付机构单号" align="center" prop="payNo" />
      <el-table-column label="金额" align="center" prop="money" />
      <el-table-column label="手续费" align="center" prop="cashFee" />
      <el-table-column label="用户银行id" align="center" prop="bankId" />
      <el-table-column label="用户支付宝id" align="center" prop="alipayId" />
      <el-table-column label="用户微信id" align="center" prop="weixinId" />
      <el-table-column label="提现回执" align="center" prop="replyContent" />
      <el-table-column label="提现状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.cash_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_user_cash:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_user_cash:remove']"
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

    <!-- 添加或修改用户提现对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户id" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户id" />
        </el-form-item>
        <el-form-item label="提现类型" prop="cashType">
          <el-radio-group v-model="form.cashType">
            <el-radio
              v-for="dict in dict.type.cash_type"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付机构单号" prop="payNo">
          <el-input v-model="form.payNo" placeholder="请输入支付机构单号" />
        </el-form-item>
        <el-form-item label="金额" prop="money">
          <el-input v-model="form.money" placeholder="请输入金额" />
        </el-form-item>
        <el-form-item label="手续费" prop="cashFee">
          <el-input v-model="form.cashFee" placeholder="请输入手续费" />
        </el-form-item>
        <el-form-item label="用户银行id" prop="bankId">
          <el-input v-model="form.bankId" placeholder="请输入用户银行id" />
        </el-form-item>
        <el-form-item label="用户支付宝id" prop="alipayId">
          <el-input v-model="form.alipayId" placeholder="请输入用户支付宝id" />
        </el-form-item>
        <el-form-item label="用户微信id" prop="weixinId">
          <el-input v-model="form.weixinId" placeholder="请输入用户微信id" />
        </el-form-item>
        <el-form-item label="提现回执">
          <editor v-model="form.replyContent" :min-height="192"/>
        </el-form-item>
        <el-form-item label="提现状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.cash_status"
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
import { listApp_user_cash, getApp_user_cash, delApp_user_cash, addApp_user_cash, updateApp_user_cash } from "@/api/system/app_user_cash";

export default {
  name: "App_user_cash",
  dicts: ['cash_status', 'cash_type'],
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
      // 用户提现表格数据
      app_user_cashList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        cashType: null,
        payNo: null,
        money: null,
        cashFee: null,
        bankId: null,
        alipayId: null,
        weixinId: null,
        replyContent: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          { required: true, message: "用户id不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询用户提现列表 */
    getList() {
      this.loading = true;
      listApp_user_cash(this.queryParams).then(response => {
        this.app_user_cashList = response.rows;
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
        cashId: null,
        userId: null,
        cashType: null,
        payNo: null,
        money: null,
        cashFee: null,
        bankId: null,
        alipayId: null,
        weixinId: null,
        replyContent: null,
        createTime: null,
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
      this.ids = selection.map(item => item.cashId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加用户提现";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const cashId = row.cashId || this.ids
      getApp_user_cash(cashId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改用户提现";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.cashId != null) {
            updateApp_user_cash(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_user_cash(this.form).then(response => {
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
      const cashIds = row.cashId || this.ids;
      this.$modal.confirm('是否确认删除用户提现编号为"' + cashIds + '"的数据项？').then(function() {
        return delApp_user_cash(cashIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_user_cash/export', {
        ...this.queryParams
      }, `app_user_cash_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
