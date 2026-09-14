"""Fail-closed person matching; existing source identities are never reassigned."""

TEMP_TABLES = ('tmp_fs_people', 'tmp_fs_keys', 'tmp_fs_names', 'tmp_fs_source',
               'tmp_fs_source_keys', 'tmp_fs_source_names', 'tmp_fs_choices')


def record_scope(table, alias='p', target=False):
    identifiers = [record['record_id'] for record in table['records']]
    if not identifiers:
        return '0=1'
    source_field = 'target_source_table_id' if target else 'source_table_id'
    record_field = 'target_source_record_id' if target else 'feishu_record_id'
    ids = ','.join('0x' + value.encode('utf-8').hex() for value in identifiers)
    source = '0x' + table['table_id'].encode('utf-8').hex()
    return f"BINARY {alias}.{source_field}={source} AND BINARY {alias}.{record_field} IN ({ids})"


def normalized_key(expression, numeric=False):
    text = f"NULLIF(TRIM(CAST({expression} AS CHAR)), '')"
    if numeric:
        return f"CASE WHEN {text} REGEXP '^[0-9]+([.][0-9]+)?$' THEN CAST(CAST({text} AS DECIMAL(20,4)) AS CHAR) ELSE NULL END"
    return text


def prepare_identity(options):
    o = options
    key = normalized_key(f"c.{o['key']}", o.get('numeric', False))
    source_key = normalized_key(f"p.{o['source_key']}", o.get('numeric', False))
    name = f"NULLIF(TRIM(p.{o['source_name']}), '')"
    active = o.get('active', '1')
    changed_key = "p.match_key IS NOT NULL AND NOT (BINARY e.match_key <=> BINARY p.match_key)"
    if o['key'] == o['origin']:
        changed_key += " AND (e.origin IS NULL OR LEFT(e.origin,3)<>'FS-')"
    # 快照仅在当前连接内存在，避免一次导入前后候选集变化导致任意认领。
    statements = [f"DROP TEMPORARY TABLE IF EXISTS {table};" for table in reversed(TEMP_TABLES)]
    statements += [
        f"CREATE TEMPORARY TABLE tmp_fs_people AS SELECT c.{o['id']} person_id,"
        f"{key} match_key,NULLIF(TRIM(c.{o['name']}),'') person_name,c.{o['origin']} origin,{active} active "
        f"FROM {o['destination']} c;",
        "CREATE TEMPORARY TABLE tmp_fs_keys AS SELECT match_key,COUNT(*) matches,MIN(person_id) person_id,"
        "MIN(person_name) person_name,MIN(active) active FROM tmp_fs_people WHERE match_key IS NOT NULL GROUP BY match_key;",
        "CREATE TEMPORARY TABLE tmp_fs_names AS SELECT person_name,COUNT(*) matches FROM tmp_fs_people "
        "WHERE person_name IS NOT NULL GROUP BY person_name;",
        f"CREATE TEMPORARY TABLE tmp_fs_source AS SELECT p.source_table_id,p.feishu_record_id,{source_key} match_key,"
        f"{name} person_name,p.canonical_status FROM `{o['profile']}` p WHERE {o['scope']};",
        "CREATE TEMPORARY TABLE tmp_fs_source_keys AS SELECT match_key,COUNT(*) matches FROM tmp_fs_source "
        "WHERE match_key IS NOT NULL GROUP BY match_key;",
        "CREATE TEMPORARY TABLE tmp_fs_source_names AS SELECT person_name,COUNT(*) matches FROM tmp_fs_source "
        "WHERE person_name IS NOT NULL GROUP BY person_name;",
    ]
    statements.append(
        "CREATE TEMPORARY TABLE tmp_fs_choices AS SELECT p.source_table_id,p.feishu_record_id,"
        f"COALESCE(s.{o['id']},k.person_id) person_id,CASE "
        "WHEN p.canonical_status='skipped' THEN 'skipped' "
        "WHEN s.source_record_id IS NOT NULL THEN CASE "
        "WHEN e.person_id IS NULL OR e.active=0 OR p.person_name IS NULL OR s.match_status='needs_review' "
        "OR NOT (BINARY e.person_name <=> BINARY p.person_name) "
        f"OR ({changed_key}) "
        "OR (s.match_status='created' AND COALESCE(n.matches,0)>1) THEN 'needs_review' ELSE 'existing' END "
        "WHEN p.person_name IS NULL OR p.match_key IS NULL OR sk.matches>1 OR sn.matches>1 THEN 'needs_review' "
        "WHEN k.matches=1 AND k.active=1 AND BINARY k.person_name=BINARY p.person_name THEN 'matched' "
        "WHEN COALESCE(k.matches,0)>0 OR COALESCE(n.matches,0)>0 "
        f"OR EXISTS (SELECT 1 FROM {o['destination']} c WHERE BINARY c.{o['origin']}=BINARY CONCAT('FS-',p.feishu_record_id)) "
        "THEN 'needs_review' ELSE 'create' END action FROM tmp_fs_source p "
        f"LEFT JOIN {o['mapping']} s ON BINARY s.source_table_id=BINARY p.source_table_id AND BINARY s.source_record_id=BINARY p.feishu_record_id "
        f"LEFT JOIN tmp_fs_people e ON e.person_id=s.{o['id']} "
        "LEFT JOIN tmp_fs_keys k ON BINARY k.match_key=BINARY p.match_key "
        "LEFT JOIN tmp_fs_names n ON BINARY n.person_name=BINARY p.person_name "
        "LEFT JOIN tmp_fs_source_keys sk ON BINARY sk.match_key=BINARY p.match_key "
        "LEFT JOIN tmp_fs_source_names sn ON BINARY sn.person_name=BINARY p.person_name;"
    )
    return statements


def finish_identity(options):
    o = options
    return [
        f"UPDATE {o['mapping']} s JOIN tmp_fs_choices d ON BINARY s.source_table_id=BINARY d.source_table_id "
        "AND BINARY s.source_record_id=BINARY d.feishu_record_id SET s.match_status='needs_review' WHERE d.action='needs_review';",
        f"UPDATE `{o['profile']}` p JOIN tmp_fs_choices d ON BINARY d.source_table_id=BINARY p.source_table_id "
        f"AND BINARY d.feishu_record_id=BINARY p.feishu_record_id LEFT JOIN {o['mapping']} s "
        "ON BINARY s.source_table_id=BINARY p.source_table_id AND BINARY s.source_record_id=BINARY p.feishu_record_id "
        f"SET p.canonical_table=IF(d.action='skipped',NULL,'{o['destination']}'),p.canonical_id=IF(d.action IN ('needs_review','skipped'),NULL,s.{o['id']}),"
        f"p.canonical_status=IF(d.action='skipped','skipped',IF(d.action='needs_review' OR s.{o['id']} IS NULL,'needs_review','linked')),"
        f"p.canonical_message=IF(d.action='skipped',p.canonical_message,IF(d.action='needs_review' OR s.{o['id']} IS NULL,'identity_requires_review',NULL));",
        f"UPDATE app_feishu_migration_record r JOIN `{o['profile']}` p "
        "ON BINARY r.source_table_id=BINARY p.source_table_id AND BINARY r.source_record_id=BINARY p.feishu_record_id "
        "SET r.merge_status=CASE p.canonical_status WHEN 'skipped' THEN 'skipped' WHEN 'linked' THEN 'merged' ELSE 'conflict' END,"
        f"r.target_table=p.canonical_table,r.target_id=p.canonical_id,r.merge_message=p.canonical_message WHERE {o['scope']};",
    ] + [f"DROP TEMPORARY TABLE IF EXISTS {table};" for table in reversed(TEMP_TABLES)]


def choice_join():
    return ("JOIN tmp_fs_choices d ON BINARY d.source_table_id=BINARY p.source_table_id "
            "AND BINARY d.feishu_record_id=BINARY p.feishu_record_id ")
