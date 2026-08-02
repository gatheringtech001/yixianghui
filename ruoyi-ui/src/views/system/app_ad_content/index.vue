<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属广告位" prop="positionId">
        <el-input
          v-model="queryParams.positionId"
          placeholder="请输入所属广告位"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="广告标题" prop="adName">
        <el-input
          v-model="queryParams.adName"
          placeholder="请输入广告标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable
          v-model="queryParams.startTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable
          v-model="queryParams.endTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择结束时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="广告链接" prop="linkUrl">
        <el-input
          v-model="queryParams.linkUrl"
          placeholder="请输入广告链接"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排序" prop="orderNum">
        <el-input
          v-model="queryParams.orderNum"
          placeholder="请输入排序"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="广告状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择广告状态" clearable>
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
          v-hasPermi="['system:app_ad_content:add']"
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
          v-hasPermi="['system:app_ad_content:edit']"
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
          v-hasPermi="['system:app_ad_content:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_ad_content:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_ad_contentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="广告内容id" align="center" prop="contentId" />
      <el-table-column label="所属广告位" align="center" prop="positionId" />
      <el-table-column label="广告标题" align="center" prop="adName" />
      <el-table-column label="广告介绍" align="center" prop="description" />
      <el-table-column label="广告图片" align="center" prop="adImage" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.adImage" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="富文本" align="center" prop="adContent" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="广告链接" align="center" prop="linkUrl" />
      <el-table-column label="排序" align="center" prop="orderNum" />
      <el-table-column label="广告状态" align="center" prop="status">
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
            v-hasPermi="['system:app_ad_content:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_ad_content:remove']"
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

    <!-- 添加或修改广告内容对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属广告位" prop="positionId">
          <el-input v-model="form.positionId" placeholder="请输入所属广告位" />
        </el-form-item>
        <el-form-item label="广告标题" prop="adName">
          <el-input v-model="form.adName" placeholder="请输入广告标题" />
        </el-form-item>
        <el-form-item label="广告介绍" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="广告图片" prop="adImage">
          <image-upload v-model="form.adImage"/>
        </el-form-item>
        <el-form-item label="富文本" prop="adContent">
          <editor v-model="form.adContent" :min-height="192"/>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker clearable
            v-model="form.startTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择开始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker clearable
            v-model="form.endTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择结束时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="广告链接" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="请输入广告链接" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input v-model="form.orderNum" placeholder="请输入排序" />
        </el-form-item>
        <el-form-item label="广告状态" prop="status">
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
import { listApp_ad_content, getApp_ad_content, delApp_ad_content, addApp_ad_content, updateApp_ad_content } from "@/api/system/app_ad_content";

export default {
  name: "App_ad_content",
  dicts: ['enable_status'],
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
      // 广告内容表格数据
      app_ad_contentList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        positionId: null,
        adName: null,
        description: null,
        adImage: null,
        adContent: null,
        startTime: null,
        endTime: null,
        linkUrl: null,
        orderNum: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        positionId: [
          { required: true, message: "所属广告位不能为空", trigger: "blur" }
        ],
        adName: [
          { required: true, message: "广告标题不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "广告状态不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询广告内容列表 */
    getList() {
      this.loading = true;
      listApp_ad_content(this.queryParams).then(response => {
        this.app_ad_contentList = response.rows;
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
        contentId: null,
        positionId: null,
        adName: null,
        description: null,
        adImage: null,
        adContent: null,
        startTime: null,
        endTime: null,
        linkUrl: null,
        orderNum: null,
        status: null
      };
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate();
        }
      });
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
      this.ids = selection.map(item => item.contentId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加广告内容";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      const contentId = row.contentId || this.ids
      getApp_ad_content(contentId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改广告内容";
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.clearValidate();
          }
        });
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.contentId != null) {
            updateApp_ad_content(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_ad_content(this.form).then(response => {
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
      const contentIds = row.contentId || this.ids;
      this.$modal.confirm('是否确认删除广告内容编号为"' + contentIds + '"的数据项？').then(function() {
        return delApp_ad_content(contentIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_ad_content/export', {
        ...this.queryParams
      }, `app_ad_content_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
