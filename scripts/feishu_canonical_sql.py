"""Build canonical business-domain SQL for the normalized Feishu tables."""

from feishu_structured_model import TARGET_TABLES, column_name
from feishu_identity_sql import choice_join, finish_identity, prepare_identity, record_scope
from feishu_owner_sql import owner_relations_sql


def _q(value):
    if value is None:
        return "NULL"
    return f"CONVERT(0x{str(value).encode('utf-8').hex()} USING utf8mb4)"


def _table(tables, base_key, table_name):
    return next(table for base, table in tables
                if base["key"] == base_key and table["name"] == table_name)


def _col(table, field_name):
    field = next(field for field in table["fields"] if field["field_name"] == field_name)
    return f"`{column_name(field['field_id'])}`"


def _self_links(tables):
    statements = []
    for base, table in tables:
        target = TARGET_TABLES[(base["key"], table["name"])]
        if target in {'app_travel_customer_profile', 'app_eldercare_customer_profile',
                      'app_consultant_feishu', 'app_customer_income_feishu',
                      'app_travel_order_profile', 'app_activity_plan_feishu'}:
            continue
        statements.append(
            f"UPDATE `{target}` p SET canonical_table={_q(target)},canonical_id=business_id,"
            f"canonical_status='linked',canonical_message=NULL WHERE canonical_status<>'skipped' AND {record_scope(table)};"
        )
    return statements


def _customer_sql(tables):
    travel = _table(tables, "travel", "客户信息表")
    elder = _table(tables, "eldercare", "客户档案")
    travel_target = TARGET_TABLES[("travel", "客户信息表")]
    elder_target = TARGET_TABLES[("eldercare", "客户档案")]
    d = [
        "CREATE TABLE IF NOT EXISTS app_customer_feishu_source (source_table_id varchar(64) NOT NULL,source_record_id varchar(64) NOT NULL,customer_id bigint unsigned NOT NULL,business_line varchar(20) NOT NULL,match_method varchar(32) NOT NULL,match_status varchar(16) NOT NULL,match_message varchar(500) DEFAULT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(source_table_id,source_record_id),KEY idx_customer_feishu_source_customer(customer_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    ]
    m = []
    for table, target, business_line, key_name, db_key in (
        (travel, travel_target, "travel", "客户编号", "customer_no"),
        (elder, elder_target, "eldercare", "电话", "link_mobile"),
    ):
        identity = {'profile': target, 'destination': 'app_customer', 'mapping': 'app_customer_feishu_source',
                    'id': 'customer_id', 'name': 'customer_name', 'origin': 'customer_no', 'key': db_key,
                    'source_key': _col(table, key_name), 'source_name': _col(table, '客户名称'),
                    'numeric': business_line == 'travel', 'active': "IF(c.del_flag='0',1,0)", 'scope': record_scope(table)}
        m += prepare_identity(identity)
        m.append(
            "INSERT INTO app_customer_feishu_source (source_table_id,source_record_id,customer_id,business_line,match_method,match_status,match_message) "
            f"SELECT d.source_table_id,d.feishu_record_id,d.person_id,{_q(business_line)},{_q(db_key)},'matched',NULL "
            "FROM tmp_fs_choices d WHERE d.action='matched';"
        )
        name = _col(table, "客户名称")
        phone = _col(table, "联系方式" if business_line == "travel" else "电话")
        source = _col(table, "来源" if business_line == "travel" else "客户获取渠道")
        label = _col(table, "客户状态" if business_line == "travel" else "客户标签")
        sign_time = _col(table, "日期" if business_line == "travel" else "登记日期")
        m.append(
            "INSERT INTO app_customer (customer_name,customer_no,link_mobile,acquisition_channel,customer_label,sign_time,status,create_by,create_time,del_flag) "
            f"SELECT p.{name},CONCAT('FS-',p.feishu_record_id),p.{phone},p.{source},p.{label},p.{sign_time},'0','feishu',CURRENT_TIMESTAMP,'0' "
            f"FROM `{target}` p {choice_join()} WHERE d.action='create';"
        )
        m.append(
            "INSERT INTO app_customer_feishu_source (source_table_id,source_record_id,customer_id,business_line,match_method,match_status,match_message) "
            f"SELECT p.source_table_id,p.feishu_record_id,c.customer_id,{_q(business_line)},'created','created',NULL "
            f"FROM `{target}` p {choice_join()} JOIN app_customer c ON BINARY c.customer_no=BINARY CONCAT('FS-',p.feishu_record_id) "
            "WHERE d.action='create';"
        )
        m += finish_identity(identity)
    return d, m


def _consultant_sql(tables):
    table = _table(tables, "eldercare", "养老顾问列表")
    target = TARGET_TABLES[("eldercare", "养老顾问列表")]
    name, phone, remark = (_col(table, value) for value in ("养老顾问", "电话", "备注"))
    d = [
        "CREATE TABLE IF NOT EXISTS app_consultant_feishu_source (source_table_id varchar(64) NOT NULL,source_record_id varchar(64) NOT NULL,consultant_id bigint unsigned NOT NULL,match_status varchar(16) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(source_table_id,source_record_id),KEY idx_consultant_feishu_source_consultant(consultant_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    ]
    identity = {'profile': target, 'destination': 'app_consultant', 'mapping': 'app_consultant_feishu_source',
                'id': 'consultant_id', 'name': 'consultant_name', 'origin': 'consultant_no', 'key': 'mobile',
                'source_key': phone, 'source_name': name, 'scope': record_scope(table)}
    m = prepare_identity(identity) + [
        "INSERT INTO app_consultant_feishu_source (source_table_id,source_record_id,consultant_id,match_status) "
        "SELECT source_table_id,feishu_record_id,person_id,'matched' FROM tmp_fs_choices WHERE action='matched';",
        "INSERT INTO app_consultant (consultant_no,consultant_name,mobile,remark,status,create_time) "
        f"SELECT CONCAT('FS-',p.feishu_record_id),p.{name},p.{phone},p.{remark},'00',CURRENT_TIMESTAMP "
        f"FROM `{target}` p {choice_join()} WHERE d.action='create';",
        "INSERT INTO app_consultant_feishu_source (source_table_id,source_record_id,consultant_id,match_status) "
        f"SELECT p.source_table_id,p.feishu_record_id,c.consultant_id,'created' FROM `{target}` p {choice_join()} "
        "JOIN app_consultant c ON BINARY c.consultant_no=BINARY CONCAT('FS-',p.feishu_record_id) WHERE d.action='create';",
    ] + finish_identity(identity)
    return d, m


def _orders_sql(tables):
    table = _table(tables, "travel", "预订订单表")
    target = TARGET_TABLES[("travel", "预订订单表")]
    return [
        f"UPDATE `{target}` p JOIN app_goods_order o ON BINARY o.feishu_record_id=BINARY p.feishu_record_id SET p.canonical_table='app_goods_order',p.canonical_id=o.order_id,p.canonical_status='linked',p.canonical_message=NULL WHERE p.canonical_status<>'skipped' AND {record_scope(table)};",
        f"UPDATE `{target}` p LEFT JOIN app_goods_order o ON BINARY o.feishu_record_id=BINARY p.feishu_record_id SET p.canonical_id=NULL,p.canonical_status='unresolved',p.canonical_message='missing app_goods_order' WHERE o.order_id IS NULL AND p.canonical_status<>'skipped' AND {record_scope(table)};",
        f"UPDATE app_feishu_migration_record r JOIN `{target}` p ON BINARY p.feishu_record_id=BINARY r.source_record_id AND BINARY p.source_table_id=BINARY r.source_table_id SET r.merge_status=CASE p.canonical_status WHEN 'skipped' THEN 'skipped' WHEN 'linked' THEN 'merged' ELSE 'conflict' END,r.target_table=p.canonical_table,r.target_id=p.canonical_id,r.merge_message=p.canonical_message WHERE {record_scope(table)};",
    ]


def _income_sql(tables):
    table = _table(tables, "eldercare", "🧾收入明细数据")
    target = TARGET_TABLES[("eldercare", "🧾收入明细数据")]
    field_names = ("销售内容", "充值金额", "消费金额", "余额", "积分", "成交日期", "是否结算", "公司收入", "管家提成", "产品类别", "备注")
    product, charge, purchase, balance, score, trade, settled, company, consultant_income, product_type, remark = (_col(table, name) for name in field_names)
    # 飞书未勾选的复选框省略返回字段；省略不代表第三种结算状态。
    normalized_settlement = f"CASE WHEN p.{settled}=1 THEN 1 ELSE 0 END"
    party_fields = [_q(next(f['field_id'] for f in table['fields'] if f['field_name'] == name))
                    for name in ('客户姓名', '养老管家')]
    ready = "d.unresolved=0"
    customer_id = "IF(d.customers=1,d.customer_id,NULL)"
    consultant_id = "IF(d.consultants=1,d.consultant_id,NULL)"
    equalities = [('charge_amount', charge), ('purchase_amount', purchase), ('balance', balance),
                  ('score', score), ('company_income', company), ('consultant_income', consultant_income)]
    unchanged = ' AND '.join(f"(i.{column} <=> p.{field})" for column, field in equalities)
    unchanged += f" AND (i.trade_date <=> DATE(p.{trade})) AND (i.settlement <=> {normalized_settlement})"
    unchanged += f" AND (BINARY i.product_name <=> BINARY p.{product}) AND (i.customer_id <=> {customer_id}) AND (i.consultant_id <=> {consultant_id})"
    valid = f"({ready} AND i.income_id IS NOT NULL AND {unchanged})"
    m = [
        "DROP TEMPORARY TABLE IF EXISTS tmp_fs_income_existing;",
        "CREATE TEMPORARY TABLE tmp_fs_income_existing AS SELECT income_no FROM app_customer_income;",
        "DROP TEMPORARY TABLE IF EXISTS tmp_fs_income_links;",
        "CREATE TEMPORARY TABLE tmp_fs_income_links AS SELECT p.source_table_id,p.feishu_record_id,"
        f"COUNT(DISTINCT CASE WHEN rel.relation_status='resolved' AND rel.target_business_table='app_customer' AND BINARY rel.source_field_id=BINARY {party_fields[0]} THEN rel.target_business_id END) customers,"
        f"MIN(CASE WHEN rel.relation_status='resolved' AND rel.target_business_table='app_customer' AND BINARY rel.source_field_id=BINARY {party_fields[0]} THEN rel.target_business_id END) customer_id,"
        f"COUNT(DISTINCT CASE WHEN rel.relation_status='resolved' AND rel.target_business_table='app_consultant' AND BINARY rel.source_field_id=BINARY {party_fields[1]} THEN rel.target_business_id END) consultants,"
        f"MIN(CASE WHEN rel.relation_status='resolved' AND rel.target_business_table='app_consultant' AND BINARY rel.source_field_id=BINARY {party_fields[1]} THEN rel.target_business_id END) consultant_id,"
        "SUM(CASE WHEN rel.source_record_id IS NOT NULL AND (rel.relation_status<>'resolved' OR rel.target_business_id IS NULL "
        f"OR (BINARY rel.source_field_id=BINARY {party_fields[0]} AND COALESCE(rel.target_business_table,'')<>'app_customer') "
        f"OR (BINARY rel.source_field_id=BINARY {party_fields[1]} AND COALESCE(rel.target_business_table,'')<>'app_consultant')) THEN 1 ELSE 0 END) unresolved "
        f"FROM `{target}` p LEFT JOIN app_feishu_business_relation rel ON BINARY rel.source_table_id=BINARY p.source_table_id "
        f"AND BINARY rel.source_record_id=BINARY p.feishu_record_id AND BINARY rel.source_field_id IN ({','.join(party_fields)}) "
        f"WHERE p.canonical_status<>'skipped' AND {record_scope(table)} GROUP BY p.source_table_id,p.feishu_record_id;",
        "INSERT INTO app_customer_income (user_id,product_name,income_no,dept_id,charge_amount,purchase_amount,balance,score,trade_date,settlement,company_income,consultant_income,product_type,remark,customer_id,consultant_id,create_by,create_time) "
        f"SELECT 0,p.{product},CONCAT('FS-',p.feishu_record_id),0,p.{charge},p.{purchase},p.{balance},p.{score},DATE(p.{trade}),{normalized_settlement},p.{company},p.{consultant_income},p.{product_type},p.{remark},"
        f"{customer_id},{consultant_id},'feishu',CURRENT_TIMESTAMP "
        f"FROM `{target}` p JOIN tmp_fs_income_links d ON BINARY d.source_table_id=BINARY p.source_table_id AND BINARY d.feishu_record_id=BINARY p.feishu_record_id "
        f"WHERE {ready} AND NOT EXISTS (SELECT 1 FROM tmp_fs_income_existing i WHERE BINARY i.income_no=BINARY CONCAT('FS-',p.feishu_record_id));",
        f"UPDATE `{target}` p JOIN tmp_fs_income_links d ON BINARY d.source_table_id=BINARY p.source_table_id AND BINARY d.feishu_record_id=BINARY p.feishu_record_id "
        "LEFT JOIN app_customer_income i ON BINARY i.income_no=BINARY CONCAT('FS-',p.feishu_record_id) "
        f"SET p.canonical_table='app_customer_income',p.canonical_id=IF({valid},i.income_id,NULL),"
        f"p.canonical_status=IF({valid},'linked','needs_review'),p.canonical_message=IF({valid},"
        "CASE WHEN d.consultants>1 THEN 'shared_commission_unallocated' WHEN d.customers>1 THEN 'multiple_customers_preserved' "
        "WHEN d.consultants=0 OR d.customers=0 THEN 'source_party_not_specified' ELSE NULL END,'income_relation_or_value_requires_review');",
        f"UPDATE app_feishu_migration_record r JOIN `{target}` p ON BINARY p.source_table_id=BINARY r.source_table_id AND BINARY p.feishu_record_id=BINARY r.source_record_id "
        f"SET r.merge_status=CASE p.canonical_status WHEN 'skipped' THEN 'skipped' WHEN 'linked' THEN 'merged' ELSE 'conflict' END,r.target_table=p.canonical_table,r.target_id=p.canonical_id,r.merge_message=p.canonical_message WHERE {record_scope(table)};",
        "DROP TEMPORARY TABLE IF EXISTS tmp_fs_income_links;",
        "DROP TEMPORARY TABLE IF EXISTS tmp_fs_income_existing;",
    ]
    return m


def _activity_sql(tables):
    table = _table(tables, "eldercare", "活动计划表")
    target = TARGET_TABLES[("eldercare", "活动计划表")]
    return [
        f"UPDATE `{target}` p SET canonical_table=NULL,canonical_id=NULL,canonical_status='skipped',"
        f"canonical_message=COALESCE(canonical_message,'activity_execution_source_only') WHERE {record_scope(table)};",
        f"UPDATE app_feishu_migration_record r JOIN `{target}` p ON BINARY p.source_table_id=BINARY r.source_table_id "
        "AND BINARY p.feishu_record_id=BINARY r.source_record_id SET r.merge_status='skipped',"
        f"r.target_table=NULL,r.target_id=NULL,r.merge_message=p.canonical_message WHERE {record_scope(table)};",
    ]


def build_canonical_sql(tables):
    customer_ddl, customer_dml = _customer_sql(tables)
    consultant_ddl, consultant_dml = _consultant_sql(tables)
    relation_resolve = []
    for base, table in tables:
        target = TARGET_TABLES[(base["key"], table["name"])]
        relation_resolve.append(
            f"UPDATE app_feishu_business_relation rel JOIN `{target}` p ON BINARY p.feishu_record_id=BINARY rel.target_source_record_id "
            "SET rel.target_business_table=p.canonical_table,rel.target_business_id=IF(p.canonical_status='linked',p.canonical_id,NULL),"
            "rel.relation_status=IF(p.canonical_status='linked' AND p.canonical_id IS NOT NULL,'resolved','unresolved'),"
            "rel.relation_message=IF(p.canonical_status='linked' AND p.canonical_id IS NOT NULL,NULL,'target_requires_review') "
            f"WHERE {record_scope(table, 'rel', True)} AND rel.relation_status<>'superseded' "
            "AND COALESCE(rel.relation_message,'')<>'owner_user_unique_consultant_name';"
        )
    d = customer_ddl + consultant_ddl
    m = _self_links(tables) + customer_dml + consultant_dml + _orders_sql(tables)
    m += relation_resolve + _income_sql(tables) + _activity_sql(tables) + relation_resolve
    m += owner_relations_sql(_table(tables, 'travel', '预订订单表'))
    m.append(
        "UPDATE app_feishu_business_relation SET relation_message='target record is absent from Feishu export' "
        "WHERE relation_status='unresolved' AND relation_message='target not resolved';"
    )
    return d, m
