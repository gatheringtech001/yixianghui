<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属用户" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入所属用户"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品id" prop="goodsId">
        <el-input
          v-model="queryParams.goodsId"
          placeholder="请输入商品id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否sku" prop="isSku">
        <el-select v-model="queryParams.isSku" placeholder="请选择是否sku" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="型号信息" prop="dataId">
        <el-input
          v-model="queryParams.dataId"
          placeholder="请输入型号信息"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="型号组合名" prop="dataValues">
        <el-input
          v-model="queryParams.dataValues"
          placeholder="请输入型号组合名"
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
          v-hasPermi="['system:app_goods_cart:add']"
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
          v-hasPermi="['system:app_goods_cart:edit']"
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
          v-hasPermi="['system:app_goods_cart:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_goods_cart:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_goods_cartList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="购物车id" align="center" prop="cartId" v-if="false"/>
      <el-table-column label="所属用户id" align="center" prop="userId" v-if="false"/>
      <el-table-column label="所属用户" align="center" prop="userName" min-width="100" show-overflow-tooltip/>
      <el-table-column label="商品id" align="center" prop="goodsId" v-if="false"/>
      <el-table-column label="商品" align="center" prop="goodsName" min-width="120" show-overflow-tooltip/>
      <el-table-column label="是否sku" align="center" prop="isSku">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isSku"/>
        </template>
      </el-table-column>
      <el-table-column label="型号信息" align="center" prop="dataId" show-overflow-tooltip/>
      <el-table-column label="型号组合名" align="center" prop="dataValues" show-overflow-tooltip/>
      <el-table-column label="商品数量" align="center" prop="goodsCount" />
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
            v-hasPermi="['system:app_goods_cart:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_goods_cart:remove']"
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

    <!-- 添加或修改用户购物车对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属用户" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入所属用户" />
        </el-form-item>
        <el-form-item label="商品id" prop="goodsId">
          <el-input v-model="form.goodsId" placeholder="请输入商品id" />
        </el-form-item>
        <el-form-item label="是否sku" prop="isSku">
          <el-radio-group v-model="form.isSku">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="型号信息" prop="dataId">
          <el-input v-model="form.dataId" placeholder="请输入型号信息" />
        </el-form-item>
        <el-form-item label="型号组合名" prop="dataValues">
          <el-input v-model="form.dataValues" placeholder="请输入型号组合名" />
        </el-form-item>
        <el-form-item label="商品数量" prop="goodsCount">
          <el-input v-model="form.goodsCount" placeholder="请输入商品数量" />
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
import { listApp_goods_cart, getApp_goods_cart, delApp_goods_cart, addApp_goods_cart, updateApp_goods_cart } from "@/api/system/app_goods_cart";
import {fastKey} from "core-js/internals/internal-metadata";

export default {
  name: "App_goods_cart",
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
      // 用户购物车表格数据
      app_goods_cartList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        goodsId: null,
        isSku: null,
        dataId: null,
        dataValues: null,
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
    fastKey,
    /** 查询用户购物车列表 */
    getList() {
      this.loading = true;
      listApp_goods_cart(this.queryParams).then(response => {
        this.app_goods_cartList = response.rows;
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
        cartId: null,
        userId: null,
        goodsId: null,
        isSku: null,
        dataId: null,
        dataValues: null,
        goodsCount: null,
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
      this.ids = selection.map(item => item.cartId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加用户购物车";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const cartId = row.cartId || this.ids
      getApp_goods_cart(cartId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改用户购物车";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.cartId != null) {
            updateApp_goods_cart(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_cart(this.form).then(response => {
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
      const cartIds = row.cartId || this.ids;
      this.$modal.confirm('是否确认删除用户购物车编号为"' + cartIds + '"的数据项？').then(function() {
        return delApp_goods_cart(cartIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_cart/export', {
        ...this.queryParams
      }, `app_goods_cart_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
