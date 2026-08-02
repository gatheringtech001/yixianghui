<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="服务站点" prop="deptId">
        <el-input
          v-model="queryParams.deptId"
          placeholder="请输入服务站点"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户姓名" prop="customerId">
        <el-input
          v-model="queryParams.customerId"
          placeholder="请选择客户姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="成交日期" prop="tradeDate">
        <el-select v-model="queryParams.tradeDate" placeholder="请选择是否结算" clearable>
          <el-option
            v-for="dict in dict.type.common_date_str"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="养老顾问" prop="consultantId">
        <el-input
          v-model="queryParams.consultantId"
          placeholder="请输入养老顾问"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
          v-hasPermi="['system:app_customer_income:add']"
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
          v-hasPermi="['system:app_customer_income:edit']"
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
          v-hasPermi="['system:app_customer_income:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_customer_income:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_customer_incomeList" @selection-change="handleSelectionChange" style="width: 100%; min-width: 600px;">
      <el-table-column type="selection" width="55" align="center"  fixed="left"/>
      <el-table-column label="收入ID" align="center" prop="incomeId" v-if="false"/>
      <el-table-column label="客户姓名" align="center" prop="customerName"  fixed="left" min-width="100" show-overflow-tooltip/>
      <el-table-column label="销售内容" align="center" prop="productName"  fixed="left" min-width="100" show-overflow-tooltip/>
      <el-table-column label="编号" align="center" prop="incomeNo"  fixed="left" min-width="120" show-overflow-tooltip/>
      <el-table-column label="服务站点" align="center" prop="deptId" v-if="false"/>
      <el-table-column label="服务站点" align="center" prop="departName" min-width="100" show-overflow-tooltip/>
      <el-table-column label="充值金额" align="center" prop="chargeAmount" />
      <el-table-column label="消费金额" align="center" prop="purchaseAmount" />
      <el-table-column label="余额" align="center" prop="balance" />
      <el-table-column label="金币" align="center" prop="score" />
      <el-table-column label="成交日期" align="center" prop="tradeDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.tradeDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否结算" align="center" prop="settlement">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.settlement"/>
        </template>
      </el-table-column>
      <el-table-column label="公司收入" align="center" prop="companyIncome" />
      <el-table-column label="顾问提成" align="center" prop="consultantIncome" />
      <el-table-column label="产品类别" align="center" prop="productType" />
      <el-table-column label="供应商" align="center" prop="supplierId" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建人id" align="center" prop="userId" v-if="false"/>
      <el-table-column label="创建人" align="center" prop="userName" show-overflow-tooltip/>
      <el-table-column label="养老顾问" align="center" prop="consultantId" v-if="false"/>
      <el-table-column label="养老顾问" align="center" prop="supplierName" min-width="80" show-overflow-tooltip/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width"
                       width="100" fixed="right">
        <template slot-scope="scope">
          <div class="action-dropdown">
            <el-dropdown>
              <el-button type="primary" size="small">
                操作<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item @click.native="handleUpdate(scope.row)"
                                  icon="el-icon-info"
                                  v-has-permi="['system:app_customer_income:edit']">修改</el-dropdown-item>
                <el-dropdown-item @click.native="handleDelete(scope.row)"
                                  icon="el-icon-edit"
                                  v-hasPermi="['system:app_customer_income:remove']">删除</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <p>
      充值金额：{{stat.chargeAmount}}元  消费记录：{{stat.purchaseAmount}}元  顾问提成：{{stat.consultantIncomeAmount}}元  公司收入：{{stat.companyIncomeAmount}}元
    </p>
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改收入明细对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
<!--        <el-form-item label="服务站点" prop="deptId">-->
<!--          <el-input v-model="form.deptId" placeholder="请输入服务站点" />-->
<!--        </el-form-item>-->

        <el-form-item label="服务站点" prop="deptId">
          <treeselect v-model="form.deptId" :options="deptOptions" :normalizer="normalizerDept" placeholder="选择服务站点" />
        </el-form-item>
        <el-form-item label="养老顾问" prop="consultantId">
          <el-select v-model="form.consultantId" placeholder="请选择养老顾问">
            <el-option
              v-for="data in consultantList"
              :key="data.consultantId"
              :label="data.consultantName"
              :value="data.consultantId"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="销售内容" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入销售内容" />
        </el-form-item>
        <el-form-item v-if="form.incomeId" label="编号" prop="incomeNo">
          <el-input v-model="form.incomeNo" placeholder="请输入编号" />
        </el-form-item>
        <el-form-item label="充值金额" prop="chargeAmount">
          <el-input v-model="form.chargeAmount" placeholder="请输入充值金额" />
        </el-form-item>
        <el-form-item label="消费金额" prop="purchaseAmount">
          <el-input v-model="form.purchaseAmount" placeholder="请输入消费金额" />
        </el-form-item>
        <el-form-item label="余额" prop="balance">
          <el-input v-model="form.balance" placeholder="请输入余额" />
        </el-form-item>
        <el-form-item label="金币" prop="score">
          <el-input v-model="form.score" placeholder="请输入金币" />
        </el-form-item>
        <el-form-item label="产品类别" prop="productType">
          <el-select v-model="queryParams.productType" placeholder="请选择产品类别" clearable>
            <el-option
              v-for="dict in dict.type.product_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="成交日期" prop="tradeDate">
          <el-date-picker clearable
            v-model="form.tradeDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择成交日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="是否结算" prop="settlement">
          <el-radio-group v-model="form.settlement">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公司收入" prop="companyIncome">
          <el-input v-model="form.companyIncome" placeholder="请输入公司收入" />
        </el-form-item>
        <el-form-item label="顾问提成" prop="consultantIncome">
          <el-input v-model="form.consultantIncome" placeholder="请输入顾问提成" />
        </el-form-item>
        <el-form-item label="客户姓名" prop="customerId">
          <el-input v-model="form.customerId" placeholder="请选择" />
        </el-form-item>
<!--        <el-form-item label="供应商" prop="supplierId">-->
<!--          <el-input v-model="form.supplierId" placeholder="请输入供应商" />-->
<!--        </el-form-item>-->
        <el-form-item label="供应商" prop="supplierId">
          <el-select v-model="form.supplierId" placeholder="请选择供应商">
            <el-option
              v-for="data in supplierList"
              :key="data.supplierId"
              :label="data.supplierName"
              :value="data.supplierId"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
import { listApp_customer_income, statApp_customer_income, getApp_customer_income, delApp_customer_income, addApp_customer_income, updateApp_customer_income } from "@/api/system/app_customer_income";
import { listApp_supplier } from "@/api/system/app_supplier";
import { listApp_consultant } from "@/api/system/app_consultant";
import {listDept} from "@/api/system/dept";
import Treeselect from "@riophae/vue-treeselect";


export default {
  name: "App_customer_income",
  dicts: ['common_is_not', 'product_type'],
  components: {
    Treeselect
  },
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
      // 收入明细表格数据
      app_customer_incomeList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        productName: null,
        incomeNo: null,
        deptId: null,
        chargeAmount: null,
        purchaseAmount: null,
        balance: null,
        score: null,
        tradeDate: null,
        settlement: null,
        companyIncome: null,
        consultantIncome: null,
        productType: null,
        supplierId: null,
        consultantId: null,
        customerId: null,
      },
      stat: {
        chargeAmount: null,
        purchaseAmount: null,
        consultantIncomeAmount: null,
        companyIncomeAmount: null,
      },
      // 供应商列表
      supplierList: [],
      // 顾问列表
      consultantList: [],
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      },

      // 分站树选项
      deptOptions: [],
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询收入明细列表 */
    getList() {
      this.loading = true;
      listApp_customer_income(this.queryParams).then(response => {
        this.app_customer_incomeList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.getStat();
      });
    },

    /** 转换分站数据结构 */
    normalizerDept(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.deptId,
        label: node.deptName,
        children: node.children
      };
    },
    /** 查询部门下拉树结构 */
    getDeptTreeselect() {
      listDept().then(response => {
        this.deptOptions = [];
        const data = { deptId: 0, deptName: '无站点', children: [] };
        data.children = this.handleTree(response.data, "deptId", "parentId");
        this.deptOptions.push(data);
      });
    },

    /** 查询部门列表 */
    getDeptList() {
      // this.loading = true;
      listDept(this.queryParams).then(response => {
        this.deptList = this.handleTree(response.data, "deptId");
        // this.loading = false;
      });
    },
    /** 统计收入明细 */
    getStat() {
      statApp_customer_income(this.queryParams).then(response => {
        this.stat = response.data;
      });
    },
    /** 供应商列表 */
    getSupplierList() {
      listApp_supplier(this.queryParams).then(response => {
        this.supplierList = response.rows; //
        // console.log(response.rows)
        // console.log(this.supplierList)
      });
    },
    /** 顾问列表 */
    getConsultantList() {
      listApp_consultant(this.queryParams).then(response => {
        this.consultantList = response.rows; //
        // console.log(response.rows)
        // console.log(this.consultantList)
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
        incomeId: null,
        userId: null,
        productName: null,
        incomeNo: null,
        deptId: null,
        chargeAmount: null,
        purchaseAmount: null,
        balance: null,
        score: null,
        tradeDate: null,
        settlement: null,
        companyIncome: null,
        consultantIncome: null,
        productType: null,
        supplierId: null,
        remark: null,
        consultantId: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
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
      this.ids = selection.map(item => item.incomeId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加收入明细";
      this.getSupplierList();
      this.getDeptTreeselect();
      this.getConsultantList();
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const incomeId = row.incomeId || this.ids
      getApp_customer_income(incomeId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改收入明细";
        this.getSupplierList();
        this.getDeptTreeselect();
        this.getConsultantList();
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.incomeId != null) {
            updateApp_customer_income(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_customer_income(this.form).then(response => {
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
      const incomeIds = row.incomeId || this.ids;
      this.$modal.confirm('是否确认删除收入明细编号为"' + incomeIds + '"的数据项？').then(function() {
        return delApp_customer_income(incomeIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_customer_income/export', {
        ...this.queryParams
      }, `app_customer_income_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
