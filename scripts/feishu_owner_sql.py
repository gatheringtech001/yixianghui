"""Resolve order User fields to verified business consultants, not login accounts."""
from feishu_identity_sql import record_scope


def owner_relations_sql(table):
    field = next(f for f in table['fields'] if f['field_name'] == '客服负责人')
    if field.get('ui_type') != 'User' or field.get('property', {}).get('multiple') is not False:
        raise ValueError('Order owner must be a single Feishu User field')
    field_id = "CONVERT(0x" + field['field_id'].encode().hex() + " USING utf8mb4)"
    message = 'owner_user_unique_consultant_name'
    user_scope = record_scope(table, 'app_feishu_business_user').replace('.feishu_record_id', '.source_record_id')
    relation_scope = record_scope(table, 'rel').replace('.feishu_record_id', '.source_record_id')
    return [
        'DROP TEMPORARY TABLE IF EXISTS tmp_fs_owner_people;',
        "CREATE TEMPORARY TABLE tmp_fs_owner_people AS SELECT MIN(consultant_id) consultant_id,"
        "MIN(consultant_name) consultant_name FROM app_consultant WHERE consultant_name IS NOT NULL "
        "AND TRIM(consultant_name)<>'' GROUP BY BINARY consultant_name "
        "HAVING COUNT(*)=1 AND MIN(status)='01';",
        'DROP TEMPORARY TABLE IF EXISTS tmp_fs_owner_users;',
        "CREATE TEMPORARY TABLE tmp_fs_owner_users AS SELECT source_table_id,source_record_id,"
        "MIN(user_name) user_name FROM app_feishu_business_user "
        f"WHERE BINARY source_field_id=BINARY {field_id} AND {user_scope} "
        "GROUP BY source_table_id,source_record_id HAVING COUNT(*)=1 "
        "AND MIN(COALESCE(feishu_user_id,''))<>'';",
        'DROP TEMPORARY TABLE IF EXISTS tmp_fs_owner_sources;',
        "CREATE TEMPORARY TABLE tmp_fs_owner_sources AS SELECT canonical_id,MIN(source_table_id) source_table_id,"
        "MIN(feishu_record_id) feishu_record_id FROM app_consultant_feishu "
        "WHERE canonical_table='app_consultant' AND canonical_status='linked' GROUP BY canonical_id HAVING COUNT(*)=1;",
        "DELETE rel FROM app_feishu_business_relation rel WHERE "
        f"{relation_scope} AND BINARY rel.source_field_id=BINARY {field_id} "
        f"AND rel.relation_message='{message}';",
        "INSERT INTO app_feishu_business_relation (source_table_id,source_record_id,source_field_id,"
        "target_source_table_id,target_source_record_id,target_business_table,target_business_id,"
        "display_text,relation_status,relation_message,relation_order) "
        f"SELECT p.source_table_id,p.feishu_record_id,{field_id},s.source_table_id,s.feishu_record_id,"
        f"'app_consultant',c.consultant_id,c.consultant_name,'resolved','{message}',0 "
        "FROM app_travel_order_profile p JOIN tmp_fs_owner_users u "
        "ON BINARY u.source_table_id=BINARY p.source_table_id AND BINARY u.source_record_id=BINARY p.feishu_record_id "
        "JOIN tmp_fs_owner_people c ON BINARY c.consultant_name=BINARY u.user_name "
        "JOIN tmp_fs_owner_sources s ON s.canonical_id=c.consultant_id "
        f"WHERE p.canonical_status='linked' AND p.canonical_id IS NOT NULL AND {record_scope(table)};",
        'DROP TEMPORARY TABLE IF EXISTS tmp_fs_owner_people;',
        'DROP TEMPORARY TABLE IF EXISTS tmp_fs_owner_users;',
        'DROP TEMPORARY TABLE IF EXISTS tmp_fs_owner_sources;',
    ]
