<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="邀请人id" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入邀请人id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="新用户id" prop="newUserId">
        <el-input
          v-model="queryParams.newUserId"
          placeholder="请输入新用户id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邀请码" prop="inviterCode">
        <el-input
          v-model="queryParams.inviterCode"
          placeholder="请输入邀请码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="累计佣金" prop="totalAward">
        <el-input
          v-model="queryParams.totalAward"
          placeholder="请输入累计佣金"
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
          v-hasPermi="['system:app_user_inviter:add']"
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
          v-hasPermi="['system:app_user_inviter:edit']"
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
          v-hasPermi="['system:app_user_inviter:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_user_inviter:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_user_inviterList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="邀请ID" align="center" prop="inviterId" width="90" />
      <el-table-column label="邀请人" align="left" min-width="200">
        <template slot-scope="scope">
          <div class="user-cell">
            <el-avatar v-if="scope.row.inviterAvatar" :size="36" :src="avatarUrl(scope.row.inviterAvatar)" />
            <el-avatar v-else :size="36">{{ avatarText(scope.row.inviterNickName) }}</el-avatar>
            <div class="user-meta">
              <div class="user-name">{{ scope.row.inviterNickName || '未知用户' }}</div>
              <div class="user-sub">ID: {{ scope.row.userId }} · {{ scope.row.inviterPhonenumber || '无手机号' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="被邀请人" align="left" min-width="200">
        <template slot-scope="scope">
          <div class="user-cell">
            <el-avatar v-if="scope.row.newUserAvatar" :size="36" :src="avatarUrl(scope.row.newUserAvatar)" />
            <el-avatar v-else :size="36">{{ avatarText(scope.row.newUserNickName) }}</el-avatar>
            <div class="user-meta">
              <div class="user-name">{{ scope.row.newUserNickName || '未知用户' }}</div>
              <div class="user-sub">ID: {{ scope.row.newUserId }} · {{ scope.row.newUserPhonenumber || '无手机号' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="邀请码" align="center" prop="inviterCode" width="120" show-overflow-tooltip />
      <el-table-column label="累计佣金" align="center" prop="totalAward" width="100" />
      <el-table-column label="邀请时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="100" show-overflow-tooltip />
      <el-table-column label="是否有效" align="center" prop="status" width="90">
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
            v-hasPermi="['system:app_user_inviter:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_user_inviter:remove']"
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

    <!-- 添加或修改邀请记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="邀请人id" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入邀请人id" />
        </el-form-item>
        <el-form-item label="新用户id" prop="newUserId">
          <el-input v-model="form.newUserId" placeholder="请输入新用户id" />
        </el-form-item>
        <el-form-item label="邀请码" prop="inviterCode">
          <el-input v-model="form.inviterCode" placeholder="请输入邀请码" />
        </el-form-item>
        <el-form-item label="累计佣金" prop="totalAward">
          <el-input v-model="form.totalAward" placeholder="请输入累计佣金" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
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
import { listApp_user_inviter, getApp_user_inviter, delApp_user_inviter, addApp_user_inviter, updateApp_user_inviter } from "@/api/system/app_user_inviter";

export default {
  name: "App_user_inviter",
  dicts: ['common_is_not'],
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
      // 邀请记录表格数据
      app_user_inviterList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        newUserId: null,
        inviterCode: null,
        totalAward: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    avatarUrl(path) {
      if (!path) return '';
      if (path.startsWith('http')) return path;
      return process.env.VUE_APP_BASE_API + path;
    },
    avatarText(name) {
      return (name || '?').slice(0, 1);
    },
    /** 查询邀请记录列表 */
    getList() {
      this.loading = true;
      listApp_user_inviter(this.queryParams).then(response => {
        this.app_user_inviterList = response.rows;
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
        inviterId: null,
        userId: null,
        newUserId: null,
        inviterCode: null,
        totalAward: null,
        remark: null,
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
      this.ids = selection.map(item => item.inviterId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加邀请记录";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const inviterId = row.inviterId || this.ids
      getApp_user_inviter(inviterId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改邀请记录";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.inviterId != null) {
            updateApp_user_inviter(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_user_inviter(this.form).then(response => {
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
      const inviterIds = row.inviterId || this.ids;
      this.$modal.confirm('是否确认删除邀请记录编号为"' + inviterIds + '"的数据项？').then(function() {
        return delApp_user_inviter(inviterIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_user_inviter/export', {
        ...this.queryParams
      }, `app_user_inviter_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  padding: 4px 0;
}
.user-meta {
  margin-left: 10px;
  text-align: left;
  min-width: 0;
}
.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}
.user-sub {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 2px;
}
</style>
