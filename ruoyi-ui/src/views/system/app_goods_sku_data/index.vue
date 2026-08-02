<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="商品ID" prop="goodsId">
        <el-input v-model="queryParams.goodsId" placeholder="请输入所属商品" disabled />
      </el-form-item>
      <el-form-item label="组合名称" prop="dataValues">
        <el-input v-model="queryParams.dataValues" placeholder="请输入选项组合名称" clearable
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="数据状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择数据状态" clearable>
          <el-option v-for="dict in dict.type.enable_status" :key="dict.value" :label="dict.label"
            :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
          v-hasPermi="['system:app_goods_sku_data:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
          v-hasPermi="['system:app_goods_sku_data:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['system:app_goods_sku_data:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
          v-hasPermi="['system:app_goods_sku_data:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-list" size="mini" @click="handleOpenAttr"
          v-hasPermi="['system:app_goods_sku_data:add']">管理属性</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_goods_sku_dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="数据id" align="center" prop="dataId" />
      <el-table-column label="商品id" align="center" prop="goodsId" />
      <el-table-column label="属性id集合" align="center" prop="skuIds" />
      <el-table-column label="选项组合id" align="center" prop="optionIds" />
      <el-table-column label="选项组合名称" align="center" prop="dataValues" />
      <el-table-column label="商品价格" align="center" prop="dataPrice" />
      <el-table-column label="选项图片" align="center" prop="dataImage" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.dataImage" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column label="选项库存" align="center" prop="dataStock" />
      <el-table-column label="数据状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.enable_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_goods_sku_data:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_goods_sku_data:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改型号信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="所属商品" prop="goodsId" style="display: none">
          <el-input v-model="form.goodsId" placeholder="请输入所属商品" />
        </el-form-item>
        <el-form-item :label="skuItem.skuName" :prop="skuItem.sku_key" v-for="(skuItem, index) in app_goods_skuList"
          :key="index">
          <el-radio-group v-model="form[skuItem.sku_key]" @input="skuRadioFn">
            <el-radio v-for="optionItem in skuItem.optionList" :key="optionItem.optionId"
              :label="optionItem.optionId + '|@|' + optionItem.optionName">
              {{ optionItem.optionName }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="属性组合名称" prop="dataValues">
          <el-input v-model="form.dataValues" placeholder="请输入属性组合名称" disabled />
        </el-form-item>
        <el-form-item label="选项图片" prop="dataImage">
          <image-upload v-model="form.dataImage" />
        </el-form-item>
        <el-form-item label="商品价格" prop="dataPrice">
          <el-input-number v-model="form.dataPrice" controls-position="right" placeholder="请输入商品价格" :min="0"
            :precision="2"></el-input-number>
        </el-form-item>
        <el-form-item label="选项库存" prop="dataStock">
          <el-input v-model="form.dataStock" placeholder="请输入选项库存" />
        </el-form-item>
        <el-form-item label="数据状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.enable_status" :key="dict.value" :label="dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
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
import { listApp_goods_sku_data, getApp_goods_sku_data, delApp_goods_sku_data, addApp_goods_sku_data, updateApp_goods_sku_data } from "@/api/system/app_goods_sku_data";
import { listApp_goods_sku } from "@/api/system/app_goods_sku";

export default {
  name: "App_goods_sku_data",
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
      // 型号信息表格数据
      app_goods_sku_dataList: [],
      // 规格数据
      app_goods_skuList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        goodsId: null,
        goodsName: null,
        dataValues: null,
        dataStock: null,
        status: null
      },
      // 表单参数
      form: {
        goodsId: null,
        dataValues: '',
        dataImage: '',
        dataPrice: '',
        dataStock: 99,
        status: null,
        remark: ''
      },
      // 表单校验
      rules: {
        dataValues: [
          { required: true, message: '请输入属性组合名称', trigger: 'blur' }
        ],
        dataPrice: [
          { required: true, message: '请输入商品价格', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '请选择数据状态', trigger: 'change' }
        ]
      }
    };
  },
  created() {
    const goodsId = this.$route.query && this.$route.query.goodsId
    if (goodsId) {
      this.queryParams.goodsId = goodsId
    }
    this.getList();
  },
  methods: {
    /** 查询型号信息列表 */
    getList() {
      this.loading = true;
      listApp_goods_sku_data(this.queryParams).then(response => {
        this.app_goods_sku_dataList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    getSkuList(type) {
      let _this = this
      _this.loading = true;
      listApp_goods_sku({ goodsId: _this.queryParams.goodsId }).then(response => {
        _this.app_goods_skuList = response.rows;
        _this.total = response.total;
        _this.loading = false;
        _this.reset(type);
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
    },
    // 表单重置
    reset(type) {
      let formItems = {}
      if (type == 'create') {
        // 处理SKU
        this.app_goods_skuList.forEach(item => {
          item['sku_key'] = 'sku_' + item.skuId
          formItems['sku_' + item.skuId] = ''
          this.rules['sku_' + item.skuId] = [
            { required: true, message: '请选择' + item.skuName, trigger: 'change' }
          ]
        });
        this.form = {
          goodsId: this.queryParams.goodsId,
          dataValues: '',
          dataImage: '',
          dataPrice: '',
          dataStock: 99,
          status: null,
          remark: '',
          ...formItems
        };
        this.resetForm("form");
      } else {
        // 处理SKU
        this.app_goods_skuList.forEach(item => {
          item['sku_key'] = 'sku_' + item.skuId
          item.optionList.forEach(items => {
            if (this.form.optionIds && (',' + this.form.optionIds + ',').indexOf(',' + items.optionId + ',') != -1) {
              formItems['sku_' + item.skuId] = items.optionId + '|@|' + items.optionName
            }
          })
          this.rules['sku_' + item.skuId] = [
            { required: true, message: '请选择' + item.skuName, trigger: 'change' }
          ]
        });
        this.form = { ...this.form, ...formItems }
      }
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
      this.ids = selection.map(item => item.dataId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.title = "添加型号信息";
      this.open = true;
      this.getSkuList('create');
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      const dataId = row.dataId || this.ids
      getApp_goods_sku_data(dataId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改型号信息";
        this.getSkuList('update');
      });
    },

    /** 属性配置 */
    handleOpenAttr(row) {
      // this.reset();
      // const goodsId = row.goodsId
      // this.$router.push({path: "/goods/app_goods_sku_data", query: {goodsId: goodsId}});
      // this.reset();
      const goodsId = this.queryParams.goodsId
      this.$router.push({ path: "/goods/app_goods_sku", query: { goodsId: goodsId } });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          let copyForm = { ...this.form }
          copyForm['skuIds'] = ''
          let optionIds = []
          this.app_goods_skuList.forEach(item => {
            if (copyForm['skuIds'] != '') {
              copyForm['skuIds'] += ','
            }
            copyForm['skuIds'] += item.skuId
            optionIds.push(copyForm[item.sku_key].split('|@|')[0])
            delete copyForm[item.sku_key]
          });
          copyForm['optionIds'] = optionIds.join(',')
          if (copyForm.dataId != null) {
            updateApp_goods_sku_data(copyForm).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_sku_data(copyForm).then(response => {
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
      const dataIds = row.dataId || this.ids;
      this.$modal.confirm('是否确认删除型号信息编号为"' + dataIds + '"的数据项？').then(function () {
        return delApp_goods_sku_data(dataIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => { });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_sku_data/export', {
        ...this.queryParams
      }, `app_goods_sku_data_${new Date().getTime()}.xlsx`)
    },
    /**
     * SKU单选框选中值时执行的方法
     */
    skuRadioFn() {
      let _this = this
      // 处理SKU
      let dataValues = ''
      this.app_goods_skuList.forEach(item => {
        if (_this.form[item.sku_key] != '') {
          if (dataValues) {
            dataValues += '-' + _this.form[item.sku_key].split('|@|')[1]
          } else {
            dataValues += _this.form[item.sku_key].split('|@|')[1]
          }
        }
      });
      this.form.dataValues = dataValues
    }
  }
};
</script>
