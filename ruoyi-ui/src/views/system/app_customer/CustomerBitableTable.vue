<template>
  <div class="customer-bitable">
    <div class="bitable-toolbar">
      <div class="toolbar-left">
        <span class="record-count">共 {{ total }} 条记录</span>
        <span class="view-hint">点击行查看详情 · 列设置可自定义显示字段</span>
      </div>
      <div class="toolbar-right">
        <el-button size="mini" icon="el-icon-view" @click="$emit('show-default-columns')">常用列</el-button>
        <el-popover placement="bottom-end" width="440" trigger="click">
          <el-button slot="reference" size="mini" icon="el-icon-s-operation">列设置</el-button>
          <div class="column-picker">
            <div v-for="group in columnGroups" :key="group.key">
              <div class="group-title">{{ group.label }}</div>
              <el-checkbox-group :value="visibleColumnProps" @input="$emit('update:visibleColumnProps', $event)">
                <el-checkbox
                  v-for="prop in group.props"
                  :key="prop"
                  :label="prop"
                >{{ columnLabelMap[prop] }}</el-checkbox>
              </el-checkbox-group>
            </div>
            <div class="picker-actions">
              <el-button size="mini" @click="$emit('reset-columns')">恢复默认</el-button>
              <el-button size="mini" type="primary" @click="$emit('save-columns')">保存列配置</el-button>
            </div>
          </div>
        </el-popover>
      </div>
    </div>

    <el-table
      ref="customerTable"
      v-loading="loading"
      :data="list"
      :height="tableHeight"
      row-key="customerId"
      border
      stripe
      highlight-current-row
      :default-sort="defaultSort"
      @selection-change="$emit('selection-change', $event)"
      @row-click="(row) => $emit('row-click', row)"
      @sort-change="handleSortChange"
    >
      <el-table-column type="selection" width="48" align="center" fixed="left" />
      <el-table-column
        v-for="col in visibleColumns"
        :key="col.prop"
        :label="col.label"
        :prop="col.prop"
        :width="col.width"
        :min-width="col.minWidth"
        :fixed="col.fixed"
        :show-overflow-tooltip="col.tooltip !== false"
        :sortable="col.sortable ? 'custom' : false"
        :sort-orders="['descending', 'ascending']"
        align="center"
      >
        <template slot-scope="scope">
          <dict-tag
            v-if="col.dict && dict.type[col.dict]"
            :options="dict.type[col.dict]"
            :value="scope.row[col.prop]"
          />
          <span v-else-if="col.type === 'date'">{{ formatDate(scope.row[col.prop]) }}</span>
          <span v-else-if="col.prop === 'customerName'" class="cell-name">{{ scope.row[col.prop] || '-' }}</span>
          <span v-else :class="{ 'cell-empty': isEmpty(scope.row[col.prop]) }">{{ displayCell(scope.row[col.prop]) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-view" @click.stop="$emit('detail', scope.row)">详情</el-button>
          <el-button type="text" size="mini" icon="el-icon-edit" @click.stop="$emit('edit', scope.row)">编辑</el-button>
          <el-button type="text" size="mini" icon="el-icon-delete" class="danger-text" @click.stop="$emit('delete', scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="load-more-bar">
      <span class="loaded-tip">已加载 {{ list.length }} / {{ total }} 条</span>
      <el-button
        v-if="!isAllLoaded"
        type="primary"
        plain
        size="small"
        :loading="loadingMore"
        @click="$emit('load-more')"
      >加载更多</el-button>
      <span v-else-if="total > 0" class="all-loaded-tip">已全部加载</span>
    </div>
  </div>
</template>

<script>
import { ALL_COLUMNS, COLUMN_GROUPS } from './customerColumns'

export default {
  name: 'CustomerBitableTable',
  props: {
    list: { type: Array, default: () => [] },
    loading: Boolean,
    total: { type: Number, default: 0 },
    dict: { type: Object, default: () => ({ type: {} }) },
    visibleColumnProps: { type: Array, default: () => [] },
    tableHeight: { type: [String, Number], default: 'calc(100vh - 380px)' },
    loadingMore: Boolean,
    isAllLoaded: Boolean,
    defaultSort: {
      type: Object,
      default: () => ({ prop: 'signTime', order: 'descending' })
    }
  },
  watch: {
    list() {
      this.syncTableLayout()
    },
    visibleColumnProps() {
      this.syncTableLayout()
    }
  },
  mounted() {
    this.syncTableLayout()
    this._resizeHandler = () => this.syncTableLayout()
    window.addEventListener('resize', this._resizeHandler)
  },
  beforeDestroy() {
    if (this._resizeHandler) {
      window.removeEventListener('resize', this._resizeHandler)
    }
  },
  computed: {
    columnGroups() {
      return COLUMN_GROUPS
    },
    columnLabelMap() {
      return ALL_COLUMNS.reduce((acc, col) => {
        acc[col.prop] = col.label
        return acc
      }, {})
    },
    visibleColumns() {
      const map = ALL_COLUMNS.reduce((acc, col) => {
        acc[col.prop] = col
        return acc
      }, {})
      return this.visibleColumnProps.map(prop => map[prop]).filter(Boolean)
    }
  },
  methods: {
    handleSortChange(column) {
      this.$emit('sort-change', column)
    },
    syncTableLayout() {
      this.$nextTick(() => {
        if (this.$refs.customerTable) {
          this.$refs.customerTable.doLayout()
        }
      })
    },
    formatDate(val) {
      if (!val) return '-'
      return this.parseTime(val, '{y}-{m}-{d}')
    },
    isEmpty(val) {
      return val === null || val === undefined || val === ''
    },
    displayCell(val) {
      return this.isEmpty(val) ? '-' : val
    }
  }
}
</script>

<style scoped lang="scss">
@import './customer-table.scss';

.danger-text {
  color: #f56c6c;
}

.load-more-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 0 8px;
  border-top: 1px solid #ebeef5;

  .loaded-tip {
    font-size: 13px;
    color: #909399;
  }

  .all-loaded-tip {
    font-size: 13px;
    color: #c0c4cc;
  }
}
</style>
