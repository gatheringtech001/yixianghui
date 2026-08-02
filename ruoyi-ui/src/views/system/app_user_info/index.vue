<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="真实姓名" prop="trueName">
        <el-input
          v-model="queryParams.trueName"
          placeholder="请输入真实姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="身份证号" prop="idcard">
        <el-input
          v-model="queryParams.idcard"
          placeholder="请输入身份证号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="微信号" prop="wexinAccount">
        <el-input
          v-model="queryParams.wexinAccount"
          placeholder="请输入微信号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="支付宝账号" prop="alipayAccount">-->
<!--        <el-input-->
<!--          v-model="queryParams.alipayAccount"-->
<!--          placeholder="请输入支付宝账号"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
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
          v-hasPermi="['system:app_user_info:add']"
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
          v-hasPermi="['system:app_user_info:edit']"
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
          v-hasPermi="['system:app_user_info:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_user_info:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_user_infoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户ID" align="center" prop="userId" />
<!--      <el-table-column label="应用ID" align="center" prop="appId" />-->
<!--      <el-table-column label="所属分组" align="center" prop="groupId" />-->
<!--      <el-table-column label="推荐人" align="center" prop="introId" />-->
<!--      <el-table-column label="推荐码地址" align="center" prop="qrcodeUrl" />-->
      <el-table-column label="金币数量" align="center" prop="golden" />
<!--      <el-table-column label="积分数量" align="center" prop="score" />-->
<!--      <el-table-column label="钱包" align="center" prop="money" />-->
<!--      <el-table-column label="粉丝数量" align="center" prop="fansCount" />-->
      <el-table-column label="真实姓名" align="center" prop="trueName" />
      <el-table-column label="身份证号" align="center" prop="idcard" />
<!--      <el-table-column label="提现银行" align="center" prop="bankTitle" />-->
<!--      <el-table-column label="银行户名" align="center" prop="bankUsername" />-->
<!--      <el-table-column label="银行卡号" align="center" prop="bankAccount" />-->
<!--      <el-table-column label="用户所在城市" align="center" prop="city" />-->
      <el-table-column label="微信号" align="center" prop="wexinAccount" />
      <el-table-column label="微信openid" align="center" prop="weixinOpenid" />
<!--      <el-table-column label="支付宝账号" align="center" prop="alipayAccount" />-->
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_user_info:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_user_info:remove']"
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

    <!-- 添加或修改用户信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
<!--        <el-form-item label="应用ID" prop="appId">-->
<!--          <el-input v-model="form.appId" placeholder="请输入应用ID" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="所属分组" prop="groupId">-->
<!--          <el-input v-model="form.groupId" placeholder="请输入所属分组" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="推荐人" prop="introId">-->
<!--          <el-input v-model="form.introId" placeholder="请输入推荐人" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="推荐码地址" prop="qrcodeUrl">-->
<!--          <el-input v-model="form.qrcodeUrl" placeholder="请输入推荐码地址" />-->
<!--        </el-form-item>-->
        <el-form-item label="金币数量" prop="golden">
          <el-input v-model="form.golden" placeholder="请输入金币数量" />
        </el-form-item>
<!--        <el-form-item label="积分数量" prop="score">-->
<!--          <el-input v-model="form.score" placeholder="请输入积分数量" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="钱包" prop="money">-->
<!--          <el-input v-model="form.money" placeholder="请输入钱包" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="粉丝数量" prop="fansCount">-->
<!--          <el-input v-model="form.fansCount" placeholder="请输入粉丝数量" />-->
<!--        </el-form-item>-->
        <el-form-item label="真实姓名" prop="trueName">
          <el-input v-model="form.trueName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idcard">
          <el-input v-model="form.idcard" placeholder="请输入身份证号" />
        </el-form-item>
<!--        <el-form-item label="提现银行" prop="bankTitle">-->
<!--          <el-input v-model="form.bankTitle" placeholder="请输入提现银行" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="银行户名" prop="bankUsername">-->
<!--          <el-input v-model="form.bankUsername" placeholder="请输入银行户名" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="银行卡号" prop="bankAccount">-->
<!--          <el-input v-model="form.bankAccount" placeholder="请输入银行卡号" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="用户所在城市" prop="city">-->
<!--          <el-input v-model="form.city" placeholder="请输入用户所在城市" />-->
<!--        </el-form-item>-->
        <el-form-item label="微信号" prop="wexinAccount">
          <el-input v-model="form.wexinAccount" placeholder="请输入微信号" />
        </el-form-item>
        <el-form-item label="微信openid" prop="weixinOpenid">
          <el-input v-model="form.weixinOpenid" placeholder="请输入微信openid" />
        </el-form-item>
<!--        <el-form-item label="支付宝账号" prop="alipayAccount">-->
<!--          <el-input v-model="form.alipayAccount" placeholder="请输入支付宝账号" />-->
<!--        </el-form-item>-->
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApp_user_info, getApp_user_info, delApp_user_info, addApp_user_info, updateApp_user_info } from "@/api/system/app_user_info";

export default {
  name: "App_user_info",
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
      // 用户信息表格数据
      app_user_infoList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        appId: null,
        groupId: null,
        introId: null,
        qrcodeUrl: null,
        golden: null,
        score: null,
        money: null,
        fansCount: null,
        trueName: null,
        idcard: null,
        bankTitle: null,
        bankUsername: null,
        bankAccount: null,
        city: null,
        wexinAccount: null,
        weixinOpenid: null,
        alipayAccount: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        appId: [
          { required: true, message: "应用ID不能为空", trigger: "blur" }
        ],
        groupId: [
          { required: true, message: "所属分组不能为空", trigger: "blur" }
        ],
        introId: [
          { required: true, message: "推荐人不能为空", trigger: "blur" }
        ],
        qrcodeUrl: [
          { required: true, message: "推荐码地址不能为空", trigger: "blur" }
        ],
        money: [
          { required: true, message: "钱包不能为空", trigger: "blur" }
        ],
        fansCount: [
          { required: true, message: "粉丝数量不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询用户信息列表 */
    getList() {
      this.loading = true;
      listApp_user_info(this.queryParams).then(response => {
        this.app_user_infoList = response.rows;
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
        userId: null,
        appId: null,
        groupId: null,
        introId: null,
        qrcodeUrl: null,
        golden: null,
        score: null,
        money: null,
        fansCount: null,
        trueName: null,
        idcard: null,
        bankTitle: null,
        bankUsername: null,
        bankAccount: null,
        city: null,
        wexinAccount: null,
        weixinOpenid: null,
        alipayAccount: null
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
      this.ids = selection.map(item => item.userId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加用户信息";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const userId = row.userId || this.ids
      getApp_user_info(userId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改用户信息";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.userId != null) {
            updateApp_user_info(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_user_info(this.form).then(response => {
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
      const userIds = row.userId || this.ids;
      this.$modal.confirm('是否确认删除用户信息编号为"' + userIds + '"的数据项？').then(function() {
        return delApp_user_info(userIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_user_info/export', {
        ...this.queryParams
      }, `app_user_info_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
