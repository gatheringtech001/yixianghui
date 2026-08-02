<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="快递名称" prop="expressName">
        <el-input
          v-model="queryParams.expressName"
          placeholder="请输入快递名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="英文名称" prop="simpleName">
        <el-input
          v-model="queryParams.simpleName"
          placeholder="请输入英文名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="服务号码" prop="expressPhone">
        <el-input
          v-model="queryParams.expressPhone"
          placeholder="请输入服务号码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="官方网址" prop="expressUrl">
        <el-input
          v-model="queryParams.expressUrl"
          placeholder="请输入官方网址"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="首重价格" prop="firstPrice">
        <el-input
          v-model="queryParams.firstPrice"
          placeholder="请输入首重价格"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="续重价格" prop="secondPrice">
        <el-input
          v-model="queryParams.secondPrice"
          placeholder="请输入续重价格"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="快递状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择快递状态" clearable>
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
          v-hasPermi="['system:app_express:add']"
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
          v-hasPermi="['system:app_express:edit']"
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
          v-hasPermi="['system:app_express:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_express:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_expressList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="快递id" align="center" prop="expressId" v-if="false"/>
      <el-table-column label="快递名称" align="center" prop="expressName" />
      <el-table-column label="英文名称" align="center" prop="simpleName" />
      <el-table-column label="快递图片" align="center" prop="expressImage" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.expressImage" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="服务号码" align="center" prop="expressPhone" />
      <el-table-column label="官方网址" align="center" prop="expressUrl" />
      <el-table-column label="说明" align="center" prop="description" />
      <el-table-column label="首重价格" align="center" prop="firstPrice" />
      <el-table-column label="续重价格" align="center" prop="secondPrice" />
      <el-table-column label="快递状态" align="center" prop="status">
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
            v-hasPermi="['system:app_express:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_express:remove']"
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

    <!-- 添加或修改快递公司对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="快递名称" prop="expressName">
          <el-input v-model="form.expressName" placeholder="请输入快递名称" />
        </el-form-item>
        <el-form-item label="英文名称" prop="simpleName">
          <el-input v-model="form.simpleName" placeholder="请输入英文名称" />
        </el-form-item>
        <el-form-item label="快递图片" prop="expressImage">
          <image-upload v-model="form.expressImage"/>
        </el-form-item>
        <el-form-item label="服务号码" prop="expressPhone">
          <el-input v-model="form.expressPhone" placeholder="请输入服务号码" />
        </el-form-item>
        <el-form-item label="官方网址" prop="expressUrl">
          <el-input v-model="form.expressUrl" placeholder="请输入官方网址" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="首重价格" prop="firstPrice">
          <el-input v-model="form.firstPrice" placeholder="请输入首重价格" />
        </el-form-item>
        <el-form-item label="续重价格" prop="secondPrice">
          <el-input v-model="form.secondPrice" placeholder="请输入续重价格" />
        </el-form-item>
        <el-form-item label="快递状态" prop="status">
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
import { listApp_express, getApp_express, delApp_express, addApp_express, updateApp_express } from "@/api/system/app_express";

export default {
  name: "App_express",
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
      // 快递公司表格数据
      app_expressList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        expressName: null,
        simpleName: null,
        expressImage: null,
        expressPhone: null,
        expressUrl: null,
        description: null,
        firstPrice: null,
        secondPrice: null,
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
    /** 查询快递公司列表 */
    getList() {
      this.loading = true;
      listApp_express(this.queryParams).then(response => {
        this.app_expressList = response.rows;
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
        expressId: null,
        expressName: null,
        simpleName: null,
        expressImage: null,
        expressPhone: null,
        expressUrl: null,
        description: null,
        firstPrice: null,
        secondPrice: null,
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
      this.ids = selection.map(item => item.expressId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加快递公司";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const expressId = row.expressId || this.ids
      getApp_express(expressId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改快递公司";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.expressId != null) {
            updateApp_express(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_express(this.form).then(response => {
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
      const expressIds = row.expressId || this.ids;
      this.$modal.confirm('是否确认删除快递公司编号为"' + expressIds + '"的数据项？').then(function() {
        return delApp_express(expressIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_express/export', {
        ...this.queryParams
      }, `app_express_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
