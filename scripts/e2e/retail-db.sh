#!/usr/bin/env bash
set -euo pipefail

# Only the dedicated local test database is writable by this script.
RETAIL_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RETAIL_MYSQL=/opt/homebrew/opt/mysql@8.4/bin/mysql
RETAIL_DUMP=/opt/homebrew/opt/mysql@8.4/bin/mysqldump
RETAIL_DATABASE=yixianghui_retail_test

"${RETAIL_MYSQL}" -h127.0.0.1 -uroot -e "CREATE DATABASE IF NOT EXISTS ${RETAIL_DATABASE} CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
RETAIL_TABLES="$("${RETAIL_MYSQL}" -h127.0.0.1 -uroot -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${RETAIL_DATABASE}'")"
if [[ "${RETAIL_TABLES}" == "0" ]]; then
  "${RETAIL_DUMP}" -h127.0.0.1 -uroot --no-data --skip-add-drop-table yixianghui_e2e |
    "${RETAIL_MYSQL}" -h127.0.0.1 -uroot "${RETAIL_DATABASE}"
fi

has_column() {
  "${RETAIL_MYSQL}" -h127.0.0.1 -uroot "${RETAIL_DATABASE}" -Nse "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='$1' AND column_name='$2'"
}

if [[ "$(has_column app_goods_coupon channel_code)" == "0" ]]; then
  "${RETAIL_MYSQL}" -h127.0.0.1 -uroot "${RETAIL_DATABASE}" < "${RETAIL_ROOT}/sql/distribution-coupon-channel.sql"
fi
if [[ "$(has_column app_goods_order order_origin)" == "0" ]]; then
  "${RETAIL_MYSQL}" -h127.0.0.1 -uroot "${RETAIL_DATABASE}" < "${RETAIL_ROOT}/sql/app_goods_order_feishu_history.sql"
fi

ensure_detail_column() {
  if [[ "$(has_column app_goods_order_detail "$1")" == "0" ]]; then
    "${RETAIL_MYSQL}" -h127.0.0.1 -uroot "${RETAIL_DATABASE}" -e "ALTER TABLE app_goods_order_detail ADD COLUMN $1 $2 NULL"
  fi
}
ensure_detail_column sku_id BIGINT
ensure_detail_column self_sku_id BIGINT
ensure_detail_column sku_seq_no INT
ensure_detail_column self_goods_count INT
ensure_detail_column inter_count INT
ensure_detail_column order_start_date DATE
ensure_detail_column order_end_date DATE

"${RETAIL_MYSQL}" -h127.0.0.1 -uroot "${RETAIL_DATABASE}" < "${RETAIL_ROOT}/sql/supplier_retail_checkout.sql"
"${RETAIL_MYSQL}" -h127.0.0.1 -uroot "${RETAIL_DATABASE}" -e "ALTER TABLE sys_user ENGINE=InnoDB"
echo 'Local retail schema ready. Run RETAIL_DB_TEST=1 mvn -pl lankong-admin -am test -Dtest=RetailCheckoutDatabaseTest -Dsurefire.failIfNoSpecifiedTests=false'
