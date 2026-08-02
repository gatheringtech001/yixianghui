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
      <el-form-item label="所属商品" prop="goodsId">
        <el-input
          v-model="queryParams.goodsId"
          placeholder="请输入所属商品"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属订单" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入所属订单"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属详单" prop="detailId">
        <el-input
          v-model="queryParams.detailId"
          placeholder="请输入所属详单"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品评分" prop="goodsStar">
        <el-input
          v-model="queryParams.goodsStar"
          placeholder="请输入商品评分"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物流评分" prop="expressStar">
        <el-input
          v-model="queryParams.expressStar"
          placeholder="请输入物流评分"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="服务评分" prop="waiterStar">
        <el-input
          v-model="queryParams.waiterStar"
          placeholder="请输入服务评分"
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
          v-hasPermi="['system:app_goods_comment:add']"
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
          v-hasPermi="['system:app_goods_comment:edit']"
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
          v-hasPermi="['system:app_goods_comment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_goods_comment:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_goods_commentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="评价id" align="center" prop="commentId" v-if="false"/>
      <el-table-column label="所属用户id" align="center" prop="userId" v-if="false"/>
      <el-table-column label="所属用户" align="center" prop="userName" min-width="80" show-overflow-tooltip/>
      <el-table-column label="所属商品id" align="center" prop="goodsId" v-if="false"/>
      <el-table-column label="所属商品" align="center" prop="goodsName" min-width="100" show-overflow-tooltip/>
      <el-table-column label="所属订单id" align="center" prop="orderId" v-if="false"/>
      <el-table-column label="所属订单" align="center" prop="appGoodsOrderNo" min-width="120" show-overflow-tooltip/>
      <el-table-column label="所属详单" align="center" prop="detailId" v-if="false"/>
      <el-table-column label="评价标签" align="center" prop="commentTags" show-overflow-tooltip/>
      <el-table-column label="评价内容" align="center" prop="commentContent" show-overflow-tooltip/>
      <el-table-column label="评论图片" align="center" prop="commentImages" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.commentImages" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="商品评分" align="center" prop="goodsStar" />
      <el-table-column label="物流评分" align="center" prop="expressStar" />
      <el-table-column label="服务评分" align="center" prop="waiterStar" />
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
            v-hasPermi="['system:app_goods_comment:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_goods_comment:remove']"
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

    <!-- 添加或修改商品评价对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属用户" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入所属用户" />
        </el-form-item>
        <el-form-item label="所属商品" prop="goodsId">
          <el-input v-model="form.goodsId" placeholder="请输入所属商品" />
        </el-form-item>
        <el-form-item label="所属订单" prop="orderId">
          <el-input v-model="form.orderId" placeholder="请输入所属订单" />
        </el-form-item>
        <el-form-item label="所属详单" prop="detailId">
          <el-input v-model="form.detailId" placeholder="请输入所属详单" />
        </el-form-item>
        <el-form-item label="评价标签" prop="commentTags">
          <el-input v-model="form.commentTags" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="评价内容" prop="commentContent">
          <el-input v-model="form.commentContent" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="评论图片" prop="commentImages">
          <image-upload v-model="form.commentImages"/>
        </el-form-item>
        <el-form-item label="商品评分" prop="goodsStar">
          <el-input v-model="form.goodsStar" placeholder="请输入商品评分" />
        </el-form-item>
        <el-form-item label="物流评分" prop="expressStar">
          <el-input v-model="form.expressStar" placeholder="请输入物流评分" />
        </el-form-item>
        <el-form-item label="服务评分" prop="waiterStar">
          <el-input v-model="form.waiterStar" placeholder="请输入服务评分" />
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
import { listApp_goods_comment, getApp_goods_comment, delApp_goods_comment, addApp_goods_comment, updateApp_goods_comment } from "@/api/system/app_goods_comment";

export default {
  name: "App_goods_comment",
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
      // 商品评价表格数据
      app_goods_commentList: [],
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
        orderId: null,
        detailId: null,
        commentTags: null,
        commentContent: null,
        commentImages: null,
        goodsStar: null,
        expressStar: null,
        waiterStar: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          { required: true, message: "所属用户不能为空", trigger: "blur" }
        ],
        goodsId: [
          { required: true, message: "所属商品不能为空", trigger: "blur" }
        ],
        orderId: [
          { required: true, message: "所属订单不能为空", trigger: "blur" }
        ],
        detailId: [
          { required: true, message: "所属详单不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询商品评价列表 */
    getList() {
      this.loading = true;
      listApp_goods_comment(this.queryParams).then(response => {
        this.app_goods_commentList = response.rows;
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
        commentId: null,
        userId: null,
        goodsId: null,
        orderId: null,
        detailId: null,
        commentTags: null,
        commentContent: null,
        commentImages: null,
        goodsStar: null,
        expressStar: null,
        waiterStar: null,
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
      this.ids = selection.map(item => item.commentId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加商品评价";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const commentId = row.commentId || this.ids
      getApp_goods_comment(commentId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改商品评价";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.commentId != null) {
            updateApp_goods_comment(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_comment(this.form).then(response => {
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
      const commentIds = row.commentId || this.ids;
      this.$modal.confirm('是否确认删除商品评价编号为"' + commentIds + '"的数据项？').then(function() {
        return delApp_goods_comment(commentIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_comment/export', {
        ...this.queryParams
      }, `app_goods_comment_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
