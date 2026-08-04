SET NAMES utf8mb4;

DROP TABLE IF EXISTS app_goods_related;
DROP TABLE IF EXISTS app_goods_education_ext;
DROP TABLE IF EXISTS app_goods_sku_option;
DROP TABLE IF EXISTS app_goods_sku;
DROP TABLE IF EXISTS app_goods_sku_data;

CREATE TABLE app_goods_related (
    id bigint NOT NULL AUTO_INCREMENT,
    goods_id bigint NOT NULL,
    section_id varchar(100) NOT NULL,
    section_name varchar(100) NOT NULL,
    content text,
    sort_order int DEFAULT 0,
    min_content_length int DEFAULT 500,
    create_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_goods_education_ext (
    ext_id bigint unsigned NOT NULL AUTO_INCREMENT,
    goods_id bigint unsigned NOT NULL,
    course_time varchar(100) DEFAULT '',
    course_place varchar(255) DEFAULT '',
    teacher_name varchar(100) DEFAULT '',
    lesson_count int unsigned DEFAULT NULL,
    class_size_max int unsigned DEFAULT NULL,
    class_size_min int unsigned DEFAULT NULL,
    start_date date DEFAULT NULL,
    signup_start date DEFAULT NULL,
    signup_end date DEFAULT NULL,
    material_note varchar(255) DEFAULT '',
    consult_phone varchar(30) DEFAULT '',
    create_time datetime DEFAULT CURRENT_TIMESTAMP,
    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ext_id),
    UNIQUE KEY uk_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_goods_sku (
    sku_id bigint NOT NULL AUTO_INCREMENT,
    goods_id bigint NOT NULL,
    sku_name varchar(100) NOT NULL,
    sku_type varchar(3) NOT NULL DEFAULT '',
    sku_code varchar(20) NOT NULL DEFAULT '',
    par_sku_id bigint NOT NULL DEFAULT 0,
    sort_order int DEFAULT 0,
    status char(1) DEFAULT '1',
    valid_time datetime DEFAULT NULL,
    invalid_time datetime DEFAULT NULL,
    create_time datetime DEFAULT CURRENT_TIMESTAMP,
    stock int DEFAULT NULL,
    stock_unit varchar(20) DEFAULT NULL,
    sale_num int DEFAULT NULL,
    price decimal(10,2) DEFAULT NULL,
    sale_price decimal(10,2) DEFAULT NULL,
    PRIMARY KEY (sku_id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_goods_sku_option (
    option_id bigint unsigned NOT NULL AUTO_INCREMENT,
    goods_id bigint unsigned DEFAULT 0,
    sku_id bigint DEFAULT NULL,
    option_name varchar(255) NOT NULL DEFAULT '',
    option_param varchar(255) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    status char(1) DEFAULT '0',
    option_type varchar(3) NOT NULL DEFAULT '',
    option_value varchar(1000) NOT NULL DEFAULT '',
    option_value_unit varchar(20) NOT NULL DEFAULT '',
    option_sort int unsigned DEFAULT 0,
    sku_seq_no int DEFAULT 0,
    PRIMARY KEY (option_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_goods_sku_data (
    data_id bigint unsigned NOT NULL AUTO_INCREMENT,
    goods_id bigint unsigned DEFAULT 0,
    sku_ids varchar(255) DEFAULT NULL,
    option_ids varchar(255) DEFAULT NULL,
    data_values varchar(255) DEFAULT NULL,
    data_price decimal(10,2) DEFAULT NULL,
    data_image varchar(255) DEFAULT '',
    data_stock bigint DEFAULT NULL,
    remark varchar(500) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    status char(1) DEFAULT '0',
    PRIMARY KEY (data_id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELETE FROM app_goods;
DELETE FROM app_goods_category;
DELETE FROM app_goods_cart;
DELETE FROM app_goods_collect;
DELETE FROM app_goods_comment;
