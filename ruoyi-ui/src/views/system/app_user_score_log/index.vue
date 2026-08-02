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
<!--      <el-form-item label="积分数量" prop="score">-->
<!--        <el-input-->
<!--          v-model="queryParams.score"-->
<!--          placeholder="请输入积分数量"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="积分余额" prop="balance">-->
<!--        <el-input-->
<!--          v-model="queryParams.balance"-->
<!--          placeholder="请输入积分余额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="变化类型" prop="tradeType">
        <el-select v-model="queryParams.tradeType" placeholder="请选择变化类型" clearable>
          <el-option
            v-for="dict in dict.type.wallet_change_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="交易标题" prop="tradeTitle">
        <el-input
          v-model="queryParams.tradeTitle"
          placeholder="请输入交易标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="交易说明" prop="tradeDetail">-->
<!--        <el-input-->
<!--          v-model="queryParams.tradeDetail"-->
<!--          placeholder="请输入交易说明"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="交易数据" prop="tradeData">-->
<!--        <el-input-->
<!--          v-model="queryParams.tradeData"-->
<!--          placeholder="请输入交易数据"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="业务类型" prop="businessType">
        <el-select v-model="queryParams.businessType" placeholder="请选择业务类型" clearable>
          <el-option
            v-for="dict in dict.type.gold_got_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="业务id" prop="businessId">
        <el-input
          v-model="queryParams.businessId"
          placeholder="请输入业务id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否有效" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择是否有效" clearable>
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
          v-hasPermi="['system:app_user_score_log:add']"
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
          v-hasPermi="['system:app_user_score_log:edit']"
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
          v-hasPermi="['system:app_user_score_log:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_user_score_log:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_user_score_logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="记录id" align="center" prop="logId" />
      <el-table-column label="用户id" align="center" prop="userId" />
      <el-table-column label="积分数量" align="center" prop="score" />
      <el-table-column label="积分余额" align="center" prop="balance" />
      <el-table-column label="变化类型" align="center" prop="tradeType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.wallet_change_type" :value="scope.row.tradeType"/>
        </template>
      </el-table-column>
      <el-table-column label="交易标题" align="center" prop="tradeTitle" />
      <el-table-column label="交易说明" align="center" prop="tradeDetail" />
      <el-table-column label="交易数据" align="center" prop="tradeData" />
      <el-table-column label="业务类型" align="center" prop="businessType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.gold_got_type" :value="scope.row.businessType"/>
        </template>
      </el-table-column>
      <el-table-column label="业务id" align="center" prop="businessId" />
      <el-table-column label="是否有效" align="center" prop="status">
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
            v-hasPermi="['system:app_user_score_log:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_user_score_log:remove']"
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

    <!-- 添加或修改积分记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户id" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户id" />
        </el-form-item>
        <el-form-item label="积分数量" prop="score">
          <el-input v-model="form.score" placeholder="请输入积分数量" />
        </el-form-item>
        <el-form-item label="积分余额" prop="balance">
          <el-input v-model="form.balance" placeholder="请输入积分余额" />
        </el-form-item>
        <el-form-item label="变化类型" prop="tradeType">
          <el-radio-group v-model="form.tradeType">
            <el-radio
              v-for="dict in dict.type.wallet_change_type"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="交易标题" prop="tradeTitle">
          <el-input v-model="form.tradeTitle" placeholder="请输入交易标题" />
        </el-form-item>
        <el-form-item label="交易说明" prop="tradeDetail">
          <el-input v-model="form.tradeDetail" placeholder="请输入交易说明" />
        </el-form-item>
        <el-form-item label="交易数据" prop="tradeData">
          <el-input v-model="form.tradeData" placeholder="请输入交易数据" />
        </el-form-item>
        <el-form-item label="业务类型" prop="businessType">
          <el-radio-group v-model="form.businessType">
            <el-radio
              v-for="dict in dict.type.gold_got_type"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="业务id" prop="businessId">
          <el-input v-model="form.businessId" placeholder="请输入业务id" />
        </el-form-item>
        <el-form-item label="是否有效" prop="status">
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
import { listApp_user_score_log, getApp_user_score_log, delApp_user_score_log, addApp_user_score_log, updateApp_user_score_log } from "@/api/system/app_user_score_log";

export default {
  name: "App_user_score_log",
  dicts: ['common_is_not', 'gold_got_type', 'wallet_change_type'],
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
      // 积分记录表格数据
      app_user_score_logList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        score: null,
        balance: null,
        tradeType: null,
        tradeTitle: null,
        tradeDetail: null,
        tradeData: null,
        businessType: null,
        businessId: null,
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
    /** 查询积分记录列表 */
    getList() {
      this.loading = true;
      listApp_user_score_log(this.queryParams).then(response => {
        this.app_user_score_logList = response.rows;
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
        score: null,
        balance: null,
        tradeType: null,
        tradeTitle: null,
        tradeDetail: null,
        tradeData: null,
        businessType: null,
        businessId: null,
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
      this.ids = selection.map(item => item.logId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加积分记录";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const logId = row.logId || this.ids
      getApp_user_score_log(logId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改积分记录";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.logId != null) {
            updateApp_user_score_log(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_user_score_log(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除积分记录编号为"' + logIds + '"的数据项？').then(function() {
        return delApp_user_score_log(logIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_user_score_log/export', {
        ...this.queryParams
      }, `app_user_score_log_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
