<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
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
      <el-form-item label="商品ID" prop="goodsId">
        <el-input
          v-model="queryParams.goodsId"
          placeholder="请输入商品ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品数量" prop="goodsCount">
        <el-input
          v-model="queryParams.goodsCount"
          placeholder="请输入商品数量"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品小计金额" prop="goodsMoney">
        <el-input
          v-model="queryParams.goodsMoney"
          placeholder="请输入商品小计金额"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="折扣金额" prop="discountMoney">
        <el-input
          v-model="queryParams.discountMoney"
          placeholder="请输入折扣金额"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否sku商品" prop="isSku">
        <el-select v-model="queryParams.isSku" placeholder="请选择是否sku商品" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="sku数据id" prop="skuDataId">
        <el-input
          v-model="queryParams.skuDataId"
          placeholder="请输入sku数据id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退货id" prop="goodsBackId">
        <el-input
          v-model="queryParams.goodsBackId"
          placeholder="请输入退货id"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退款金额" prop="refundMoney">
        <el-input
          v-model="queryParams.refundMoney"
          placeholder="请输入退款金额"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否已评论" prop="isComment">
        <el-input
          v-model="queryParams.isComment"
          placeholder="请输入是否已评论"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="详单状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择详单状态" clearable>
          <el-option
            v-for="dict in dict.type.order_status"
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
          v-hasPermi="['system:app_goods_order_detail:add']"
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
          v-hasPermi="['system:app_goods_order_detail:edit']"
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
          v-hasPermi="['system:app_goods_order_detail:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_goods_order_detail:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_goods_order_detailList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="详单id" align="center" prop="detailId" />
      <el-table-column label="用户ID" align="center" prop="userId" />
      <el-table-column label="所属订单" align="center" prop="orderId" />
      <el-table-column label="商品ID" align="center" prop="goodsId" />
      <el-table-column label="商品数量" align="center" prop="goodsCount" />
      <el-table-column label="商品小计金额" align="center" prop="goodsMoney" />
      <el-table-column label="折扣金额" align="center" prop="discountMoney" />
      <el-table-column label="是否sku商品" align="center" prop="isSku">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isSku"/>
        </template>
      </el-table-column>
      <el-table-column label="sku数据id" align="center" prop="skuDataId" />
      <el-table-column label="sku数据描述" align="center" prop="skuDataValues" />
      <el-table-column label="退货id" align="center" prop="goodsBackId" />
      <el-table-column label="退款金额" align="center" prop="refundMoney" />
      <el-table-column label="商品备注" align="center" prop="remark" />
      <el-table-column label="是否已评论" align="center" prop="isComment" />
      <el-table-column label="详单状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.order_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_goods_order_detail:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_goods_order_detail:remove']"
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

    <!-- 添加或修改订单详细对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="所属订单" prop="orderId">
          <el-input v-model="form.orderId" placeholder="请输入所属订单" />
        </el-form-item>
        <el-form-item label="商品ID" prop="goodsId">
          <el-input v-model="form.goodsId" placeholder="请输入商品ID" />
        </el-form-item>
        <el-form-item label="商品数量" prop="goodsCount">
          <el-input v-model="form.goodsCount" placeholder="请输入商品数量" />
        </el-form-item>
        <el-form-item label="商品小计金额" prop="goodsMoney">
          <el-input v-model="form.goodsMoney" placeholder="请输入商品小计金额" />
        </el-form-item>
        <el-form-item label="折扣金额" prop="discountMoney">
          <el-input v-model="form.discountMoney" placeholder="请输入折扣金额" />
        </el-form-item>
        <el-form-item label="是否sku商品" prop="isSku">
          <el-radio-group v-model="form.isSku">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="sku数据id" prop="skuDataId">
          <el-input v-model="form.skuDataId" placeholder="请输入sku数据id" />
        </el-form-item>
        <el-form-item label="sku数据描述" prop="skuDataValues">
          <el-input v-model="form.skuDataValues" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="退货id" prop="goodsBackId">
          <el-input v-model="form.goodsBackId" placeholder="请输入退货id" />
        </el-form-item>
        <el-form-item label="退款金额" prop="refundMoney">
          <el-input v-model="form.refundMoney" placeholder="请输入退款金额" />
        </el-form-item>
        <el-form-item label="商品备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入商品备注" />
        </el-form-item>
        <el-form-item label="是否已评论" prop="isComment">
          <el-input v-model="form.isComment" placeholder="请输入是否已评论" />
        </el-form-item>
        <el-form-item label="详单状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.order_status"
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
import { listApp_goods_order_detail, getApp_goods_order_detail, delApp_goods_order_detail, addApp_goods_order_detail, updateApp_goods_order_detail } from "@/api/system/app_goods_order_detail";

export default {
  name: "App_goods_order_detail",
  dicts: ['common_is_not', 'order_status'],
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
      // 订单详细表格数据
      app_goods_order_detailList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        orderId: null,
        goodsId: null,
        goodsCount: null,
        goodsMoney: null,
        discountMoney: null,
        isSku: null,
        skuDataId: null,
        skuDataValues: null,
        goodsBackId: null,
        refundMoney: null,
        isComment: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          { required: true, message: "用户ID不能为空", trigger: "blur" }
        ],
        goodsId: [
          { required: true, message: "商品ID不能为空", trigger: "blur" }
        ],
        isComment: [
          { required: true, message: "是否已评论不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "详单状态不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询订单详细列表 */
    getList() {
      this.loading = true;
      listApp_goods_order_detail(this.queryParams).then(response => {
        this.app_goods_order_detailList = response.rows;
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
        detailId: null,
        userId: null,
        orderId: null,
        goodsId: null,
        goodsCount: null,
        goodsMoney: null,
        discountMoney: null,
        isSku: null,
        skuDataId: null,
        skuDataValues: null,
        goodsBackId: null,
        refundMoney: null,
        remark: null,
        isComment: null,
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
      this.ids = selection.map(item => item.detailId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加订单详细";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const detailId = row.detailId || this.ids
      getApp_goods_order_detail(detailId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改订单详细";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.detailId != null) {
            updateApp_goods_order_detail(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_order_detail(this.form).then(response => {
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
      const detailIds = row.detailId || this.ids;
      this.$modal.confirm('是否确认删除订单详细编号为"' + detailIds + '"的数据项？').then(function() {
        return delApp_goods_order_detail(detailIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_order_detail/export', {
        ...this.queryParams
      }, `app_goods_order_detail_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
