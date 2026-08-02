<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="优惠券id" prop="couponId">
        <el-input
          v-model="queryParams.couponId"
          placeholder="请输入优惠券id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="领取用户" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入领取用户"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="使用的订单" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入使用的订单"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="折扣金额" prop="discountPrice">
        <el-input
          v-model="queryParams.discountPrice"
          placeholder="请输入折扣金额"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="领取方式" prop="getMethod">
        <el-select v-model="queryParams.getMethod" placeholder="请选择领取方式" clearable>
          <el-option
            v-for="dict in dict.type.coupon_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否已使用" prop="isUsed">
        <el-select v-model="queryParams.isUsed" placeholder="请选择是否已使用" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['system:app_goods_coupon_got:add']"
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
          v-hasPermi="['system:app_goods_coupon_got:edit']"
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
          v-hasPermi="['system:app_goods_coupon_got:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_goods_coupon_got:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_goods_coupon_gotList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="记录ID" align="center" prop="gotId" />
      <el-table-column label="优惠券id" align="center" prop="couponId" />

      <el-table-column label="优惠券名称" align="center" prop="couponInfo">
        <template slot-scope="scope">
          <span>{{ scope.row.couponInfo.couponName }}</span>
        </template>
      </el-table-column>

      <el-table-column label="用户ID" align="center" prop="userId" />
      <el-table-column label="折扣金额" align="center" prop="discountPrice" />
      <el-table-column label="领取方式" align="center" prop="getMethod">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.coupon_type" :value="scope.row.getMethod"/>
        </template>
      </el-table-column>
      <el-table-column label="是否已使用" align="center" prop="isUsed">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isUsed"/>
        </template>
      </el-table-column>
      <el-table-column label="使用的订单ID" align="center" prop="orderId" />
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
            v-hasPermi="['system:app_goods_coupon_got:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_goods_coupon_got:remove']"
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

    <!-- 添加或修改优惠券领取记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="优惠券id" prop="couponId">
          <el-input v-model="form.couponId" placeholder="请输入优惠券id" />
        </el-form-item>
        <el-form-item label="领取用户" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入领取用户" />
        </el-form-item>
        <el-form-item label="使用的订单" prop="orderId">
          <el-input v-model="form.orderId" placeholder="请输入使用的订单" />
        </el-form-item>
        <el-form-item label="折扣金额" prop="discountPrice">
          <el-input v-model="form.discountPrice" placeholder="请输入折扣金额" />
        </el-form-item>
        <el-form-item label="领取方式" prop="getMethod">
          <el-radio-group v-model="form.getMethod">
            <el-radio
              v-for="dict in dict.type.coupon_type"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否已使用" prop="isUsed">
          <el-radio-group v-model="form.isUsed">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
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
import { listApp_goods_coupon_got, getApp_goods_coupon_got, delApp_goods_coupon_got, addApp_goods_coupon_got, updateApp_goods_coupon_got } from "@/api/system/app_goods_coupon_got";

export default {
  name: "App_goods_coupon_got",
  dicts: ['coupon_type', 'common_is_not'],
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
      // 优惠券领取记录表格数据
      app_goods_coupon_gotList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        couponId: null,
        userId: null,
        orderId: null,
        discountPrice: null,
        getMethod: null,
        isUsed: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        couponId: [
          { required: true, message: "优惠券id不能为空", trigger: "blur" }
        ],
        userId: [
          { required: true, message: "领取用户不能为空", trigger: "blur" }
        ],
        getMethod: [
          { required: true, message: "领取方式不能为空", trigger: "change" }
        ],
        isUsed: [
          { required: true, message: "是否已使用不能为空", trigger: "change" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询优惠券领取记录列表 */
    getList() {
      this.loading = true;
      listApp_goods_coupon_got(this.queryParams).then(response => {
        this.app_goods_coupon_gotList = response.rows;
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
        gotId: null,
        couponId: null,
        userId: null,
        orderId: null,
        discountPrice: null,
        getMethod: null,
        isUsed: null,
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
      this.ids = selection.map(item => item.gotId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加优惠券领取记录";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const gotId = row.gotId || this.ids
      getApp_goods_coupon_got(gotId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改优惠券领取记录";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.gotId != null) {
            updateApp_goods_coupon_got(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_coupon_got(this.form).then(response => {
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
      const gotIds = row.gotId || this.ids;
      this.$modal.confirm('是否确认删除优惠券领取记录编号为"' + gotIds + '"的数据项？').then(function() {
        return delApp_goods_coupon_got(gotIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_coupon_got/export', {
        ...this.queryParams
      }, `app_goods_coupon_got_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
