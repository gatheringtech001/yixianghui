<template>
  <div class="app-container migration-page">
    <el-alert
      title="飞书数据已无损迁入后台数据库；本页只读展示原始记录，正式业务表归并会按稳定业务键逐批处理。"
      type="info"
      :closable="false"
      show-icon
    />

    <div class="summary-row">
      <el-card shadow="never">
        <div class="summary-value">{{ tables.length }}</div>
        <div class="summary-label">迁移表</div>
      </el-card>
      <el-card shadow="never">
        <div class="summary-value">{{ totalFields }}</div>
        <div class="summary-label">迁移字段</div>
      </el-card>
      <el-card shadow="never">
        <div class="summary-value">{{ totalRecords }}</div>
        <div class="summary-label">迁移记录</div>
      </el-card>
    </div>

    <div class="content-row">
      <el-card class="table-directory" shadow="never">
        <el-radio-group v-model="activeBase" size="small" @change="selectFirstTable">
          <el-radio-button label="travel">旅居</el-radio-button>
          <el-radio-button label="eldercare">养老</el-radio-button>
        </el-radio-group>
        <div class="directory-list">
          <button
            v-for="item in filteredTables"
            :key="item.sourceTableId"
            type="button"
            :class="['directory-item', { active: item.sourceTableId === selectedTableId }]"
            @click="selectTable(item)"
          >
            <span>{{ item.sourceTableName }}</span>
            <small>{{ item.recordCount }} 条</small>
          </button>
        </div>
      </el-card>

      <el-card class="record-card" shadow="never">
        <div slot="header" class="record-header">
          <div>
            <strong>{{ selectedTable ? selectedTable.sourceTableName : '请选择数据表' }}</strong>
            <span v-if="selectedTable" class="record-meta">
              {{ selectedTable.fieldCount }} 个字段 · {{ selectedTable.recordCount }} 条记录
            </span>
          </div>
          <el-select v-model="queryParams.mergeStatus" clearable size="small" placeholder="归并状态" @change="search">
            <el-option label="待归并" value="pending" />
            <el-option label="已匹配" value="matched" />
            <el-option label="已导入" value="imported" />
            <el-option label="冲突" value="conflict" />
          </el-select>
        </div>

        <el-table v-loading="loading" :data="records" border stripe>
          <el-table-column label="归并状态" width="90" fixed="left">
            <template slot-scope="scope">
              <el-tag :type="statusType(scope.row.mergeStatus)" size="mini">
                {{ statusLabel(scope.row.mergeStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-for="field in visibleFields"
            :key="field.sourceFieldId"
            :label="field.sourceFieldName"
            min-width="170"
            show-overflow-tooltip
          >
            <template slot-scope="scope">
              {{ formatValue(scope.row.fields[field.sourceFieldName]) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="showDetail(scope.row)">全部字段</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="loadRecords"
        />
      </el-card>
    </div>

    <el-drawer :title="detailTitle" :visible.sync="detailOpen" size="52%">
      <div class="detail-list">
        <div v-for="field in fields" :key="field.sourceFieldId" class="detail-row">
          <div class="detail-label">{{ field.sourceFieldName }}</div>
          <div class="detail-value">{{ formatValue(detailFields[field.sourceFieldName]) || '—' }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  listMigrationFields,
  listMigrationRecords,
  listMigrationTables
} from '@/api/system/feishuMigration'

export default {
  name: 'FeishuMigration',
  data() {
    return {
      loading: false,
      tables: [],
      fields: [],
      records: [],
      total: 0,
      activeBase: 'travel',
      selectedTableId: '',
      detailOpen: false,
      detailFields: {},
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        sourceTableId: '',
        mergeStatus: ''
      }
    }
  },
  computed: {
    filteredTables() {
      return this.tables.filter(item => item.baseKey === this.activeBase)
    },
    selectedTable() {
      return this.tables.find(item => item.sourceTableId === this.selectedTableId)
    },
    totalFields() {
      return this.tables.reduce((sum, item) => sum + Number(item.fieldCount || 0), 0)
    },
    totalRecords() {
      return this.tables.reduce((sum, item) => sum + Number(item.recordCount || 0), 0)
    },
    visibleFields() {
      const primary = this.fields.filter(item => Number(item.isPrimary) === 1)
      const others = this.fields.filter(item => Number(item.isPrimary) !== 1).slice(0, 7)
      return primary.concat(others)
    },
    detailTitle() {
      return (this.selectedTable ? this.selectedTable.sourceTableName : '') + ' · 全部字段'
    }
  },
  created() {
    this.loadTables()
  },
  methods: {
    async loadTables() {
      const response = await listMigrationTables()
      this.tables = response.data || []
      this.selectFirstTable()
    },
    selectFirstTable() {
      const first = this.filteredTables[0]
      if (first) this.selectTable(first)
    },
    async selectTable(item) {
      this.selectedTableId = item.sourceTableId
      this.queryParams.sourceTableId = item.sourceTableId
      this.queryParams.pageNum = 1
      const response = await listMigrationFields(item.sourceTableId)
      this.fields = response.data || []
      this.loadRecords()
    },
    async loadRecords() {
      if (!this.queryParams.sourceTableId) return
      this.loading = true
      try {
        const response = await listMigrationRecords(this.queryParams)
        this.records = response.rows || []
        this.total = response.total || 0
      } finally {
        this.loading = false
      }
    },
    search() {
      this.queryParams.pageNum = 1
      this.loadRecords()
    },
    showDetail(row) {
      this.detailFields = row.fields || {}
      this.detailOpen = true
    },
    formatValue(value) {
      if (value === null || value === undefined || value === '') return ''
      if (Array.isArray(value)) return value.map(item => this.formatValue(item)).filter(Boolean).join('、')
      if (typeof value === 'object') {
        if (value.text || value.name || value.label) return value.text || value.name || value.label
        return JSON.stringify(value)
      }
      return String(value)
    },
    statusLabel(status) {
      return { pending: '待归并', matched: '已匹配', imported: '已导入', conflict: '冲突' }[status] || status
    },
    statusType(status) {
      return { pending: 'warning', matched: '', imported: 'success', conflict: 'danger' }[status] || 'info'
    }
  }
}
</script>

<style scoped>
.migration-page { background: #f5f7fa; min-height: calc(100vh - 84px); }
.summary-row { display: grid; grid-template-columns: repeat(3, minmax(160px, 1fr)); gap: 16px; margin: 16px 0; }
.summary-value { color: #303133; font-size: 28px; font-weight: 700; }
.summary-label { color: #909399; margin-top: 6px; }
.content-row { display: grid; grid-template-columns: 250px minmax(0, 1fr); gap: 16px; align-items: start; }
.table-directory { position: sticky; top: 16px; }
.directory-list { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; }
.directory-item { display: flex; justify-content: space-between; gap: 10px; padding: 11px 12px; border: 1px solid #ebeef5; border-radius: 6px; background: #fff; color: #606266; cursor: pointer; text-align: left; }
.directory-item.active { border-color: #409eff; background: #ecf5ff; color: #409eff; }
.directory-item small { color: #909399; white-space: nowrap; }
.record-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.record-meta { color: #909399; font-size: 12px; margin-left: 12px; }
.detail-list { padding: 0 24px 24px; }
.detail-row { display: grid; grid-template-columns: 190px minmax(0, 1fr); border-bottom: 1px solid #ebeef5; padding: 12px 0; }
.detail-label { color: #606266; font-weight: 600; padding-right: 16px; }
.detail-value { color: #303133; white-space: pre-wrap; word-break: break-all; }
@media (max-width: 900px) {
  .content-row { grid-template-columns: 1fr; }
  .table-directory { position: static; }
}
</style>
