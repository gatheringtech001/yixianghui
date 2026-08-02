<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="位置名称" prop="positionName">
        <el-input v-model="queryParams.positionName" placeholder="请输入位置名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="位置编号" prop="positionCode">
        <el-input v-model="queryParams.positionCode" placeholder="请输入位置编号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="广告位ID" prop="positionId">
        <el-input v-model="queryParams.positionId" placeholder="请输入广告位ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:app_ad_position:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:app_ad_position:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:app_ad_position:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:app_ad_position:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      ref="positionTable"
      v-loading="loading"
      :data="app_ad_positionList"
      @selection-change="handleSelectionChange"
      @expand-change="handleExpandChange"
      row-key="positionId"
    >
      <el-table-column type="expand">
        <template slot-scope="props">
          <div class="expand-panel">
            <div class="expand-header">
              <div class="expand-title">广告内容（广告位 ID: {{ props.row.positionId }} · 共 {{ (expandContentMap[props.row.positionId] || []).length }} 条）</div>
              <div class="expand-actions">
                <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAddExpandContent(props.row)" v-hasPermi="['system:app_ad_content:add']">添加内容</el-button>
                <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="!checkedExpandContent[props.row.positionId] || !checkedExpandContent[props.row.positionId].length" @click="handleDeleteExpandContentBatch(props.row)" v-hasPermi="['system:app_ad_content:remove']">删除选中</el-button>
              </div>
            </div>
            <el-table
              v-loading="expandLoadingMap[props.row.positionId]"
              :data="expandContentMap[props.row.positionId] || []"
              border
              size="small"
              @selection-change="(sel) => handleExpandContentSelectionChange(props.row.positionId, sel)"
            >
              <el-table-column type="selection" width="45" align="center" />
              <el-table-column label="内容ID" prop="contentId" width="90" align="center" />
              <el-table-column label="广告标题" prop="adName" min-width="120" show-overflow-tooltip />
              <el-table-column label="广告介绍" prop="description" min-width="140" show-overflow-tooltip />
              <el-table-column label="广告图片" prop="adImage" width="100" align="center">
                <template slot-scope="scope">
                  <image-preview v-if="scope.row.adImage" :src="scope.row.adImage" :width="50" :height="50"/>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="富文本" prop="adContent" width="80" align="center">
                <template slot-scope="scope">
                  <el-tag v-if="scope.row.adContent" size="mini" type="success">有</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="开始时间" prop="startTime" width="110" align="center" />
              <el-table-column label="结束时间" prop="endTime" width="110" align="center" />
              <el-table-column label="广告链接" prop="linkUrl" min-width="120" show-overflow-tooltip />
              <el-table-column label="排序" prop="orderNum" width="70" align="center" />
              <el-table-column label="状态" prop="status" width="90" align="center">
                <template slot-scope="scope">
                  <dict-tag :options="dict.type.enable_status" :value="scope.row.status"/>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" align="center" fixed="right">
                <template slot-scope="scope">
                  <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEditExpandContent(scope.row, props.row.positionId)" v-hasPermi="['system:app_ad_content:edit']">编辑</el-button>
                  <el-button size="mini" type="text" icon="el-icon-delete" class="danger-text" @click="handleDeleteExpandContent(scope.row, props.row.positionId)" v-hasPermi="['system:app_ad_content:remove']">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="广告位ID" align="center" prop="positionId" width="100" />
      <el-table-column label="位置名称" align="center" prop="positionName" min-width="140" />
      <el-table-column label="位置编号" align="center" prop="positionCode" min-width="160" show-overflow-tooltip />
      <el-table-column label="内容数" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="contentCountMap[scope.row.positionId] != null" size="mini" type="info">{{ contentCountMap[scope.row.positionId] }}</el-tag>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="位置状态" align="center" prop="status" width="100" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-folder-opened" @click="handleManageContent(scope.row)">管理内容</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:app_ad_position:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:app_ad_position:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="广告位ID">
              <el-input v-model="form.positionId" disabled placeholder="保存后自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="位置名称" prop="positionName">
              <el-input v-model="form.positionName" placeholder="请输入位置名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="位置编号" prop="positionCode">
              <el-input v-model="form.positionCode" placeholder="如 mnp_index_banner" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">广告内容（从表）</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleAddAppAdContent">添加内容</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="el-icon-delete" size="mini" @click="handleDeleteAppAdContent">删除选中</el-button>
          </el-col>
        </el-row>
        <el-table :data="appAdContentList" :row-class-name="rowAppAdContentIndex" @selection-change="handleAppAdContentSelectionChange" ref="appAdContent" border max-height="420">
          <el-table-column type="selection" width="45" align="center" />
          <el-table-column label="内容ID" prop="contentId" width="80" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.contentId || '新' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="广告标题" prop="adName" min-width="120">
            <template slot-scope="scope">
              <el-input v-model="scope.row.adName" placeholder="广告标题" size="mini" />
            </template>
          </el-table-column>
          <el-table-column label="广告介绍" prop="description" min-width="120">
            <template slot-scope="scope">
              <el-input v-model="scope.row.description" placeholder="广告介绍" size="mini" />
            </template>
          </el-table-column>
          <el-table-column label="广告图片" prop="adImage" width="120" align="center">
            <template slot-scope="scope">
              <image-upload v-model="scope.row.adImage" :limit="1" />
            </template>
          </el-table-column>
          <el-table-column label="开始时间" prop="startTime" width="150">
            <template slot-scope="scope">
              <el-date-picker clearable v-model="scope.row.startTime" type="date" value-format="yyyy-MM-dd" placeholder="开始时间" size="mini" style="width:130px" />
            </template>
          </el-table-column>
          <el-table-column label="结束时间" prop="endTime" width="150">
            <template slot-scope="scope">
              <el-date-picker clearable v-model="scope.row.endTime" type="date" value-format="yyyy-MM-dd" placeholder="结束时间" size="mini" style="width:130px" />
            </template>
          </el-table-column>
          <el-table-column label="广告链接" prop="linkUrl" min-width="120">
            <template slot-scope="scope">
              <el-input v-model="scope.row.linkUrl" placeholder="链接/分类ID等" size="mini" />
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="orderNum" width="80">
            <template slot-scope="scope">
              <el-input v-model="scope.row.orderNum" placeholder="排序" size="mini" />
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status" width="110">
            <template slot-scope="scope">
              <el-select v-model="scope.row.status" placeholder="状态" size="mini">
                <el-option v-for="dict in dict.type.enable_status" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="text" @click="handleEditDialogContent(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 广告内容编辑对话框（含富文本） -->
    <el-dialog :title="contentTitle" :visible.sync="contentOpen" width="720px" append-to-body :close-on-click-modal="false">
      <el-form ref="contentForm" :model="contentForm" :rules="contentRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属广告位" prop="positionId">
              <el-input v-model="contentForm.positionId" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内容ID">
              <el-input :value="contentForm.contentId || '保存后自动生成'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="广告标题" prop="adName">
          <el-input v-model="contentForm.adName" placeholder="请输入广告标题" />
        </el-form-item>
        <el-form-item label="广告介绍" prop="description">
          <el-input v-model="contentForm.description" type="textarea" :rows="2" placeholder="请输入广告介绍" />
        </el-form-item>
        <el-form-item label="广告图片" prop="adImage">
          <image-upload v-model="contentForm.adImage" :limit="1" />
        </el-form-item>
        <el-form-item label="富文本" prop="adContent">
          <editor v-model="contentForm.adContent" :min-height="192" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker clearable v-model="contentForm.startTime" type="date" value-format="yyyy-MM-dd" placeholder="开始时间" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker clearable v-model="contentForm.endTime" type="date" value-format="yyyy-MM-dd" placeholder="结束时间" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="广告链接" prop="linkUrl">
              <el-input v-model="contentForm.linkUrl" placeholder="链接/分类ID等" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="排序" prop="orderNum">
              <el-input v-model="contentForm.orderNum" placeholder="排序" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态" prop="status">
              <el-select v-model="contentForm.status" placeholder="状态" style="width:100%">
                <el-option v-for="dict in dict.type.enable_status" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitContentForm">确 定</el-button>
        <el-button @click="cancelContent">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApp_ad_position, getApp_ad_position, delApp_ad_position, addApp_ad_position, updateApp_ad_position } from "@/api/system/app_ad_position";
import { listApp_ad_content, getApp_ad_content, addApp_ad_content, updateApp_ad_content, delApp_ad_content } from "@/api/system/app_ad_content";

export default {
  name: "App_ad_position",
  dicts: ['enable_status'],
  data() {
    return {
      loading: true,
      ids: [],
      checkedAppAdContent: [],
      checkedExpandContent: {},
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      app_ad_positionList: [],
      appAdContentList: [],
      expandContentMap: {},
      expandLoadingMap: {},
      contentCountMap: {},
      title: "",
      open: false,
      contentOpen: false,
      contentTitle: "",
      contentForm: {},
      contentSaveMode: "api",
      contentDialogIndex: -1,
      contentRules: {
        positionId: [{ required: true, message: "所属广告位不能为空", trigger: "blur" }],
        adName: [{ required: true, message: "广告标题不能为空", trigger: "blur" }],
        status: [{ required: true, message: "广告状态不能为空", trigger: "change" }]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        positionId: null,
        positionName: null,
        positionCode: null,
        status: null
      },
      form: {},
      rules: {
        positionName: [{ required: true, message: "位置名称不能为空", trigger: "blur" }]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listApp_ad_position(this.queryParams).then(response => {
        this.app_ad_positionList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.loadContentCounts();
      });
    },
    loadContentCounts() {
      listApp_ad_content({ pageNum: 1, pageSize: 9999 }).then(response => {
        const map = {};
        (response.rows || []).forEach(item => {
          map[item.positionId] = (map[item.positionId] || 0) + 1;
        });
        this.contentCountMap = map;
      });
    },
    refreshExpandContent(positionId) {
      this.$set(this.expandLoadingMap, positionId, true);
      return getApp_ad_position(positionId).then(response => {
        const list = response.data.appAdContentList || [];
        this.$set(this.expandContentMap, positionId, list);
        this.$set(this.contentCountMap, positionId, list.length);
        return list;
      }).finally(() => {
        this.$set(this.expandLoadingMap, positionId, false);
      });
    },
    handleExpandChange(row, expandedRows) {
      const expanded = expandedRows.some(item => item.positionId === row.positionId);
      if (expanded) {
        this.refreshExpandContent(row.positionId);
      }
    },
    handleManageContent(row) {
      this.$refs.positionTable.toggleRowExpansion(row, true);
      this.refreshExpandContent(row.positionId);
    },
    handleExpandContentSelectionChange(positionId, selection) {
      this.$set(this.checkedExpandContent, positionId, selection.map(item => item.contentId));
    },
    resetContentForm() {
      this.contentForm = {
        contentId: null,
        positionId: null,
        adName: "",
        description: "",
        adImage: "",
        adContent: "",
        startTime: "",
        endTime: "",
        linkUrl: "",
        orderNum: "",
        status: "1"
      };
      this.contentSaveMode = "api";
      this.contentDialogIndex = -1;
      this.$nextTick(() => {
        if (this.$refs.contentForm) {
          this.$refs.contentForm.clearValidate();
        }
      });
    },
    cancelContent() {
      this.contentOpen = false;
      this.resetContentForm();
    },
    handleAddExpandContent(positionRow) {
      this.resetContentForm();
      this.contentForm.positionId = positionRow.positionId;
      this.contentSaveMode = "api";
      this.contentTitle = "添加广告内容";
      this.contentOpen = true;
    },
    handleEditExpandContent(row, positionId) {
      getApp_ad_content(row.contentId).then(response => {
        this.contentForm = { ...response.data, positionId: positionId || response.data.positionId };
        this.contentSaveMode = "api";
        this.contentTitle = "编辑广告内容";
        this.contentOpen = true;
        this.$nextTick(() => {
          if (this.$refs.contentForm) {
            this.$refs.contentForm.clearValidate();
          }
        });
      });
    },
    handleEditDialogContent(row) {
      const index = this.appAdContentList.indexOf(row);
      if (row.contentId) {
        getApp_ad_content(row.contentId).then(response => {
          this.contentForm = { ...response.data };
          this.contentSaveMode = "dialog";
          this.contentDialogIndex = index;
          this.contentTitle = "编辑广告内容详情";
          this.contentOpen = true;
        });
      } else {
        this.contentForm = { ...row, positionId: this.form.positionId };
        this.contentSaveMode = "dialog";
        this.contentDialogIndex = index;
        this.contentTitle = "编辑广告内容详情";
        this.contentOpen = true;
      }
    },
    submitContentForm() {
      this.$refs.contentForm.validate(valid => {
        if (!valid) {
          return;
        }
        if (this.contentSaveMode === "dialog") {
          const merged = { ...this.contentForm };
          if (this.contentDialogIndex >= 0) {
            this.$set(this.appAdContentList, this.contentDialogIndex, { ...this.appAdContentList[this.contentDialogIndex], ...merged });
          }
          this.contentOpen = false;
          this.resetContentForm();
          return;
        }
        const request = this.contentForm.contentId != null
          ? updateApp_ad_content(this.contentForm)
          : addApp_ad_content(this.contentForm);
        request.then(() => {
          this.$modal.msgSuccess(this.contentForm.contentId != null ? "修改成功" : "新增成功");
          this.contentOpen = false;
          const positionId = this.contentForm.positionId;
          this.resetContentForm();
          if (positionId) {
            this.refreshExpandContent(positionId);
          }
          this.loadContentCounts();
        });
      });
    },
    handleDeleteExpandContent(row, positionId) {
      this.$modal.confirm('是否确认删除广告内容"' + (row.adName || row.contentId) + '"？').then(() => {
        return delApp_ad_content(row.contentId);
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.refreshExpandContent(positionId);
        this.loadContentCounts();
      }).catch(() => {});
    },
    handleDeleteExpandContentBatch(positionRow) {
      const ids = this.checkedExpandContent[positionRow.positionId] || [];
      if (!ids.length) {
        this.$modal.msgError("请先选择要删除的广告内容");
        return;
      }
      this.$modal.confirm('是否确认删除选中的 ' + ids.length + ' 条广告内容？').then(() => {
        return delApp_ad_content(ids.join(","));
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.$set(this.checkedExpandContent, positionRow.positionId, []);
        this.refreshExpandContent(positionRow.positionId);
        this.loadContentCounts();
      }).catch(() => {});
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        positionId: null,
        positionName: null,
        positionCode: null,
        createTime: null,
        status: null
      };
      this.appAdContentList = [];
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.positionId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加广告位及内容";
    },
    handleUpdate(row) {
      this.reset();
      const positionId = row.positionId || this.ids;
      getApp_ad_position(positionId).then(response => {
        this.form = response.data;
        this.appAdContentList = response.data.appAdContentList || [];
        this.open = true;
        this.title = "修改广告位及内容";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.appAdContentList = this.appAdContentList;
          const request = this.form.positionId != null ? updateApp_ad_position(this.form) : addApp_ad_position(this.form);
          request.then(() => {
            this.$modal.msgSuccess(this.form.positionId != null ? "修改成功" : "新增成功");
            this.open = false;
            this.expandContentMap = {};
            this.getList();
          });
        }
      });
    },
    handleDelete(row) {
      const positionIds = row.positionId || this.ids;
      this.$modal.confirm('是否确认删除广告位编号为"' + positionIds + '"的数据项？').then(() => {
        return delApp_ad_position(positionIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    rowAppAdContentIndex({ row, rowIndex }) {
      row.index = rowIndex + 1;
    },
    handleAddAppAdContent() {
      this.appAdContentList.push({
        contentId: null,
        adName: "",
        description: "",
        adImage: "",
        adContent: "",
        startTime: "",
        endTime: "",
        linkUrl: "",
        orderNum: "",
        status: "1"
      });
    },
    handleDeleteAppAdContent() {
      if (this.checkedAppAdContent.length === 0) {
        this.$modal.msgError("请先选择要删除的广告内容");
        return;
      }
      this.appAdContentList = this.appAdContentList.filter(item => this.checkedAppAdContent.indexOf(item.index) === -1);
    },
    handleAppAdContentSelectionChange(selection) {
      this.checkedAppAdContent = selection.map(item => item.index);
    },
    handleExport() {
      this.download('system/app_ad_position/export', { ...this.queryParams }, `app_ad_position_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>

<style scoped>
.expand-panel {
  padding: 12px 24px 16px 48px;
  background: #fafafa;
}
.expand-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 8px;
}
.expand-title {
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}
.expand-actions {
  display: flex;
  gap: 8px;
}
.text-muted {
  color: #c0c4cc;
}
.danger-text {
  color: #f56c6c;
}
</style>
