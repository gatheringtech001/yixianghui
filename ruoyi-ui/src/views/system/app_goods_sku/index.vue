<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属商品" prop="goodsId">
        <el-input v-model="queryParams.goodsId" placeholder="请输入所属商品" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="所属商品" prop="goodsName">
        <el-input v-model="queryParams.goodsName" placeholder="请输入所属商品" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="属性名称" prop="skuName">
        <el-input v-model="queryParams.skuName" placeholder="请输入属性名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="属性状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择属性状态" clearable>
          <el-option v-for="dict in dict.type.enable_status" :key="dict.value" :label="dict.label"
            :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
          v-hasPermi="['system:app_goods_sku:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
          v-hasPermi="['system:app_goods_sku:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['system:app_goods_sku:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
          v-hasPermi="['system:app_goods_sku:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_goods_skuList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="类别id" align="center" prop="skuId" v-if="false" />
      <el-table-column label="所属商品Id" align="center" prop="goodsId" v-if="false" />
      <el-table-column label="所属商品" align="center" prop="goodsName" />
      <el-table-column label="属性名称" align="center" prop="skuName" />
      <el-table-column label="属性状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.enable_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-info" @click="handleOption(scope.row)"
            v-hasPermi="['system:app_goods_sku:edit']">管理选项</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_goods_sku:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_goods_sku:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改商品属性对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属商品ID" prop="goodsId" style="display: none;">
          <el-input v-model="form.goodsId" placeholder="请输入所属商品" />
        </el-form-item>
        <el-form-item label="属性名称" prop="skuName">
          <el-input v-model="form.skuName" placeholder="请输入属性名称" />
        </el-form-item>
        <el-form-item label="属性状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.enable_status" :key="dict.value"
              :label="dict.value">{{ dict.label }}</el-radio>
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
import { listApp_goods_sku, getApp_goods_sku, delApp_goods_sku, addApp_goods_sku, updateApp_goods_sku } from "@/api/system/app_goods_sku";

export default {
  name: "App_goods_sku",
  dicts: ['enable_status'],
  data() {
    var validateSkuName = (rule, value, callback) => {
      if(value.indexOf('-') != -1) {
        callback("属性名称不能包含 - 字符");
      } else {
        callback();
      }
    };
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
      // 商品属性表格数据
      app_goods_skuList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        goodsId: null,
        skuName: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        skuName: [
          { required: true, message: "属性名称不能为空", trigger: "blur" },
          { validator: validateSkuName, trigger: "blur"  }
        ],
      }
    };
  },
  created() {
    const goodsId = this.$route.query && this.$route.query.goodsId
    if (goodsId) {
      this.queryParams.goodsId = goodsId
    }
    console.log(goodsId)
    this.getList();
  },
  methods: {
    /** 查询商品属性列表 */
    getList() {
      this.loading = true;
      listApp_goods_sku(this.queryParams).then(response => {
        this.app_goods_skuList = response.rows;
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
        skuId: null,
        goodsId: null,
        skuName: null,
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
      this.ids = selection.map(item => item.skuId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加商品属性";
      this.form.goodsId = this.queryParams.goodsId
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const skuId = row.skuId || this.ids
      getApp_goods_sku(skuId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改商品属性";
      });
    },


    /** 属性配置 */
    handleOption(row) {
      // this.reset();
      // const goodsId = row.goodsId
      // this.$router.push({path: "/goods/app_goods_sku_data", query: {goodsId: goodsId}});
      this.reset();
      const skuId = row.skuId
      const goodsId = row.goodsId
      this.$router.push({ path: "/goods/app_goods_sku_option", query: { goodsId: goodsId, skuId: skuId } });
    },

    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.skuId != null) {
            updateApp_goods_sku(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_sku(this.form).then(response => {
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
      const skuIds = row.skuId || this.ids;
      this.$modal.confirm('是否确认删除商品属性编号为"' + skuIds + '"的数据项？').then(function () {
        return delApp_goods_sku(skuIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => { });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_sku/export', {
        ...this.queryParams
      }, `app_goods_sku_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
