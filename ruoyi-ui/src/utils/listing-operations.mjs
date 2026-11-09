// Keep publication checks and presentation independent of the editor's mutable state.
export function publicationLabel(status) {
  return String(status) === '1' ? '已上架' : '未上架'
}

export function treeLabel(nodes, id) {
  for (const node of nodes || []) {
    if (String(node.categoryId ?? node.deptId ?? node.id) === String(id)) {
      return node.categoryName || node.deptName || node.label || '未分类'
    }
    const child = treeLabel(node.children, id)
    if (child) return child
  }
  return ''
}

export function publicationIssues(form, kind, skus = [], now = Date.now()) {
  const issues = []
  const activity = kind === 'activity'
  if (!(activity ? form.activityName : form.goodsName)?.trim()) issues.push('填写名称')
  if (!form.deptId) issues.push('选择所属站点')
  if (!activity && !form.categoryId) issues.push('选择分类')
  if (!(activity ? form.activityCover : form.goodsCover)) issues.push('上传封面')
  if (activity) {
    if (!form.address?.trim()) issues.push('填写活动地点')
    if (!form.activityTime?.trim()) issues.push('填写活动时间')
    if (form.isFree === 0 && !(Number(form.vipPrice) > 0)) issues.push('填写大于 0 的实付价')
    for (const key of ['signEndTime', 'activityEndTime']) {
      if (form[key] && new Date(String(form[key]).replace(/-/g, '/')).getTime() <= now) {
        issues.push(key === 'signEndTime' ? '报名截止时间已过' : '活动已结束')
      }
    }
    if (Number(form.maxCount) > 0 && Number(form.signCount) >= Number(form.maxCount)) issues.push('报名名额已满')
  } else if (form.isSku === 1 && form.goodsType !== 'education') {
    if (!skus.some(sku => String(sku.status) === '1' && Number(sku.stock) > 0 && Number(sku.price) > 0)) {
      issues.push('配置有价格和库存的启用规格')
    }
  } else {
    if (!(Number(form.price) > 0)) issues.push('填写大于 0 的价格')
    if (!(Number(form.stock) > 0)) issues.push('填写可售库存')
  }
  return issues
}

export function plainContent(html) {
  return String(html || '').replace(/<\/(?:p|div|h[1-6]|li)>/gi, '\n')
    .replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').trim()
}

export function reviewQuery(params, tab) {
  const query = { ...params, status: null, 'params[reviewBucket]': null }
  if (tab === 'pending') query.status = '0'
  if (tab === 'processed') query['params[reviewBucket]'] = 'processed'
  if (tab === 'exception') query.status = '5'
  return query
}

// An already published listing may be sold out or ended; unrelated edits stay possible.
export function isNewPublication(previousStatus, nextStatus) {
  return String(previousStatus) !== '1' && String(nextStatus) === '1'
}
