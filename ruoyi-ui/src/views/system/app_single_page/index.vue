<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="页面标题" prop="pageName">
        <el-input
          v-model="queryParams.pageName"
          placeholder="请输入页面标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="页面标识" prop="pageKey">
        <el-input
          v-model="queryParams.pageKey"
          placeholder="请输入页面标识"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="页面关键字" prop="keywords">
        <el-input
          v-model="queryParams.keywords"
          placeholder="请输入页面关键字"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="是否热门" prop="isHot">-->
<!--        <el-select v-model="queryParams.isHot" placeholder="请选择是否热门" clearable>-->
<!--          <el-option-->
<!--            v-for="dict in dict.type.common_is_not"-->
<!--            :key="dict.value"-->
<!--            :label="dict.label"-->
<!--            :value="dict.value"-->
<!--          />-->
<!--        </el-select>-->
<!--      </el-form-item>-->
      <el-form-item label="阅读次数" prop="viewCount">
        <el-input
          v-model="queryParams.viewCount"
          placeholder="请输入阅读次数"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="页面状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择页面状态" clearable>
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
          v-hasPermi="['system:app_single_page:add']"
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
          v-hasPermi="['system:app_single_page:edit']"
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
          v-hasPermi="['system:app_single_page:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_single_page:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_single_pageList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="页面id" align="center" prop="pageId" />
      <el-table-column label="页面标题" align="center" prop="pageName" />
      <el-table-column label="页面标识" align="center" prop="pageKey" />
      <el-table-column label="封面图片" align="center" prop="pageCover" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.pageCover" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="页面关键字" align="center" prop="keywords" />
<!--      <el-table-column label="是否热门" align="center" prop="isHot">-->
<!--        <template slot-scope="scope">-->
<!--          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isHot"/>-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column label="阅读次数" align="center" prop="viewCount" />
      <el-table-column label="页面状态" align="center" prop="status">
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
            v-hasPermi="['system:app_single_page:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_single_page:remove']"
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

    <!-- 添加或修改单页文章对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="页面标题" prop="pageName">
          <el-input v-model="form.pageName" placeholder="请输入页面标题" />
        </el-form-item>
        <el-form-item label="页面标识" prop="pageKey">
          <el-input v-model="form.pageKey" placeholder="请输入页面标识" />
        </el-form-item>
        <el-form-item label="封面图片" prop="pageCover">
          <image-upload v-model="form.pageCover"/>
        </el-form-item>
        <el-form-item label="页面描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="页面关键字" prop="keywords">
          <el-input v-model="form.keywords" placeholder="请输入页面关键字" />
        </el-form-item>
<!--        <el-form-item label="是否热门" prop="isHot">-->
<!--          <el-radio-group v-model="form.isHot">-->
<!--            <el-radio-->
<!--              v-for="dict in dict.type.common_is_not"-->
<!--              :key="dict.value"-->
<!--              :label="parseInt(dict.value)"-->
<!--            >{{dict.label}}</el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-form-item>-->
        <el-form-item label="页面内容">
          <editor v-model="form.content" :min-height="192"/>
        </el-form-item>
        <el-form-item label="阅读次数" prop="viewCount">
          <el-input v-model="form.viewCount" placeholder="请输入阅读次数" />
        </el-form-item>
        <el-form-item label="页面状态" prop="status">
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
import { listApp_single_page, getApp_single_page, delApp_single_page, addApp_single_page, updateApp_single_page } from "@/api/system/app_single_page";

export default {
  name: "App_single_page",
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
      // 单页文章表格数据
      app_single_pageList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        pageName: null,
        pageKey: null,
        pageCover: null,
        description: null,
        keywords: null,
        isHot: null,
        content: null,
        viewCount: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        pageName: [
          { required: true, message: "页面标题不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询单页文章列表 */
    getList() {
      this.loading = true;
      listApp_single_page(this.queryParams).then(response => {
        this.app_single_pageList = response.rows;
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
        pageId: null,
        pageName: null,
        pageKey: null,
        pageCover: null,
        description: null,
        keywords: null,
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
      this.ids = selection.map(item => item.pageId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加单页文章";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const pageId = row.pageId || this.ids
      getApp_single_page(pageId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改单页文章";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.pageId != null) {
            updateApp_single_page(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_single_page(this.form).then(response => {
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
      const pageIds = row.pageId || this.ids;
      this.$modal.confirm('是否确认删除单页文章编号为"' + pageIds + '"的数据项？').then(function() {
        return delApp_single_page(pageIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_single_page/export', {
        ...this.queryParams
      }, `app_single_page_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
