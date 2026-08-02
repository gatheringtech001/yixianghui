<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属上级" prop="parentId">
        <el-input
          v-model="queryParams.parentId"
          placeholder="请输入所属上级"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类名称" prop="categoryName">
        <el-input
          v-model="queryParams.categoryName"
          placeholder="请输入分类名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择分类状态" clearable>
          <el-option
            v-for="dict in dict.type.category_status"
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
          v-hasPermi="['system:app_goods_category:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-sort"
          size="mini"
          @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="app_goods_categoryList"
      row-key="categoryId"
      :default-expand-all="isExpandAll"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
    >
      <el-table-column label="分类ID" align="center" prop="categoryId" width="90"/>
      <el-table-column label="所属上级" prop="parentName" />
      <el-table-column label="上级ID" align="center" prop="parentId" width="90"/>
<!--      <el-table-column label="祖级id集合" align="center" prop="parentIds" />-->
      <el-table-column label="分类名称" align="center" prop="categoryName" />
      <el-table-column label="分类图标" align="center" prop="categoryIcon" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.categoryIcon" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="是否首页推荐" align="center" prop="isHot">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isHot"/>
        </template>
      </el-table-column>
      <el-table-column label="分类链接类型" align="center" prop="linkType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.category_link_type" :value="scope.row.linkType"/>
        </template>
      </el-table-column>
      <el-table-column label="分类链接ID" align="center" prop="linkId" width="100"/>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="排序顺序" align="center" prop="orderNum" />
      <el-table-column label="分类状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.category_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_goods_category:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['system:app_goods_category:add']"
          >新增</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_goods_category:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改商品分类对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类ID" v-if="form.categoryId">
          <el-input v-model="form.categoryId" disabled />
        </el-form-item>
        <el-form-item label="所属上级" prop="parentId">
          <treeselect v-model="form.parentId" :options="app_goods_categoryOptions" :normalizer="normalizer" placeholder="请选择所属上级" />
        </el-form-item>
<!--        <el-form-item label="祖级id集合" prop="parentIds">-->
<!--          <el-input v-model="form.parentIds" placeholder="请输入祖级id集合" />-->
<!--        </el-form-item>-->
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类图标" prop="categoryIcon">
          <image-upload v-model="form.categoryIcon"/>
        </el-form-item>
        <el-form-item label="是否首页推荐" prop="isHot">
          <el-radio-group v-model="form.isHot">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类链接类型" prop="linkType">
          <el-radio-group v-model="form.linkType">
            <el-radio
              v-for="dict in dict.type.category_link_type"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类链接ID" prop="linkId">
          <el-input v-model="form.linkId" placeholder="分类链接ID" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="排序顺序" prop="orderNum">
          <el-input v-model="form.orderNum" placeholder="请输入排序顺序" />
        </el-form-item>
        <el-form-item label="分类状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.category_status"
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
import { listApp_goods_category, getApp_goods_category, delApp_goods_category, addApp_goods_category, updateApp_goods_category } from "@/api/system/app_goods_category";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";

export default {
  name: "App_goods_category",
  dicts: ['category_status', 'common_is_not', 'category_link_type'],
  components: {
    Treeselect
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 商品分类表格数据
      app_goods_categoryList: [],
      // 商品分类树选项
      app_goods_categoryOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      // 查询参数
      queryParams: {
        parentId: null,
        categoryName: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        categoryName: [
          { required: true, message: "分类名称不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "分类状态不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询商品分类列表 */
    getList() {
      this.loading = true;
      listApp_goods_category(this.queryParams).then(response => {
        this.app_goods_categoryList = this.handleTree(response.data, "categoryId", "parentId");
        this.loading = false;
      });
    },
    /** 转换商品分类数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.categoryId,
        label: node.categoryName,
        children: node.children
      };
    },
	/** 查询商品分类下拉树结构 */
    getTreeselect() {
      listApp_goods_category().then(response => {
        this.app_goods_categoryOptions = [];
        const data = { categoryId: 0, categoryName: '顶级节点', children: [] };
        data.children = this.handleTree(response.data, "categoryId", "parentId");
        this.app_goods_categoryOptions.push(data);
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
        categoryId: null,
        parentId: null,
        parentIds: null,
        categoryName: null,
        categoryIcon: null,
        isHot: null,
        linkType: null,
        linkId: null,
        remark: null,
        orderNum: null,
        status: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      this.getTreeselect();
      if (row != null && row.categoryId) {
        this.form.parentId = row.categoryId;
      } else {
        this.form.parentId = 0;
      }
      this.open = true;
      this.title = "添加商品分类";
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => {
        this.refreshTable = true;
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      if (row != null) {
        this.form.parentId = row.parentId;
      }
      getApp_goods_category(row.categoryId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改商品分类";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.categoryId != null) {
            updateApp_goods_category(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_category(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除商品分类编号为"' + row.categoryId + '"的数据项？').then(function() {
        return delApp_goods_category(row.categoryId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>
