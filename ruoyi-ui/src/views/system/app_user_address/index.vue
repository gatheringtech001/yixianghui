<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="所属用户" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入所属用户"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="区划编码" prop="regionCode">-->
<!--        <el-input-->
<!--          v-model="queryParams.regionCode"-->
<!--          placeholder="请输入区划编码"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="区划标签" prop="regionLabel">-->
<!--        <el-input-->
<!--          v-model="queryParams.regionLabel"-->
<!--          placeholder="请输入区划标签"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="区划id组" prop="regionIds">-->
<!--        <el-input-->
<!--          v-model="queryParams.regionIds"-->
<!--          placeholder="请输入区划id组"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="省份编码" prop="provinceCode">-->
<!--        <el-input-->
<!--          v-model="queryParams.provinceCode"-->
<!--          placeholder="请输入省份编码"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="收货省份" prop="provinceName">
        <el-input
          v-model="queryParams.provinceName"
          placeholder="请输入收货省份"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="城市编码" prop="cityCode">-->
<!--        <el-input-->
<!--          v-model="queryParams.cityCode"-->
<!--          placeholder="请输入城市编码"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="收货城市" prop="cityName">
        <el-input
          v-model="queryParams.cityName"
          placeholder="请输入收货城市"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="区县编码" prop="countyCode">-->
<!--        <el-input-->
<!--          v-model="queryParams.countyCode"-->
<!--          placeholder="请输入区县编码"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="收货区县" prop="countyName">
        <el-input
          v-model="queryParams.countyName"
          placeholder="请输入收货区县"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="乡镇街道名称" prop="streetName">
        <el-input
          v-model="queryParams.streetName"
          placeholder="请输入乡镇街道名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="乡镇街道编码" prop="streetCode">-->
<!--        <el-input-->
<!--          v-model="queryParams.streetCode"-->
<!--          placeholder="请输入乡镇街道编码"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="收货人姓名" prop="linkPerson">
        <el-input
          v-model="queryParams.linkPerson"
          placeholder="请输入收货人姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收货人手机" prop="linkMobile">
        <el-input
          v-model="queryParams.linkMobile"
          placeholder="请输入收货人手机"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮政编码" prop="postCode">
        <el-input
          v-model="queryParams.postCode"
          placeholder="请输入邮政编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否默认" prop="isDefault">
        <el-select v-model="queryParams.isDefault" placeholder="请选择是否默认" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="地址状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择地址状态" clearable>
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
          v-hasPermi="['system:app_user_address:add']"
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
          v-hasPermi="['system:app_user_address:edit']"
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
          v-hasPermi="['system:app_user_address:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_user_address:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_user_addressList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="地址ID" align="center" prop="addressId" v-if="false"/>
      <el-table-column label="所属用户id" align="center" prop="userId" v-if="false"/>
      <el-table-column label="所属用户" align="center" prop="userName" min-width="80" show-overflow-tooltip/>
<!--      <el-table-column label="区划编码" align="center" prop="regionCode" />-->
<!--      <el-table-column label="区划标签" align="center" prop="regionLabel" />-->
<!--      <el-table-column label="区划id组" align="center" prop="regionIds" />-->
<!--      <el-table-column label="省份编码" align="center" prop="provinceCode" />-->
      <el-table-column label="收货省份" align="center" prop="provinceName" show-overflow-tooltip/>
<!--      <el-table-column label="城市编码" align="center" prop="cityCode" />-->
      <el-table-column label="收货城市" align="center" prop="cityName" show-overflow-tooltip/>
<!--      <el-table-column label="区县编码" align="center" prop="countyCode" />-->
      <el-table-column label="收货区县" align="center" prop="countyName" show-overflow-tooltip/>
      <el-table-column label="乡镇街道名称" align="center" prop="streetName" show-overflow-tooltip/>
<!--      <el-table-column label="乡镇街道编码" align="center" prop="streetCode" />-->
      <el-table-column label="详细地址" align="center" prop="addressDetail" min-width="120" show-overflow-tooltip/>
      <el-table-column label="收货人姓名" align="center" prop="linkPerson" show-overflow-tooltip/>
      <el-table-column label="收货人手机" align="center" prop="linkMobile" min-width="110" show-overflow-tooltip/>
      <el-table-column label="邮政编码" align="center" prop="postCode" show-overflow-tooltip/>
      <el-table-column label="是否默认" align="center" prop="isDefault">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isDefault"/>
        </template>
      </el-table-column>
      <el-table-column label="地址状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.enable_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <div class="action-dropdown">
            <el-dropdown>
              <el-button type="primary" size="small">
                操作<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item @click.native="handleUpdate(scope.row)"
                                  icon="el-icon-info"
                                  v-has-permi="['system:app_user_address:edit']">修改</el-dropdown-item>
                <el-dropdown-item @click.native="handleDelete(scope.row)"
                                  icon="el-icon-edit"
                                  v-hasPermi="['system:app_user_address:remove']">删除</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
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

    <!-- 添加或修改用户地址对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属用户" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入所属用户" />
        </el-form-item>
<!--        <el-form-item label="区划编码" prop="regionCode">-->
<!--          <el-input v-model="form.regionCode" placeholder="请输入区划编码" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="区划标签" prop="regionLabel">-->
<!--          <el-input v-model="form.regionLabel" placeholder="请输入区划标签" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="区划id组" prop="regionIds">-->
<!--          <el-input v-model="form.regionIds" placeholder="请输入区划id组" />-->
<!--        </el-form-item>-->
        <el-form-item label="省份编码" prop="provinceCode">
          <el-input v-model="form.provinceCode" placeholder="请输入省份编码" />
        </el-form-item>
        <el-form-item label="收货省份" prop="provinceName">
          <el-input v-model="form.provinceName" placeholder="请输入收货省份" />
        </el-form-item>
        <el-form-item label="城市编码" prop="cityCode">
          <el-input v-model="form.cityCode" placeholder="请输入城市编码" />
        </el-form-item>
        <el-form-item label="收货城市" prop="cityName">
          <el-input v-model="form.cityName" placeholder="请输入收货城市" />
        </el-form-item>
        <el-form-item label="区县编码" prop="countyCode">
          <el-input v-model="form.countyCode" placeholder="请输入区县编码" />
        </el-form-item>
        <el-form-item label="收货区县" prop="countyName">
          <el-input v-model="form.countyName" placeholder="请输入收货区县" />
        </el-form-item>
        <el-form-item label="乡镇街道名称" prop="streetName">
          <el-input v-model="form.streetName" placeholder="请输入乡镇街道名称" />
        </el-form-item>
        <el-form-item label="乡镇街道编码" prop="streetCode">
          <el-input v-model="form.streetCode" placeholder="请输入乡镇街道编码" />
        </el-form-item>
        <el-form-item label="详细地址" prop="addressDetail">
          <el-input v-model="form.addressDetail" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="收货人姓名" prop="linkPerson">
          <el-input v-model="form.linkPerson" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="收货人手机" prop="linkMobile">
          <el-input v-model="form.linkMobile" placeholder="请输入收货人手机" />
        </el-form-item>
        <el-form-item label="邮政编码" prop="postCode">
          <el-input v-model="form.postCode" placeholder="请输入邮政编码" />
        </el-form-item>
        <el-form-item label="是否默认" prop="isDefault">
          <el-radio-group v-model="form.isDefault">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="地址状态" prop="status">
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
import { listApp_user_address, getApp_user_address, delApp_user_address, addApp_user_address, updateApp_user_address } from "@/api/system/app_user_address";

export default {
  name: "App_user_address",
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
      // 用户地址表格数据
      app_user_addressList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        regionCode: null,
        regionLabel: null,
        regionIds: null,
        provinceCode: null,
        provinceName: null,
        cityCode: null,
        cityName: null,
        countyCode: null,
        countyName: null,
        streetName: null,
        streetCode: null,
        addressDetail: null,
        linkPerson: null,
        linkMobile: null,
        postCode: null,
        isDefault: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        provinceCode: [
          { required: true, message: "省份编码不能为空", trigger: "blur" }
        ],
        cityCode: [
          { required: true, message: "城市编码不能为空", trigger: "blur" }
        ],
        countyCode: [
          { required: true, message: "区县编码不能为空", trigger: "blur" }
        ],
        linkPerson: [
          { required: true, message: "收货人姓名不能为空", trigger: "blur" }
        ],
        linkMobile: [
          { required: true, message: "收货人手机不能为空", trigger: "blur" }
        ],
        isDefault: [
          { required: true, message: "是否默认不能为空", trigger: "change" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询用户地址列表 */
    getList() {
      this.loading = true;
      listApp_user_address(this.queryParams).then(response => {
        this.app_user_addressList = response.rows;
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
        addressId: null,
        userId: null,
        regionCode: null,
        regionLabel: null,
        regionIds: null,
        provinceCode: null,
        provinceName: null,
        cityCode: null,
        cityName: null,
        countyCode: null,
        countyName: null,
        streetName: null,
        streetCode: null,
        addressDetail: null,
        linkPerson: null,
        linkMobile: null,
        postCode: null,
        isDefault: null,
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
      this.ids = selection.map(item => item.addressId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加用户地址";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const addressId = row.addressId || this.ids
      getApp_user_address(addressId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改用户地址";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.addressId != null) {
            updateApp_user_address(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_user_address(this.form).then(response => {
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
      const addressIds = row.addressId || this.ids;
      this.$modal.confirm('是否确认删除用户地址编号为"' + addressIds + '"的数据项？').then(function() {
        return delApp_user_address(addressIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_user_address/export', {
        ...this.queryParams
      }, `app_user_address_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
