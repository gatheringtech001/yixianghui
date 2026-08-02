<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="省份" prop="province">
        <el-input
          v-model="queryParams.province"
          placeholder="请输入省份"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="州市" prop="city">
        <el-input
          v-model="queryParams.city"
          placeholder="请输入州市"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="区县" prop="county">
        <el-input
          v-model="queryParams.county"
          placeholder="请输入区县"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮政编码" prop="areaCode">
        <el-input
          v-model="queryParams.areaCode"
          placeholder="请输入邮政编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="拼音" prop="pinyin">
        <el-input
          v-model="queryParams.pinyin"
          placeholder="请输入拼音"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="等级" prop="areaLevel">
        <el-select v-model="queryParams.areaLevel" placeholder="请选择等级" clearable>
          <el-option
            v-for="dict in dict.type.area_level"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="经度" prop="lng">
        <el-input
          v-model="queryParams.lng"
          placeholder="请输入经度"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="纬度" prop="lat">
        <el-input
          v-model="queryParams.lat"
          placeholder="请输入纬度"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否地图核心" prop="mapMaster">
        <el-select v-model="queryParams.mapMaster" placeholder="请选择是否地图核心" clearable>
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
          v-hasPermi="['system:app_area:add']"
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
          v-hasPermi="['system:app_area:edit']"
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
          v-hasPermi="['system:app_area:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_area:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_areaList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="区域ID" align="center" prop="areaId" />
      <el-table-column label="省份" align="center" prop="province" />
      <el-table-column label="州市" align="center" prop="city" />
      <el-table-column label="区县" align="center" prop="county" />
      <el-table-column label="邮政编码" align="center" prop="areaCode" />
      <el-table-column label="拼音" align="center" prop="pinyin" />
      <el-table-column label="等级" align="center" prop="areaLevel">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.area_level" :value="scope.row.areaLevel"/>
        </template>
      </el-table-column>
      <el-table-column label="经度" align="center" prop="lng" />
      <el-table-column label="纬度" align="center" prop="lat" />
      <el-table-column label="地图数据" align="center" prop="mapData" />
      <el-table-column label="是否地图核心" align="center" prop="mapMaster">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.mapMaster"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_area:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_area:remove']"
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

    <!-- 添加或修改行政区域对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" placeholder="请输入省份" />
        </el-form-item>
        <el-form-item label="州市" prop="city">
          <el-input v-model="form.city" placeholder="请输入州市" />
        </el-form-item>
        <el-form-item label="区县" prop="county">
          <el-input v-model="form.county" placeholder="请输入区县" />
        </el-form-item>
        <el-form-item label="邮政编码" prop="areaCode">
          <el-input v-model="form.areaCode" placeholder="请输入邮政编码" />
        </el-form-item>
        <el-form-item label="拼音" prop="pinyin">
          <el-input v-model="form.pinyin" placeholder="请输入拼音" />
        </el-form-item>
        <el-form-item label="等级" prop="areaLevel">
          <el-radio-group v-model="form.areaLevel">
            <el-radio
              v-for="dict in dict.type.area_level"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="经度" prop="lng">
          <el-input v-model="form.lng" placeholder="请输入经度" />
        </el-form-item>
        <el-form-item label="纬度" prop="lat">
          <el-input v-model="form.lat" placeholder="请输入纬度" />
        </el-form-item>
        <el-form-item label="地图数据" prop="mapData">
          <el-input v-model="form.mapData" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="是否地图核心" prop="mapMaster">
          <el-radio-group v-model="form.mapMaster">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
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
import { listApp_area, getApp_area, delApp_area, addApp_area, updateApp_area } from "@/api/system/app_area";

export default {
  name: "App_area",
  dicts: ['area_level', 'common_is_not'],
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
      // 行政区域表格数据
      app_areaList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        province: null,
        city: null,
        county: null,
        areaCode: null,
        pinyin: null,
        areaLevel: null,
        lng: null,
        lat: null,
        mapData: null,
        mapMaster: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        mapMaster: [
          { required: true, message: "是否地图核心不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询行政区域列表 */
    getList() {
      this.loading = true;
      listApp_area(this.queryParams).then(response => {
        this.app_areaList = response.rows;
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
        areaId: null,
        province: null,
        city: null,
        county: null,
        areaCode: null,
        pinyin: null,
        areaLevel: null,
        lng: null,
        lat: null,
        mapData: null,
        mapMaster: null
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
      this.ids = selection.map(item => item.areaId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加行政区域";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const areaId = row.areaId || this.ids
      getApp_area(areaId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改行政区域";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.areaId != null) {
            updateApp_area(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_area(this.form).then(response => {
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
      const areaIds = row.areaId || this.ids;
      this.$modal.confirm('是否确认删除行政区域编号为"' + areaIds + '"的数据项？').then(function() {
        return delApp_area(areaIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_area/export', {
        ...this.queryParams
      }, `app_area_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
