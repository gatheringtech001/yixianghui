<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="顾问编号" prop="consultantNo">
        <el-input
          v-model="queryParams.consultantNo"
          placeholder="请输入顾问编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属站点" prop="deptId">
        <el-input
          v-model="queryParams.deptId"
          placeholder="请输入所属站点"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="顾问姓名" prop="consultantName">
        <el-input
          v-model="queryParams.consultantName"
          placeholder="请输入顾问姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="顾问电话" prop="mobile">
        <el-input
          v-model="queryParams.mobile"
          placeholder="请输入顾问电话"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="身份证号" prop="idcard">
        <el-input
          v-model="queryParams.idcard"
          placeholder="请输入身份证号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="系统用户ID" prop="userId" v-if="false">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入系统用户ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option
            v-for="dict in dict.type.consultant_status"
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
          v-hasPermi="['system:app_consultant:add']"
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
          v-hasPermi="['system:app_consultant:edit']"
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
          v-hasPermi="['system:app_consultant:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['system:app_consultant:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_consultant:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_consultantList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="顾问ID" align="center" prop="consultantId" v-if="false"/>
      <el-table-column label="顾问编号" align="center" prop="consultantNo" show-overflow-tooltip/>
      <el-table-column label="所属站点" align="center" prop="deptId" min-width="120" show-overflow-tooltip/>
      <el-table-column label="顾问姓名" align="center" prop="consultantName" />
      <el-table-column label="顾问电话" align="center" prop="mobile" min-width="100"/>
      <el-table-column label="身份证号" align="center" prop="idcard" min-width="120"/>
      <el-table-column label="推荐人" align="center" prop="inviterName" />
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip/>
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.consultant_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="账号绑定" align="center" min-width="140" show-overflow-tooltip>
        <template slot-scope="scope">
          <el-tag v-if="scope.row.userId" type="success" size="mini">已绑定</el-tag>
          <el-tag v-else type="info" size="mini">未激活</el-tag>
          <span v-if="scope.row.userId" class="bind-user-name">{{ formatLinkedUser(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:app_consultant:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:app_consultant:remove']"
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

    <!-- 添加或修改康养顾问对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="顾问编号" prop="consultantNo">
          <el-input v-model="form.consultantNo" placeholder="请输入顾问编号" />
        </el-form-item>

        <el-form-item label="所属站点" prop="deptId">
          <treeselect v-model="form.deptId" :options="deptOptions" :normalizer="normalizerDept" placeholder="选择所属站点" />
        </el-form-item>
        <el-form-item label="顾问姓名" prop="consultantName">
          <el-input v-model="form.consultantName" placeholder="请输入顾问姓名" />
        </el-form-item>
        <el-form-item label="顾问电话" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入顾问电话" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idcard">
          <el-input v-model="form.idcard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.consultant_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="账号绑定">
          <template v-if="form.userId">
            <el-tag type="success" size="mini">已绑定</el-tag>
            <span class="bind-user-name">{{ form.userNickName || '用户' }}（ID:{{ form.userId }}）</span>
          </template>
          <template v-else>
            <el-tag type="info" size="mini">未激活</el-tag>
            <div class="form-tip">顾问使用与档案相同的手机号登录小程序后，系统将自动认领账号；每次登录会校验手机号是否仍与档案一致。</div>
          </template>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
    <!-- 导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的客户数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link type="primary" :underline="false" style="font-size: 12px; vertical-align: baseline" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApp_consultant, getApp_consultant, delApp_consultant, addApp_consultant, updateApp_consultant } from "@/api/system/app_consultant";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import { listDept } from "@/api/system/dept";
import {getToken} from "@/utils/auth";

export default {
  name: "App_consultant",
  dicts: ['consultant_status'],
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
      // 康养顾问表格数据
      app_consultantList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        consultantNo: null,
        deptId: null,
        consultantName: null,
        inviterName: null,
        mobile: null,
        idcard: null,
        status: null,
        userId: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      },

      // 导入参数
      upload: {
        // 是否显示弹出层（导入）
        open: false,
        // 弹出层标题（导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/system/app_consultant/importData"
      },
      // 分站树选项
      deptOptions: [],
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询康养顾问列表 */
    getList() {
      this.loading = true;
      listApp_consultant(this.queryParams).then(response => {
        this.app_consultantList = response.rows;
        this.total = response.total;
        this.loading = false;
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
        console.log(response.data)
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
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        consultantId: null,
        consultantNo: null,
        deptId: null,
        consultantName: null,
        mobile: null,
        idcard: null,
        status: null,
        createTime: null,
        userId: null,
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
      this.ids = selection.map(item => item.consultantId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.getDeptTreeselect();
      this.open = true;
      this.title = "添加康养顾问";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getDeptTreeselect();
      const consultantId = row.consultantId || this.ids
      getApp_consultant(consultantId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改康养顾问";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.consultantId != null) {
            updateApp_consultant(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_consultant(this.form).then(response => {
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
      const consultantIds = row.consultantId || this.ids;
      this.$modal.confirm('是否确认删除康养顾问编号为"' + consultantIds + '"的数据项？').then(function() {
        return delApp_consultant(consultantIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_consultant/export', {
        ...this.queryParams
      }, `app_consultant_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "客户导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('system/app_consultant/importTemplate', {
      }, `客户数据_导出模板_${new Date().getTime()}.xlsx`)
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
      this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    },
    formatLinkedUser(row) {
      const name = row.userNickName || '用户';
      return `${name}（${row.userId}）`;
    }
  }
};
</script>

<style scoped>
.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.bind-user-name {
  margin-left: 6px;
  font-size: 12px;
  color: #606266;
}
</style>
