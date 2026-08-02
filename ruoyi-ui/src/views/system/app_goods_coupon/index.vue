<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="发布人" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入发布人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="限定分类" prop="categoryId">-->
<!--        <el-input-->
<!--          v-model="queryParams.categoryId"-->
<!--          placeholder="请输入限定分类"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="限定商品" prop="goodsId">-->
<!--        <el-input-->
<!--          v-model="queryParams.goodsId"-->
<!--          placeholder="请输入限定商品"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="优惠券名称" prop="couponName">
        <el-input
          v-model="queryParams.couponName"
          placeholder="请输入优惠券名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="优惠券类型" prop="couponType">
        <el-select v-model="queryParams.couponType" placeholder="请选择优惠券类型" clearable>
          <el-option
            v-for="dict in dict.type.coupon_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
<!--      <el-form-item label="满多少可用" prop="minPrice">-->
<!--        <el-input-->
<!--          v-model="queryParams.minPrice"-->
<!--          placeholder="请输入满多少可用"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="折扣方式" prop="discountType">-->
<!--        <el-select v-model="queryParams.discountType" placeholder="请选择折扣方式" clearable>-->
<!--          <el-option-->
<!--            v-for="dict in dict.type.coupon_type"-->
<!--            :key="dict.value"-->
<!--            :label="dict.label"-->
<!--            :value="dict.value"-->
<!--          />-->
<!--        </el-select>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="折扣金额" prop="discountPrice">-->
<!--        <el-input-->
<!--          v-model="queryParams.discountPrice"-->
<!--          placeholder="请输入折扣金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="每单可用张数" prop="countPerOrder">-->
<!--        <el-input-->
<!--          v-model="queryParams.countPerOrder"-->
<!--          placeholder="请输入每单可用张数"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="发放总量" prop="couponTotal">-->
<!--        <el-input-->
<!--          v-model="queryParams.couponTotal"-->
<!--          placeholder="请输入发放总量"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="每人可领数量" prop="countPerUser">-->
<!--        <el-input-->
<!--          v-model="queryParams.countPerUser"-->
<!--          placeholder="请输入每人可领数量"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="已领取数量" prop="couponGotCount">-->
<!--        <el-input-->
<!--          v-model="queryParams.couponGotCount"-->
<!--          placeholder="请输入已领取数量"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="已使用数量" prop="couponUsedCount">-->
<!--        <el-input-->
<!--          v-model="queryParams.couponUsedCount"-->
<!--          placeholder="请输入已使用数量"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="可用开始时间" prop="enableStartTime">-->
<!--        <el-date-picker clearable-->
<!--          v-model="queryParams.enableStartTime"-->
<!--          type="date"-->
<!--          value-format="yyyy-MM-dd"-->
<!--          placeholder="请选择可用开始时间">-->
<!--        </el-date-picker>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="可用结束时间" prop="enableEndTime">-->
<!--        <el-date-picker clearable-->
<!--          v-model="queryParams.enableEndTime"-->
<!--          type="date"-->
<!--          value-format="yyyy-MM-dd"-->
<!--          placeholder="请选择可用结束时间">-->
<!--        </el-date-picker>-->
<!--      </el-form-item>-->
      <el-form-item label="领取方式" prop="getMethod">
        <el-input
          v-model="queryParams.getMethod"
          placeholder="请输入领取方式"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="优惠券状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择优惠券状态" clearable>
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
          v-hasPermi="['system:app_goods_coupon:add']"
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
          v-hasPermi="['system:app_goods_coupon:edit']"
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
          v-hasPermi="['system:app_goods_coupon:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_goods_coupon:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_goods_couponList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="优惠券id" align="center" prop="couponId" />
      <el-table-column label="发布人" align="center" prop="userId" />
<!--      <el-table-column label="限定分类" align="center" prop="categoryId" />-->
<!--      <el-table-column label="限定商品" align="center" prop="goodsId" />-->
      <el-table-column label="优惠券名称" align="center" prop="couponName" />
<!--      <el-table-column label="优惠券说明" align="center" prop="couponContent" />-->
      <el-table-column label="优惠券类型" align="center" prop="couponType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.coupon_type" :value="scope.row.couponType"/>
        </template>
      </el-table-column>
      <el-table-column label="满多少可用" align="center" prop="minPrice" />
<!--      <el-table-column label="折扣方式" align="center" prop="discountType">-->
<!--        <template slot-scope="scope">-->
<!--          <dict-tag :options="dict.type.coupon_type" :value="scope.row.discountType"/>-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column label="折扣金额" align="center" prop="discountPrice" />
      <el-table-column label="每单可用张数" align="center" prop="countPerOrder" />
      <el-table-column label="发放总量" align="center" prop="couponTotal" />
      <el-table-column label="每人可领数量" align="center" prop="countPerUser" />
      <el-table-column label="已领取数量" align="center" prop="couponGotCount" />
      <el-table-column label="已使用数量" align="center" prop="couponUsedCount" />
      <el-table-column label="可用开始时间" align="center" prop="enableStartTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.enableStartTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="可用结束时间" align="center" prop="enableEndTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.enableEndTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="领取方式" align="center" prop="getMethod">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.coupon_get_method" :value="scope.row.getMethod"/>
        </template>
      </el-table-column>
      <el-table-column label="优惠券状态" align="center" prop="status">
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
            v-hasPermi="['system:app_goods_coupon:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_goods_coupon:remove']"
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

    <!-- 添加或修改商品优惠券对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="发布人" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入发布人" />
        </el-form-item>
        <el-form-item label="限定分类" prop="categoryId">
          <el-input v-model="form.categoryId" placeholder="请输入限定分类" />
        </el-form-item>
        <el-form-item label="限定商品" prop="goodsId">
          <el-input v-model="form.goodsId" placeholder="请输入限定商品" />
        </el-form-item>
        <el-form-item label="优惠券名称" prop="couponName">
          <el-input v-model="form.couponName" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="优惠券说明">
          <editor v-model="form.couponContent" :min-height="192"/>
        </el-form-item>
        <el-form-item label="优惠券类型" prop="couponType">
          <el-select v-model="form.couponType" placeholder="请选择优惠券类型">
            <el-option
              v-for="dict in dict.type.coupon_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="满多少可用" prop="minPrice">
          <el-input v-model="form.minPrice" placeholder="请输入满多少可用" />
        </el-form-item>
        <el-form-item label="折扣方式" prop="discountType">
          <el-select v-model="form.discountType" placeholder="请选择折扣方式">
            <el-option
              v-for="dict in dict.type.coupon_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="折扣金额" prop="discountPrice">
          <el-input v-model="form.discountPrice" placeholder="请输入折扣金额" />
        </el-form-item>
        <el-form-item label="每单可用张数" prop="countPerOrder">
          <el-input v-model="form.countPerOrder" placeholder="请输入每单可用张数" />
        </el-form-item>
        <el-form-item label="发放总量" prop="couponTotal">
          <el-input v-model="form.couponTotal" placeholder="请输入发放总量" />
        </el-form-item>
        <el-form-item label="每人可领数量" prop="countPerUser">
          <el-input v-model="form.countPerUser" placeholder="请输入每人可领数量" />
        </el-form-item>
        <el-form-item label="已领取数量" prop="couponGotCount">
          <el-input v-model="form.couponGotCount" placeholder="请输入已领取数量" />
        </el-form-item>
        <el-form-item label="已使用数量" prop="couponUsedCount">
          <el-input v-model="form.couponUsedCount" placeholder="请输入已使用数量" />
        </el-form-item>
        <el-form-item label="可用开始时间" prop="enableStartTime">
          <el-date-picker clearable
            v-model="form.enableStartTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择可用开始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="可用结束时间" prop="enableEndTime">
          <el-date-picker clearable
            v-model="form.enableEndTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择可用结束时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="领取方式" prop="getMethod">
          <el-input v-model="form.getMethod" placeholder="请输入领取方式" />
        </el-form-item>
        <el-form-item label="优惠券状态" prop="status">
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
import { listApp_goods_coupon, getApp_goods_coupon, delApp_goods_coupon, addApp_goods_coupon, updateApp_goods_coupon } from "@/api/system/app_goods_coupon";

export default {
  name: "App_goods_coupon",
  dicts: ['coupon_type', 'enable_status'],
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
      // 商品优惠券表格数据
      app_goods_couponList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        categoryId: null,
        goodsId: null,
        couponName: null,
        couponContent: null,
        couponType: null,
        minPrice: null,
        discountType: null,
        discountPrice: null,
        countPerOrder: null,
        couponTotal: null,
        countPerUser: null,
        couponGotCount: null,
        couponUsedCount: null,
        enableStartTime: null,
        enableEndTime: null,
        getMethod: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          { required: true, message: "发布人不能为空", trigger: "blur" }
        ],
        categoryId: [
          { required: true, message: "限定分类不能为空", trigger: "blur" }
        ],
        goodsId: [
          { required: true, message: "限定商品不能为空", trigger: "blur" }
        ],
        couponType: [
          { required: true, message: "优惠券类型不能为空", trigger: "change" }
        ],
        countPerOrder: [
          { required: true, message: "每单可用张数不能为空", trigger: "blur" }
        ],
        countPerUser: [
          { required: true, message: "每人可领数量不能为空", trigger: "blur" }
        ],
        couponGotCount: [
          { required: true, message: "已领取数量不能为空", trigger: "blur" }
        ],
        couponUsedCount: [
          { required: true, message: "已使用数量不能为空", trigger: "blur" }
        ],
        getMethod: [
          { required: true, message: "领取方式不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询商品优惠券列表 */
    getList() {
      this.loading = true;
      listApp_goods_coupon(this.queryParams).then(response => {
        this.app_goods_couponList = response.rows;
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
        couponId: null,
        userId: null,
        categoryId: null,
        goodsId: null,
        couponName: null,
        couponContent: null,
        couponType: null,
        minPrice: null,
        discountType: null,
        discountPrice: null,
        countPerOrder: null,
        couponTotal: null,
        countPerUser: null,
        couponGotCount: null,
        couponUsedCount: null,
        enableStartTime: null,
        enableEndTime: null,
        getMethod: null,
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
      this.ids = selection.map(item => item.couponId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加商品优惠券";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const couponId = row.couponId || this.ids
      getApp_goods_coupon(couponId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改商品优惠券";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.couponId != null) {
            updateApp_goods_coupon(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_coupon(this.form).then(response => {
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
      const couponIds = row.couponId || this.ids;
      this.$modal.confirm('是否确认删除商品优惠券编号为"' + couponIds + '"的数据项？').then(function() {
        return delApp_goods_coupon(couponIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_coupon/export', {
        ...this.queryParams
      }, `app_goods_coupon_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
