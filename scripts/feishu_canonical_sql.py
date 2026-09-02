"""Build canonical business-domain SQL for the normalized Feishu tables."""

from feishu_structured_model import TARGET_TABLES, column_name


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
        statements.append(
            f"UPDATE `{target}` SET canonical_table={_q(target)},canonical_id=business_id,"
            "canonical_status='linked',canonical_message=NULL;"
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
    m = [
        "DROP TEMPORARY TABLE IF EXISTS tmp_feishu_customer_match;",
        "CREATE TEMPORARY TABLE tmp_feishu_customer_match AS SELECT customer_id,customer_no,link_mobile FROM app_customer WHERE customer_no IS NULL OR customer_no NOT LIKE 'FS-%';",
        "CREATE INDEX idx_tmp_feishu_customer_no ON tmp_feishu_customer_match(customer_no);",
        "CREATE INDEX idx_tmp_feishu_customer_mobile ON tmp_feishu_customer_match(link_mobile);",
    ]
    for table, target, business_line, key_name, db_key in (
        (travel, travel_target, "travel", "客户编号", "customer_no"),
        (elder, elder_target, "eldercare", "电话", "link_mobile"),
    ):
        source_id = table["table_id"]
        key_col = _col(table, key_name)
        source_table = "tmp_feishu_customer_match"
        candidates = (
            f"SELECT {db_key} match_key,MIN(customer_id) customer_id,COUNT(*) candidate_count "
            f"FROM {source_table} WHERE {db_key} IS NOT NULL AND {db_key}<>'' GROUP BY {db_key}"
        )
        if business_line == "travel":
            candidates = (
                "SELECT CAST(customer_no AS DECIMAL(20,4)) match_key,MIN(customer_id) customer_id,COUNT(*) candidate_count "
                f"FROM {source_table} WHERE customer_no REGEXP '^[0-9]+([.][0-9]+)?$' GROUP BY CAST(customer_no AS DECIMAL(20,4))"
            )
        join_key = f"c.match_key=p.{key_col}" if business_line == "travel" else f"BINARY c.match_key=BINARY p.{key_col}"
        review_key = f"k.match_key=p.{key_col}" if business_line == "travel" else f"BINARY k.match_key=BINARY p.{key_col}"
        m.append(
            "INSERT INTO app_customer_feishu_source (source_table_id,source_record_id,customer_id,business_line,match_method,match_status,match_message) "
            f"SELECT p.source_table_id,p.feishu_record_id,c.customer_id,{_q(business_line)},{_q(db_key)},'matched',NULL "
            f"FROM `{target}` p JOIN ({candidates}) c ON {join_key} AND c.candidate_count=1 "
            "ON DUPLICATE KEY UPDATE customer_id=VALUES(customer_id),match_method=VALUES(match_method),match_status=VALUES(match_status),match_message=NULL;"
        )
        name = _col(table, "客户名称")
        phone = _col(table, "联系方式" if business_line == "travel" else "电话")
        source = _col(table, "来源" if business_line == "travel" else "客户获取渠道")
        label = _col(table, "客户状态" if business_line == "travel" else "客户标签")
        sign_time = _col(table, "日期" if business_line == "travel" else "登记日期")
        m.append(
            "INSERT INTO app_customer (customer_name,customer_no,link_mobile,acquisition_channel,customer_label,sign_time,status,create_by,create_time,del_flag) "
            f"SELECT p.{name},CONCAT('FS-',p.feishu_record_id),p.{phone},p.{source},p.{label},p.{sign_time},'0','feishu',CURRENT_TIMESTAMP,'0' "
            f"FROM `{target}` p LEFT JOIN app_customer_feishu_source s ON s.source_table_id=p.source_table_id AND s.source_record_id=p.feishu_record_id "
            "WHERE s.source_record_id IS NULL;"
        )
        m.append(
            "INSERT INTO app_customer_feishu_source (source_table_id,source_record_id,customer_id,business_line,match_method,match_status,match_message) "
            f"SELECT p.source_table_id,p.feishu_record_id,c.customer_id,{_q(business_line)},'created',"
            f"IF(IFNULL(k.candidate_count,0)>1,'needs_review','created'),IF(IFNULL(k.candidate_count,0)>1,'multiple existing customers matched source key',NULL) "
            f"FROM `{target}` p JOIN app_customer c ON BINARY c.customer_no=BINARY CONCAT('FS-',p.feishu_record_id) "
            f"LEFT JOIN ({candidates}) k ON {review_key} "
            "ON DUPLICATE KEY UPDATE customer_id=VALUES(customer_id),match_status=VALUES(match_status),match_message=VALUES(match_message);"
        )
        m.append(
            f"UPDATE `{target}` p JOIN app_customer_feishu_source s ON s.source_table_id=p.source_table_id AND s.source_record_id=p.feishu_record_id "
            "SET p.canonical_table='app_customer',p.canonical_id=s.customer_id,p.canonical_status=IF(s.match_status='needs_review','needs_review','linked'),p.canonical_message=s.match_message;"
        )
        m.append(
            f"UPDATE app_feishu_migration_record r JOIN `{target}` p ON p.feishu_record_id=r.source_record_id "
            "SET r.merge_status='merged',r.target_table='app_customer',r.target_id=p.canonical_id,r.merge_message=p.canonical_message "
            f"WHERE r.source_table_id={_q(source_id)};"
        )
    m.append("DROP TEMPORARY TABLE IF EXISTS tmp_feishu_customer_match;")
    return d, m


def _consultant_sql(tables):
    table = _table(tables, "eldercare", "养老顾问列表")
    target = TARGET_TABLES[("eldercare", "养老顾问列表")]
    name, phone, remark = (_col(table, value) for value in ("养老顾问", "电话", "备注"))
    d = [
        "CREATE TABLE IF NOT EXISTS app_consultant_feishu_source (source_table_id varchar(64) NOT NULL,source_record_id varchar(64) NOT NULL,consultant_id bigint unsigned NOT NULL,match_status varchar(16) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(source_table_id,source_record_id),KEY idx_consultant_feishu_source_consultant(consultant_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    ]
    candidates = "SELECT mobile,MIN(consultant_id) consultant_id,COUNT(*) candidate_count FROM app_consultant WHERE mobile IS NOT NULL AND mobile<>'' GROUP BY mobile"
    m = [
        "INSERT INTO app_consultant_feishu_source (source_table_id,source_record_id,consultant_id,match_status) "
        f"SELECT p.source_table_id,p.feishu_record_id,c.consultant_id,'matched' FROM `{target}` p JOIN ({candidates}) c ON BINARY c.mobile=BINARY p.{phone} AND c.candidate_count=1 "
        "ON DUPLICATE KEY UPDATE consultant_id=VALUES(consultant_id),match_status=VALUES(match_status);",
        "INSERT INTO app_consultant (consultant_no,consultant_name,mobile,remark,status,create_time) "
        f"SELECT CONCAT('FS-',p.feishu_record_id),p.{name},p.{phone},p.{remark},'0',CURRENT_TIMESTAMP FROM `{target}` p "
        "LEFT JOIN app_consultant_feishu_source s ON s.source_table_id=p.source_table_id AND s.source_record_id=p.feishu_record_id WHERE s.source_record_id IS NULL;",
        "INSERT INTO app_consultant_feishu_source (source_table_id,source_record_id,consultant_id,match_status) "
        f"SELECT p.source_table_id,p.feishu_record_id,c.consultant_id,'created' FROM `{target}` p JOIN app_consultant c ON BINARY c.consultant_no=BINARY CONCAT('FS-',p.feishu_record_id) "
        "ON DUPLICATE KEY UPDATE consultant_id=VALUES(consultant_id),match_status=VALUES(match_status);",
        f"UPDATE `{target}` p JOIN app_consultant_feishu_source s ON s.source_table_id=p.source_table_id AND s.source_record_id=p.feishu_record_id SET p.canonical_table='app_consultant',p.canonical_id=s.consultant_id,p.canonical_status='linked',p.canonical_message=NULL;",
        f"UPDATE app_feishu_migration_record r JOIN `{target}` p ON p.feishu_record_id=r.source_record_id SET r.merge_status='merged',r.target_table='app_consultant',r.target_id=p.canonical_id,r.merge_message=NULL WHERE r.source_table_id={_q(table['table_id'])};",
    ]
    return d, m


def _orders_sql(tables):
    table = _table(tables, "travel", "预订订单表")
    target = TARGET_TABLES[("travel", "预订订单表")]
    return [
        f"UPDATE `{target}` p JOIN app_goods_order o ON BINARY o.feishu_record_id=BINARY p.feishu_record_id SET p.canonical_table='app_goods_order',p.canonical_id=o.order_id,p.canonical_status='linked',p.canonical_message=NULL;",
        f"UPDATE `{target}` p LEFT JOIN app_goods_order o ON BINARY o.feishu_record_id=BINARY p.feishu_record_id SET p.canonical_status='unresolved',p.canonical_message='missing app_goods_order' WHERE o.order_id IS NULL;",
        f"UPDATE app_feishu_migration_record r JOIN `{target}` p ON p.feishu_record_id=r.source_record_id SET r.merge_status=IF(p.canonical_id IS NULL,'conflict','merged'),r.target_table=p.canonical_table,r.target_id=p.canonical_id,r.merge_message=p.canonical_message WHERE r.source_table_id={_q(table['table_id'])};",
    ]


def _income_sql(tables):
    table = _table(tables, "eldercare", "🧾收入明细数据")
    target = TARGET_TABLES[("eldercare", "🧾收入明细数据")]
    field_names = ("销售内容", "充值金额", "消费金额", "余额", "积分", "成交日期", "是否结算", "公司收入", "管家提成", "产品类别", "备注")
    product, charge, purchase, balance, score, trade, settled, company, consultant_income, product_type, remark = (_col(table, name) for name in field_names)
    m = [
        "INSERT INTO app_customer_income (user_id,product_name,income_no,dept_id,charge_amount,purchase_amount,balance,score,trade_date,settlement,company_income,consultant_income,product_type,remark,customer_id,consultant_id,create_by,create_time) "
        f"SELECT 0,p.{product},CONCAT('FS-',p.feishu_record_id),0,p.{charge},p.{purchase},p.{balance},p.{score},DATE(p.{trade}),IF(p.{settled}=1,1,0),p.{company},p.{consultant_income},p.{product_type},p.{remark},"
        "(SELECT MIN(rel.target_business_id) FROM app_feishu_business_relation rel WHERE rel.source_table_id=p.source_table_id AND rel.source_record_id=p.feishu_record_id AND rel.target_business_table='app_customer'),"
        "(SELECT MIN(rel.target_business_id) FROM app_feishu_business_relation rel WHERE rel.source_table_id=p.source_table_id AND rel.source_record_id=p.feishu_record_id AND rel.target_business_table='app_consultant'),'feishu',CURRENT_TIMESTAMP "
        f"FROM `{target}` p WHERE NOT EXISTS (SELECT 1 FROM app_customer_income i WHERE BINARY i.income_no=BINARY CONCAT('FS-',p.feishu_record_id));",
        f"UPDATE `{target}` p JOIN app_customer_income i ON BINARY i.income_no=BINARY CONCAT('FS-',p.feishu_record_id) SET p.canonical_table='app_customer_income',p.canonical_id=i.income_id,p.canonical_status='linked',p.canonical_message=NULL;",
        f"UPDATE app_feishu_migration_record r JOIN `{target}` p ON p.feishu_record_id=r.source_record_id SET r.merge_status='merged',r.target_table='app_customer_income',r.target_id=p.canonical_id,r.merge_message=NULL WHERE r.source_table_id={_q(table['table_id'])};",
    ]
    return m


def _activity_sql(tables):
    table = _table(tables, "eldercare", "活动计划表")
    target = TARGET_TABLES[("eldercare", "活动计划表")]
    date, address = (_col(table, name) for name in ("日期", "地址"))
    return [
        "INSERT INTO app_activity (activity_name,address,description,activity_time,create_time,update_time,status,is_free,price,vip_price) "
        f"SELECT CONCAT('飞书活动-',p.feishu_record_id),p.{address},'飞书活动计划回填',DATE_FORMAT(p.{date},'%Y-%m-%d %H:%i:%s'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'0',1,0,0 FROM `{target}` p "
        "WHERE NOT EXISTS (SELECT 1 FROM app_activity a WHERE BINARY a.activity_name=BINARY CONCAT('飞书活动-',p.feishu_record_id));",
        f"UPDATE `{target}` p JOIN app_activity a ON BINARY a.activity_name=BINARY CONCAT('飞书活动-',p.feishu_record_id) SET p.canonical_table='app_activity',p.canonical_id=a.activity_id,p.canonical_status='linked',p.canonical_message=NULL;",
        f"UPDATE app_feishu_migration_record r JOIN `{target}` p ON p.feishu_record_id=r.source_record_id SET r.merge_status='merged',r.target_table='app_activity',r.target_id=p.canonical_id,r.merge_message=NULL WHERE r.source_table_id={_q(table['table_id'])};",
    ]


def build_canonical_sql(tables):
    customer_ddl, customer_dml = _customer_sql(tables)
    consultant_ddl, consultant_dml = _consultant_sql(tables)
    relation_resolve = []
    for base, table in tables:
        target = TARGET_TABLES[(base["key"], table["name"])]
        relation_resolve.append(
            f"UPDATE app_feishu_business_relation rel JOIN `{target}` p ON BINARY p.feishu_record_id=BINARY rel.target_source_record_id "
            "SET rel.target_business_table=p.canonical_table,rel.target_business_id=p.canonical_id,rel.relation_status='resolved',rel.relation_message=NULL "
            f"WHERE rel.target_source_table_id={_q(table['table_id'])} AND p.canonical_id IS NOT NULL;"
        )
    d = customer_ddl + consultant_ddl
    m = _self_links(tables) + customer_dml + consultant_dml + _orders_sql(tables)
    m += relation_resolve + _income_sql(tables) + _activity_sql(tables) + relation_resolve
    return d, m
