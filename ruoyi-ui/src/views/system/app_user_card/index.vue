<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="用户id" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="卡id" prop="cardId">
        <el-input
          v-model="queryParams.cardId"
          placeholder="请输入卡id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="有效期开始时间" prop="enableStartTime">
        <el-date-picker clearable
          v-model="queryParams.enableStartTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择有效期开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="有效期结束时间" prop="enableEndTime">
        <el-date-picker clearable
          v-model="queryParams.enableEndTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择有效期结束时间">
        </el-date-picker>
      </el-form-item>
<!--      <el-form-item label="可用天数" prop="enableDayCount">-->
<!--        <el-input-->
<!--          v-model="queryParams.enableDayCount"-->
<!--          placeholder="请输入可用天数"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="激活码" prop="activeCode">-->
<!--        <el-input-->
<!--          v-model="queryParams.activeCode"-->
<!--          placeholder="请输入激活码"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="激活时间" prop="activeTime">-->
<!--        <el-date-picker clearable-->
<!--          v-model="queryParams.activeTime"-->
<!--          type="date"-->
<!--          value-format="yyyy-MM-dd"-->
<!--          placeholder="请选择激活时间">-->
<!--        </el-date-picker>-->
<!--      </el-form-item>-->
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
          v-hasPermi="['system:app_user_card:add']"
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
          v-hasPermi="['system:app_user_card:edit']"
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
          v-hasPermi="['system:app_user_card:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_user_card:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_user_cardList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="记录id" align="center" prop="recordId" />
      <el-table-column label="用户id" align="center" prop="userId" />
      <el-table-column label="卡id" align="center" prop="cardId" />
<!--      <el-table-column label="激活类型" align="center" prop="activeType" />-->
      <el-table-column label="有效期开始时间" align="center" prop="enableStartTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.enableStartTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期结束时间" align="center" prop="enableEndTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.enableEndTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
<!--      <el-table-column label="可用天数" align="center" prop="enableDayCount" />-->
<!--      <el-table-column label="激活码" align="center" prop="activeCode" />-->
<!--      <el-table-column label="激活时间" align="center" prop="activeTime" width="180">-->
<!--        <template slot-scope="scope">-->
<!--          <span>{{ parseTime(scope.row.activeTime, '{y}-{m}-{d}') }}</span>-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.pay_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_user_card:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_user_card:remove']"
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

    <!-- 添加或修改用户会员卡对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="用户id" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户id" />
        </el-form-item>
        <el-form-item label="卡id" prop="cardId">
          <el-input v-model="form.cardId" placeholder="请输入卡id" />
        </el-form-item>
        <el-form-item label="有效期开始时间" prop="enableStartTime">
          <el-date-picker clearable
            v-model="form.enableStartTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择有效期开始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="有效期结束时间" prop="enableEndTime">
          <el-date-picker clearable
            v-model="form.enableEndTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择有效期结束时间">
          </el-date-picker>
        </el-form-item>
<!--        <el-form-item label="可用天数" prop="enableDayCount">-->
<!--          <el-input v-model="form.enableDayCount" placeholder="请输入可用天数" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="激活码" prop="activeCode">-->
<!--          <el-input v-model="form.activeCode" placeholder="请输入激活码" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="激活时间" prop="activeTime">-->
<!--          <el-date-picker clearable-->
<!--            v-model="form.activeTime"-->
<!--            type="date"-->
<!--            value-format="yyyy-MM-dd"-->
<!--            placeholder="请选择激活时间">-->
<!--          </el-date-picker>-->
<!--        </el-form-item>-->
        <el-form-item label="支付状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.pay_status"
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
import { listApp_user_card, getApp_user_card, delApp_user_card, addApp_user_card, updateApp_user_card } from "@/api/system/app_user_card";

export default {
  name: "App_user_card",
  dicts: ['pay_status'],
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
      // 用户会员卡表格数据
      app_user_cardList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        cardId: null,
        activeType: null,
        enableStartTime: null,
        enableEndTime: null,
        enableDayCount: null,
        activeCode: null,
        activeTime: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          { required: true, message: "用户id不能为空", trigger: "blur" }
        ],
        cardId: [
          { required: true, message: "卡id不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询用户会员卡列表 */
    getList() {
      this.loading = true;
      listApp_user_card(this.queryParams).then(response => {
        this.app_user_cardList = response.rows;
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
        recordId: null,
        userId: null,
        cardId: null,
        activeType: null,
        enableStartTime: null,
        enableEndTime: null,
        enableDayCount: null,
        createTime: null,
        updateTime: null,
        activeCode: null,
        activeTime: null,
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
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加用户会员卡";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recordId = row.recordId || this.ids
      getApp_user_card(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改用户会员卡";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateApp_user_card(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_user_card(this.form).then(response => {
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
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除用户会员卡编号为"' + recordIds + '"的数据项？').then(function() {
        return delApp_user_card(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_user_card/export', {
        ...this.queryParams
      }, `app_user_card_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
