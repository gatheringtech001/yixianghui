<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
<!--      <el-form-item label="所属分类" prop="categoryId">-->
<!--        <el-input-->
<!--          v-model="queryParams.categoryId"-->
<!--          placeholder="请输入所属分类"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="祖级分类" prop="categoryIds">-->
<!--        <el-input-->
<!--          v-model="queryParams.categoryIds"-->
<!--          placeholder="请输入祖级分类"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="活动标题" prop="activityName">
        <el-input
          v-model="queryParams.activityName"
          placeholder="请输入活动标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="活动地点" prop="address">
        <el-input
          v-model="queryParams.address"
          placeholder="请输入活动地点"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="活动标签" prop="tags">
        <el-input
          v-model="queryParams.tags"
          placeholder="请输入活动标签"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="是否置顶" prop="isTop">-->
<!--        <el-select v-model="queryParams.isTop" placeholder="请选择是否置顶" clearable>-->
<!--          <el-option-->
<!--            v-for="dict in dict.type.common_is_not"-->
<!--            :key="dict.value"-->
<!--            :label="dict.label"-->
<!--            :value="dict.value"-->
<!--          />-->
<!--        </el-select>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="是否热门" prop="isHot">-->
<!--        <el-select v-model="queryParams.isHot" placeholder="请选择是否热门" clearable>-->
<!--          <el-option-->
<!--            v-for="dict in dict.type.common_is_not"-->
<!--            :key="dict.value"-->
<!--            :label="dict.label"-->
<!--            :value="dict.value"-->
<!--          />-->
<!--        </el-select>-->
<!--      </el-form-item>-->
      <el-form-item label="活动状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择活动状态" clearable>
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
          v-hasPermi="['system:app_activity:add']"
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
          v-hasPermi="['system:app_activity:edit']"
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
          v-hasPermi="['system:app_activity:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_activity:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_activityList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="活动ID" align="center" prop="activityId" />
      <el-table-column label="所属分类" align="center" prop="categoryId" />
      <el-table-column label="归属站点" align="center" prop="deptId" />
<!--      <el-table-column label="祖级分类" align="center" prop="categoryIds" />-->
      <el-table-column label="活动标题" align="center" prop="activityName" />
      <el-table-column label="活动地点" align="center" prop="address" />
      <el-table-column label="封面图片" align="center" prop="activityCover" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.activityCover" :width="50" :height="50"/>
        </template>
      </el-table-column>
<!--      <el-table-column label="活动简介" align="center" prop="description" />-->
      <el-table-column label="活动标签" align="center" prop="tags" />
<!--      <el-table-column label="是否置顶" align="center" prop="isTop">-->
<!--        <template slot-scope="scope">-->
<!--          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isTop"/>-->
<!--        </template>-->
<!--      </el-table-column>-->
<!--      <el-table-column label="是否热门" align="center" prop="isHot">-->
<!--        <template slot-scope="scope">-->
<!--          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isHot"/>-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column label="已报名人数" align="center" prop="signCount" />
      <el-table-column label="收费类型" align="center" prop="isFree" width="90">
        <template slot-scope="scope">
          <span>{{ scope.row.isFree === 0 ? '付费' : '免费' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="报名费用" align="center" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.isFree === 0">￥{{ scope.row.vipPrice || scope.row.price || 0 }}</span>
          <span v-else>免费</span>
        </template>
      </el-table-column>
      <el-table-column label="报名人数限制" align="center" prop="maxCount" />
      <el-table-column label="报名截止时间" align="center" prop="signEndTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.signEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="活动时间" align="center" prop="activityTime" />
      <el-table-column label="结束时间" align="center" prop="activityEndTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.activityEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
<!--      <el-table-column label="活动内容" align="center" prop="content" />-->
<!--      <el-table-column label="阅读次数" align="center" prop="viewCount" />-->
      <el-table-column label="活动状态" align="center" prop="status">
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
            v-hasPermi="['system:app_activity:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_activity:remove']"
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

    <!-- 添加或修改活动对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="归属站点" prop="deptId">
          <treeselect v-model="form.deptId" :options="deptOptions" :normalizer="normalizerDept" placeholder="选择归属站点（城市）" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <treeselect v-model="form.categoryId" :options="app_activity_categoryOptions" :normalizer="normalizer" placeholder="请选择所属上级" />
        </el-form-item>
<!--        <el-form-item label="祖级分类" prop="categoryIds">-->
<!--          <el-input v-model="form.categoryIds" placeholder="请输入祖级分类" />-->
<!--        </el-form-item>-->
        <el-form-item label="活动标题" prop="activityName">
          <el-input v-model="form.activityName" placeholder="请输入活动标题" />
        </el-form-item>
        <el-form-item label="活动地点" prop="address">
          <el-input v-model="form.address" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="封面图片" prop="activityCover">
          <image-upload v-model="form.activityCover"/>
        </el-form-item>
        <el-form-item label="活动简介" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="活动标签" prop="tags">
          <el-input v-model="form.tags" placeholder="请输入活动标签" />
        </el-form-item>
<!--        <el-form-item label="是否置顶" prop="isTop">-->
<!--          <el-radio-group v-model="form.isTop">-->
<!--            <el-radio-->
<!--              v-for="dict in dict.type.common_is_not"-->
<!--              :key="dict.value"-->
<!--              :label="parseInt(dict.value)"-->
<!--            >{{dict.label}}</el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-form-item>-->
<!--        <el-form-item label="是否热门" prop="isHot">-->
<!--          <el-radio-group v-model="form.isHot">-->
<!--            <el-radio-->
<!--              v-for="dict in dict.type.common_is_not"-->
<!--              :key="dict.value"-->
<!--              :label="parseInt(dict.value)"-->
<!--            >{{dict.label}}</el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-form-item>-->

        <el-form-item label="报名人数限制" prop="maxCount">
          <el-input v-model="form.maxCount" placeholder="请输入报名人数限制" />
        </el-form-item>
        <el-form-item label="已报名人数" prop="signCount">
          <el-input v-model="form.signCount" disabled placeholder="请输入已报名人数" />
        </el-form-item>
        <el-form-item label="报名截止时间" prop="signEndTime">
          <el-date-picker clearable
                          v-model="form.signEndTime"
                          type="datetime"
                          value-format="yyyy-MM-dd HH:mm:ss"
                          placeholder="请选择报名截止时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="活动时间" prop="activityTime">
          <el-input v-model="form.activityTime" placeholder="请输入活动时间（小程序展示用）" />
        </el-form-item>
        <el-form-item label="结束时间" prop="activityEndTime">
          <el-date-picker clearable
                          v-model="form.activityEndTime"
                          type="datetime"
                          value-format="yyyy-MM-dd HH:mm:ss"
                          placeholder="请选择活动结束时间">
          </el-date-picker>
          <span class="form-tip">仅用于判断活动是否结束，不在小程序展示</span>
        </el-form-item>
        <el-form-item label="是否免费" prop="isFree">
          <el-radio-group v-model="form.isFree">
            <el-radio :label="1">免费活动</el-radio>
            <el-radio :label="0">付费活动</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.isFree === 0">
          <el-form-item label="原价" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="实付价" prop="vipPrice">
            <el-input-number v-model="form.vipPrice" :min="0" :precision="2" :step="1" controls-position="right" />
            <span class="form-tip">用户实际支付金额，按人次计费</span>
          </el-form-item>
        </template>

        <el-form-item label="活动内容" prop="content">
          <editor v-model="form.content" :min-height="192"/>
        </el-form-item>
        <el-form-item label="阅读次数" prop="viewCount">
          <el-input v-model="form.viewCount" placeholder="请输入阅读次数" />
        </el-form-item>
        <el-form-item label="活动状态" prop="status">
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
import { listApp_activity, getApp_activity, delApp_activity, addApp_activity, updateApp_activity } from "@/api/system/app_activity";
import Treeselect from "@riophae/vue-treeselect";
import { listApp_activity_category } from "@/api/system/app_activity_category";
import { listDept } from "@/api/system/dept";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";

export default {
  name: "App_activity",
  dicts: ['common_is_not', 'enable_status'],
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
      // 活动表格数据
      app_activityList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        categoryId: null,
        categoryIds: null,
        activityName: null,
        address: null,
        activityCover: null,
        description: null,
        tags: null,
        isTop: null,
        isHot: null,
        content: null,
        viewCount: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        deptId: [
          { required: true, message: "归属站点不能为空", trigger: "change" }
        ],
        activityName: [
          { required: true, message: "活动标题不能为空", trigger: "blur" }
        ],
      },

      // 站点树选项
      deptOptions: [],
      // 活动分类树选项
      app_activity_categoryOptions: [],
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询活动列表 */
    getList() {
      this.loading = true;
      listApp_activity(this.queryParams).then(response => {
        this.app_activityList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 转换站点数据结构 */
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
    /** 查询站点下拉树结构 */
    getDeptTreeselect() {
      listDept().then(response => {
        this.deptOptions = [];
        const data = { deptId: 0, deptName: '无站点', children: [] };
        data.children = this.handleTree(response.data, "deptId", "parentId");
        this.deptOptions.push(data);
      });
    },
    /** 转换活动分类数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.categoryId,
        label: node.categoryName,
        children: node.children
      };
    },
    /** 查询活动分类下拉树结构 */
    getTreeselect() {
      listApp_activity_category().then(response => {
        this.app_activity_categoryOptions = [];
        const data = { categoryId: 0, categoryName: '顶级节点', children: [] };
        data.children = this.handleTree(response.data, "categoryId", "parentId");
        this.app_activity_categoryOptions.push(data);
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
        activityId: null,
        categoryId: null,
        deptId: null,
        categoryIds: null,
        activityName: null,
        address: null,
        activityCover: null,
        description: null,
        tags: null,
        isTop: null,
        isHot: null,
        maxCount: null,
        signCount: null,
        signEndTime: null,
        activityTime: null,
        activityEndTime: null,
        isFree: 1,
        price: 0,
        vipPrice: 0,
        content: null,
        viewCount: null,
        createTime: null,
        updateTime: null,
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
      this.ids = selection.map(item => item.activityId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.getDeptTreeselect();
      this.getTreeselect();
      this.open = true;
      this.title = "添加活动";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.getDeptTreeselect();
      this.getTreeselect();
      const activityId = row.activityId || this.ids
      getApp_activity(activityId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改活动";
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
          if (this.form.isFree === 0) {
            const payPrice = Number(this.form.vipPrice || 0)
            if (!payPrice || payPrice <= 0) {
              this.$modal.msgError("付费活动请填写大于0的实付价")
              return
            }
          } else {
            this.form.price = 0
            this.form.vipPrice = 0
          }
          if (this.form.activityId != null) {
            updateApp_activity(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_activity(this.form).then(response => {
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
      const activityIds = row.activityId || this.ids;
      this.$modal.confirm('是否确认删除活动编号为"' + activityIds + '"的数据项？').then(function() {
        return delApp_activity(activityIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_activity/export', {
        ...this.queryParams
      }, `app_activity_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
