<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属分类" prop="categoryId">
        <el-input
          v-model="queryParams.categoryId"
          placeholder="请输入所属分类"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="祖级分类" prop="categoryIds">
        <el-input
          v-model="queryParams.categoryIds"
          placeholder="请输入祖级分类"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="内容标题" prop="articleName">
        <el-input
          v-model="queryParams.articleName"
          placeholder="请输入内容标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="内容作者" prop="author">
        <el-input
          v-model="queryParams.author"
          placeholder="请输入内容作者"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="内容标签" prop="tags">
        <el-input
          v-model="queryParams.tags"
          placeholder="请输入内容标签"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否置顶" prop="isTop">
        <el-select v-model="queryParams.isTop" placeholder="请选择是否置顶" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否热门" prop="isHot">
        <el-select v-model="queryParams.isHot" placeholder="请选择是否热门" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="阅读次数" prop="viewCount">
        <el-input
          v-model="queryParams.viewCount"
          placeholder="请输入阅读次数"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="内容状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择内容状态" clearable>
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
          v-hasPermi="['system:app_article:add']"
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
          v-hasPermi="['system:app_article:edit']"
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
          v-hasPermi="['system:app_article:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_article:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="内容ID" align="center" prop="articleId" />
      <el-table-column label="所属分类" align="center" prop="categoryId" />
      <el-table-column label="祖级分类" align="center" prop="categoryIds" />
      <el-table-column label="内容标题" align="center" prop="articleName" />
      <el-table-column label="内容作者" align="center" prop="author" />
      <el-table-column label="封面图片" align="center" prop="articleCover" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.articleCover" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="内容简介" align="center" prop="description" />
      <el-table-column label="内容标签" align="center" prop="tags" />
      <el-table-column label="是否置顶" align="center" prop="isTop">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isTop"/>
        </template>
      </el-table-column>
      <el-table-column label="是否热门" align="center" prop="isHot">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isHot"/>
        </template>
      </el-table-column>
      <el-table-column label="详细内容" align="center" prop="content" />
      <el-table-column label="阅读次数" align="center" prop="viewCount" />
      <el-table-column label="内容状态" align="center" prop="status">
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
            v-hasPermi="['system:app_article:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_article:remove']"
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

    <!-- 添加或修改图文内容对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属分类" prop="categoryId">
          <el-input v-model="form.categoryId" placeholder="请输入所属分类" />
        </el-form-item>
        <el-form-item label="祖级分类" prop="categoryIds">
          <el-input v-model="form.categoryIds" placeholder="请输入祖级分类" />
        </el-form-item>
        <el-form-item label="内容标题" prop="articleName">
          <el-input v-model="form.articleName" placeholder="请输入内容标题" />
        </el-form-item>
        <el-form-item label="内容作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入内容作者" />
        </el-form-item>
        <el-form-item label="封面图片" prop="articleCover">
          <image-upload v-model="form.articleCover"/>
        </el-form-item>
        <el-form-item label="内容简介" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="内容标签" prop="tags">
          <el-input v-model="form.tags" placeholder="请输入内容标签" />
        </el-form-item>
        <el-form-item label="是否置顶" prop="isTop">
          <el-radio-group v-model="form.isTop">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否热门" prop="isHot">
          <el-radio-group v-model="form.isHot">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细内容">
          <editor v-model="form.content" :min-height="192"/>
        </el-form-item>
        <el-form-item label="阅读次数" prop="viewCount">
          <el-input v-model="form.viewCount" placeholder="请输入阅读次数" />
        </el-form-item>
        <el-form-item label="内容状态" prop="status">
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
import { listApp_article, getApp_article, delApp_article, addApp_article, updateApp_article } from "@/api/system/app_article";

export default {
  name: "App_article",
  dicts: ['common_is_not', 'enable_status'],
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
      // 图文内容表格数据
      app_articleList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        categoryId: null,
        categoryIds: null,
        articleName: null,
        author: null,
        articleCover: null,
        description: null,
        tags: null,
        isTop: null,
        isHot: null,
        content: null,
        viewCount: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        articleName: [
          { required: true, message: "内容标题不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询图文内容列表 */
    getList() {
      this.loading = true;
      listApp_article(this.queryParams).then(response => {
        this.app_articleList = response.rows;
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
        articleId: null,
        categoryId: null,
        categoryIds: null,
        articleName: null,
        author: null,
        articleCover: null,
        description: null,
        tags: null,
        isTop: null,
        isHot: null,
        content: null,
        viewCount: null,
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
      this.ids = selection.map(item => item.articleId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加图文内容";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const articleId = row.articleId || this.ids
      getApp_article(articleId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改图文内容";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.articleId != null) {
            updateApp_article(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_article(this.form).then(response => {
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
      const articleIds = row.articleId || this.ids;
      this.$modal.confirm('是否确认删除图文内容编号为"' + articleIds + '"的数据项？').then(function() {
        return delApp_article(articleIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_article/export', {
        ...this.queryParams
      }, `app_article_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
