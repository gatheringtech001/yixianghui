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
      <el-form-item label="用户银行标识" prop="bankName">
        <el-input
          v-model="queryParams.bankName"
          placeholder="请输入用户银行标识"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户银行名称" prop="bankTitle">
        <el-input
          v-model="queryParams.bankTitle"
          placeholder="请输入用户银行名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户银行分行" prop="bankSubbranch">
        <el-input
          v-model="queryParams.bankSubbranch"
          placeholder="请输入用户银行分行"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户银行户名" prop="bankAccountName">
        <el-input
          v-model="queryParams.bankAccountName"
          placeholder="请输入用户银行户名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户银行卡号" prop="bankAccountNum">
        <el-input
          v-model="queryParams.bankAccountNum"
          placeholder="请输入用户银行卡号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="银行卡状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择银行卡状态" clearable>
          <el-option
            v-for="dict in dict.type.enable_status"
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
          v-hasPermi="['system:app_user_bank:add']"
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
          v-hasPermi="['system:app_user_bank:edit']"
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
          v-hasPermi="['system:app_user_bank:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_user_bank:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_user_bankList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="银行卡id" align="center" prop="bankId" />
      <el-table-column label="用户id" align="center" prop="userId" />
      <el-table-column label="类型" align="center" prop="bankType" />
      <el-table-column label="用户银行标识" align="center" prop="bankName" />
      <el-table-column label="用户银行名称" align="center" prop="bankTitle" />
      <el-table-column label="用户银行分行" align="center" prop="bankSubbranch" />
      <el-table-column label="用户银行户名" align="center" prop="bankAccountName" />
      <el-table-column label="用户银行卡号" align="center" prop="bankAccountNum" />
      <el-table-column label="扩展数据" align="center" prop="extendData" />
      <el-table-column label="银行卡状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.enable_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_user_bank:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_user_bank:remove']"
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

    <!-- 添加或修改用户银行卡对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户id" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户id" />
        </el-form-item>
        <el-form-item label="用户银行标识" prop="bankName">
          <el-input v-model="form.bankName" placeholder="请输入用户银行标识" />
        </el-form-item>
        <el-form-item label="用户银行名称" prop="bankTitle">
          <el-input v-model="form.bankTitle" placeholder="请输入用户银行名称" />
        </el-form-item>
        <el-form-item label="用户银行分行" prop="bankSubbranch">
          <el-input v-model="form.bankSubbranch" placeholder="请输入用户银行分行" />
        </el-form-item>
        <el-form-item label="用户银行户名" prop="bankAccountName">
          <el-input v-model="form.bankAccountName" placeholder="请输入用户银行户名" />
        </el-form-item>
        <el-form-item label="用户银行卡号" prop="bankAccountNum">
          <el-input v-model="form.bankAccountNum" placeholder="请输入用户银行卡号" />
        </el-form-item>
        <el-form-item label="扩展数据" prop="extendData">
          <el-input v-model="form.extendData" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="银行卡状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.enable_status"
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
import { listApp_user_bank, getApp_user_bank, delApp_user_bank, addApp_user_bank, updateApp_user_bank } from "@/api/system/app_user_bank";

export default {
  name: "App_user_bank",
  dicts: ['enable_status'],
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
      // 用户银行卡表格数据
      app_user_bankList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        bankType: null,
        bankName: null,
        bankTitle: null,
        bankSubbranch: null,
        bankAccountName: null,
        bankAccountNum: null,
        extendData: null,
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
    /** 查询用户银行卡列表 */
    getList() {
      this.loading = true;
      listApp_user_bank(this.queryParams).then(response => {
        this.app_user_bankList = response.rows;
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
        bankId: null,
        userId: null,
        bankType: null,
        bankName: null,
        bankTitle: null,
        bankSubbranch: null,
        bankAccountName: null,
        bankAccountNum: null,
        extendData: null,
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
      this.ids = selection.map(item => item.bankId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加用户银行卡";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const bankId = row.bankId || this.ids
      getApp_user_bank(bankId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改用户银行卡";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.bankId != null) {
            updateApp_user_bank(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_user_bank(this.form).then(response => {
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
      const bankIds = row.bankId || this.ids;
      this.$modal.confirm('是否确认删除用户银行卡编号为"' + bankIds + '"的数据项？').then(function() {
        return delApp_user_bank(bankIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_user_bank/export', {
        ...this.queryParams
      }, `app_user_bank_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
