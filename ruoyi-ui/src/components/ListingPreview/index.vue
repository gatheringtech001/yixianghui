<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="760px"
    custom-class="listing-preview"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="!busy"
    :show-close="!busy"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="preview-heading">
      <image-preview v-if="cover" :src="cover" :width="88" :height="88" />
      <div><h2>{{ name || '未命名' }}</h2><el-tag :type="String(status) === '1' ? 'success' : 'info'">{{ String(status) === '1' ? '已上架' : '未上架' }}</el-tag></div>
    </div>
    <dl class="preview-fields"><template v-for="row in rows"><dt :key="row.label + '-label'">{{ row.label }}</dt><dd :key="row.label">{{ row.value === null || row.value === undefined || row.value === '' ? '—' : row.value }}</dd></template></dl>
    <section v-for="section in sections" :key="section.label" class="preview-section">
      <h3>{{ section.label }}</h3><p>{{ text(section.content) || '—' }}</p>
    </section>
    <div slot="footer"><el-button :disabled="busy" @click="$emit('update:visible', false)">{{ confirmable ? '返回修改' : '关闭' }}</el-button><el-button v-if="confirmable" type="primary" :loading="busy" @click="$emit('confirm')">确认保存</el-button></div>
  </el-dialog>
</template>
<script>
import { plainContent } from '@/utils/listing-operations.mjs'
export default {
  name: 'ListingPreview',
  props: { visible: Boolean, busy: Boolean, confirmable: Boolean, title: { type: String, default: '确认发布内容' }, name: { type: String, default: '' }, cover: { type: String, default: '' }, status: { type: [String, Number], default: '0' }, rows: { type: Array, default: () => [] }, sections: { type: Array, default: () => [] }},
  methods: { text: plainContent }
}
</script>
<style>
.listing-preview { max-width: calc(100vw - 24px); margin-top: 5vh !important; }
.listing-preview .el-dialog__body { max-height: 68vh; overflow-y: auto; overflow-wrap: anywhere; }
.preview-heading { display: flex; gap: 20px; align-items: center; margin-bottom: 24px; }
.preview-heading h2 { margin: 0 0 12px; font-size: 20px; color: #303133; }
.preview-fields { display: grid; grid-template-columns: 100px minmax(0, 1fr); margin: 0; }
.preview-fields dt, .preview-fields dd { padding: 10px 0; border-bottom: 1px solid #ebeef5; margin: 0; }
.preview-fields dt { color: #909399; }
.preview-section { margin-top: 24px; }.preview-section h3 { font-size: 15px; }.preview-section p { white-space: pre-wrap; line-height: 1.8; }
@media (max-width: 600px) { .preview-heading { gap: 12px; }.preview-fields { grid-template-columns: 84px minmax(0, 1fr); }.listing-preview .el-dialog__body { padding: 16px; } }
</style>
