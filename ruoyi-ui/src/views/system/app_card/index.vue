<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="卡名称" prop="cardName">
        <el-input
          v-model="queryParams.cardName"
          placeholder="请输入卡名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="卡片标识" prop="cardKey">
        <el-input
          v-model="queryParams.cardKey"
          placeholder="请输入卡片标识"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="卡状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择卡状态" clearable>
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
          v-hasPermi="['system:app_card:add']"
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
          v-hasPermi="['system:app_card:edit']"
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
          v-hasPermi="['system:app_card:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_card:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_cardList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="卡id" align="center" prop="cardId" />
      <el-table-column label="卡名称" align="center" prop="cardName" />
      <el-table-column label="卡片标识" align="center" prop="cardKey" />
      <el-table-column label="卡片图片" align="center" prop="cardImage" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.cardImage" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="卡介绍" align="center" prop="description" />
      <el-table-column label="价格" align="center" prop="price" />
      <el-table-column label="有效期（天）" align="center" prop="expiration" />
      <el-table-column label="卡状态" align="center" prop="status">
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
            v-hasPermi="['system:app_card:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_card:remove']"
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

    <!-- 添加或修改会员卡对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="卡名称" prop="cardName">
          <el-input v-model="form.cardName" placeholder="请输入卡名称" />
        </el-form-item>
        <el-form-item label="卡片标识" prop="cardKey">
          <el-input v-model="form.cardKey" placeholder="请输入卡片标识" />
        </el-form-item>
        <el-form-item label="卡片图片" prop="cardImage">
          <image-upload v-model="form.cardImage"/>
        </el-form-item>
        <el-form-item label="卡介绍" prop="description">
          <el-input v-model="form.description" placeholder="请输入卡介绍" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input v-model="form.price" placeholder="请输入卡价格" />
        </el-form-item>
        <el-form-item label="有效期" prop="expiration">
          <el-input v-model="form.expiration" placeholder="请输入卡有效期（天）" />
        </el-form-item>
        <el-form-item label="详细说明">
          <editor v-model="form.content" :min-height="192"/>
        </el-form-item>
        <el-form-item label="卡状态" prop="status">
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
import { listApp_card, getApp_card, delApp_card, addApp_card, updateApp_card } from "@/api/system/app_card";

export default {
  name: "App_card",
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
      // 会员卡表格数据
      app_cardList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        cardName: null,
        cardKey: null,
        cardImage: null,
        description: null,
        content: null,
        discountType: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        cardName: [
          { required: true, message: "卡名称不能为空", trigger: "blur" }
        ],
        cardKey: [
          { required: true, message: "卡片标识不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "卡状态不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询会员卡列表 */
    getList() {
      this.loading = true;
      listApp_card(this.queryParams).then(response => {
        this.app_cardList = response.rows;
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
        cardId: null,
        cardName: null,
        cardKey: null,
        cardImage: null,
        description: null,
        content: null,
        discountType: null,
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
      this.ids = selection.map(item => item.cardId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加会员卡";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const cardId = row.cardId || this.ids
      getApp_card(cardId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改会员卡";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.cardId != null) {
            updateApp_card(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_card(this.form).then(response => {
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
      const cardIds = row.cardId || this.ids;
      this.$modal.confirm('是否确认删除会员卡编号为"' + cardIds + '"的数据项？').then(function() {
        return delApp_card(cardIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_card/export', {
        ...this.queryParams
      }, `app_card_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
