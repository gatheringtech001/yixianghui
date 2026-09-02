import request from '@/utils/request'

export function listMigrationTables() {
  return request({
    url: '/system/feishu_migration/tables',
    method: 'get'
  })
}

export function listMigrationFields(sourceTableId) {
  return request({
    url: '/system/feishu_migration/fields/' + sourceTableId,
    method: 'get'
  })
}

export function listMigrationRecords(query) {
  return request({
    url: '/system/feishu_migration/records',
    method: 'get',
    params: query
  })
}
