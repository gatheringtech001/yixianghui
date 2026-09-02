#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RUNTIME_DIR="${REPO_ROOT}/.e2e-runtime"
BACKEND_PID_FILE="${RUNTIME_DIR}/backend.pid"
BACKEND_LOG="${RUNTIME_DIR}/backend.log"
BACKEND_PORT="${E2E_BACKEND_PORT:-18080}"
REDIS_PORT="${E2E_REDIS_PORT:-16379}"
MYSQL_BIN="${MYSQL_BIN:-/opt/homebrew/opt/mysql@8.4/bin/mysql}"
REDIS_SERVER_BIN="${REDIS_SERVER_BIN:-/opt/homebrew/opt/redis/bin/redis-server}"
REDIS_CLI_BIN="${REDIS_CLI_BIN:-/opt/homebrew/bin/redis-cli}"
MAVEN_BIN="${MAVEN_BIN:-/opt/homebrew/bin/mvn}"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home}"
JAVA_BIN="${JAVA_HOME}/bin/java"
APP_JAR="${REPO_ROOT}/lankong-admin/target/lankong-admin.jar"
CURL_BIN="${CURL_BIN:-/usr/bin/curl}"
FILE_BIN="${FILE_BIN:-/usr/bin/file}"
GREP_BIN="${GREP_BIN:-/usr/bin/grep}"
SORT_BIN="${SORT_BIN:-/usr/bin/sort}"
E2E_ASSET_BASE_URL="${E2E_ASSET_BASE_URL:-https://shzxj.lk01.cn/api}"

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:/opt/homebrew/bin:/usr/bin:/bin"

require_file() {
  if [[ ! -f "$1" ]]; then
    echo "Required file is missing: $1" >&2
    exit 1
  fi
}

require_executable() {
  if [[ ! -x "$1" ]]; then
    echo "Required executable is missing: $1" >&2
    exit 1
  fi
}

validate_identifier() {
  if [[ ! "$1" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "Invalid database identifier: $1" >&2
    exit 1
  fi
}

backend_running() {
  [[ -f "${BACKEND_PID_FILE}" ]] || return 1
  local pid
  pid="$(<"${BACKEND_PID_FILE}")"
  kill -0 "${pid}" 2>/dev/null
}

start_mysql() {
  if ! "${MYSQL_BIN}" -uroot -e "SELECT 1" >/dev/null 2>&1; then
    brew services start mysql@8.4 >/dev/null
  fi
}

start_redis() {
  if "${REDIS_CLI_BIN}" -p "${REDIS_PORT}" ping 2>/dev/null | grep -q PONG; then
    return
  fi
  "${REDIS_SERVER_BIN}" \
    --bind 127.0.0.1 \
    --port "${REDIS_PORT}" \
    --daemonize yes \
    --save "" \
    --appendonly no
}

setup_database() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  local dump="${REPO_ROOT}/sql/lk-shzxj-re.sql"
  require_file "${dump}"
  validate_identifier "${database}"
  validate_identifier "${username}"
  "${MYSQL_BIN}" -uroot -e "CREATE DATABASE IF NOT EXISTS \`${database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; CREATE USER IF NOT EXISTS '${username}'@'localhost' IDENTIFIED BY ''; GRANT ALL PRIVILEGES ON \`${database}\`.* TO '${username}'@'localhost'; FLUSH PRIVILEGES;"
  local table_count
  table_count="$("${MYSQL_BIN}" -u"${username}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${database}'")"
  if [[ "${table_count}" == "0" ]]; then
    "${MYSQL_BIN}" -u"${username}" "${database}" < "${dump}"
  fi
}

apply_goods_order_schema() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  local migration="${REPO_ROOT}/sql/app_goods_order_booking_fields.sql"
  local travel_status_migration="${REPO_ROOT}/sql/app_goods_order_travel_status.sql"
  local expected_column_count=4
  local actual_column_count
  local history_column_count
  local history_migration="${REPO_ROOT}/sql/app_goods_order_feishu_history.sql"
  require_file "${migration}"
  require_file "${travel_status_migration}"
  validate_identifier "${database}"
  validate_identifier "${username}"
  "${MYSQL_BIN}" -u"${username}" "${database}" < "${travel_status_migration}"
  "${MYSQL_BIN}" -u"${username}" "${database}" < "${migration}"
  actual_column_count="$("${MYSQL_BIN}" -u"${username}" -Nse "
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = '${database}'
      AND table_name = 'app_goods_order'
      AND column_name IN (
        'check_in_date',
        'check_out_date',
        'contact_name',
        'contact_phone'
      );
  ")"
  if [[ "${actual_column_count}" != "${expected_column_count}" ]]; then
    echo "Goods order schema verification failed: expected ${expected_column_count} booking columns, found ${actual_column_count}." >&2
    exit 1
  fi
  history_column_count="$("${MYSQL_BIN}" -u"${username}" -Nse "
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = '${database}' AND table_name = 'app_goods_order'
      AND column_name IN ('order_origin','feishu_record_id','feishu_order_no','channel',
        'travel_customer_record_id','travel_base_record_id','travel_base_name','room_type',
        'room_count','traveler_count','service_owner','service_remark','source_fields_json');
  ")"
  if [[ "${history_column_count}" == "0" ]]; then
    require_file "${history_migration}"
    "${MYSQL_BIN}" -u"${username}" "${database}" < "${history_migration}"
  elif [[ "${history_column_count}" != "13" ]]; then
    echo "Feishu history order schema is partially applied (${history_column_count}/13 columns)." >&2
    exit 1
  fi
}

get_activity_column_count() {
  local database="$1"
  local username="$2"
  local column_names="$3"
  "${MYSQL_BIN}" -u"${username}" -Nse "
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = '${database}'
      AND table_name = 'app_activity'
      AND column_name IN (${column_names});
  "
}

apply_activity_schema() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  local dept_migration="${REPO_ROOT}/sql/step3-activity-dept-id-fix.sql"
  local end_time_migration="${REPO_ROOT}/sql/app_activity_end_time.sql"
  local fee_migration="${REPO_ROOT}/sql/step3-activity-pay-phase1.sql"
  local column_count
  local fee_column_count
  require_file "${dept_migration}"
  require_file "${end_time_migration}"
  require_file "${fee_migration}"
  validate_identifier "${database}"
  validate_identifier "${username}"

  column_count="$(get_activity_column_count "${database}" "${username}" "'dept_id'")"
  if [[ "${column_count}" == "0" ]]; then
    "${MYSQL_BIN}" -u"${username}" "${database}" < "${dept_migration}"
  fi

  column_count="$(get_activity_column_count "${database}" "${username}" "'activity_end_time'")"
  if [[ "${column_count}" == "0" ]]; then
    "${MYSQL_BIN}" -u"${username}" "${database}" < "${end_time_migration}"
  fi

  fee_column_count="$(get_activity_column_count \
    "${database}" "${username}" "'is_free', 'price', 'vip_price'")"
  if [[ "${fee_column_count}" == "0" ]]; then
    "${MYSQL_BIN}" -u"${username}" "${database}" < "${fee_migration}"
  elif [[ "${fee_column_count}" != "3" ]]; then
    echo "Activity fee schema is partially applied (${fee_column_count}/3 columns)." >&2
    exit 1
  fi

  column_count="$(get_activity_column_count \
    "${database}" "${username}" \
    "'dept_id', 'activity_end_time', 'is_free', 'price', 'vip_price'")"
  if [[ "${column_count}" != "5" ]]; then
    echo "Activity schema verification failed: expected 5 E2E columns, found ${column_count}." >&2
    exit 1
  fi
}

configure_service_availability() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  validate_identifier "${database}"
  validate_identifier "${username}"
  "${MYSQL_BIN}" -u"${username}" "${database}" -e "
    UPDATE sys_menu
    SET status = '1'
    WHERE perms IN (
      'system:app_goods_comment:list',
      'system:app_goods_sku:list',
      'system:app_goods_collect:list',
      'system:app_activity:list',
      'system:app_activity_order:list'
    );
    UPDATE sys_menu
    SET status = '0'
    WHERE perms = 'system:app_goods_order:list';
  "
}

apply_e2e_asset_fixtures() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  local fixture="${REPO_ROOT}/sql/e2e-production-assets.sql"
  local position_count
  local content_count
  require_file "${fixture}"
  validate_identifier "${database}"
  validate_identifier "${username}"
  "${MYSQL_BIN}" -u"${username}" "${database}" < "${fixture}"
  position_count="$("${MYSQL_BIN}" -u"${username}" -Nse "
    SELECT COUNT(*)
    FROM ${database}.app_ad_position
    WHERE position_code IN (
      'mnp_brand_logo',
      'mnp_home_housekeeper',
      'mnp_profile_steward'
    ) OR position_id = 7;
  ")"
  content_count="$("${MYSQL_BIN}" -u"${username}" -Nse "
    SELECT COUNT(*)
    FROM ${database}.app_ad_content
    WHERE content_id IN (19, 20, 21, 22, 24, 25, 26);
  ")"
  if [[ "${position_count}" != "4" || "${content_count}" != "7" ]]; then
    echo "E2E asset fixture verification failed." >&2
    exit 1
  fi
}

apply_e2e_product_fixture() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  local fixture="${REPO_ROOT}/sql/e2e-production-products.sql"
  local counts
  require_file "${fixture}"
  validate_identifier "${database}"
  validate_identifier "${username}"
  "${MYSQL_BIN}" -u"${username}" "${database}" < "${fixture}"
  counts="$("${MYSQL_BIN}" -u"${username}" -Nse "
    SELECT CONCAT(
      (SELECT COUNT(*) FROM ${database}.app_goods), ',',
      (SELECT COUNT(*) FROM ${database}.app_goods WHERE goods_type = 'education'), ',',
      (SELECT COUNT(*) FROM ${database}.app_goods_related), ',',
      (SELECT COUNT(*) FROM ${database}.app_goods_sku), ',',
      (SELECT COUNT(*) FROM ${database}.app_goods_sku_option), ',',
      (SELECT COUNT(*) FROM ${database}.app_goods_sku_data)
    );
  ")"
  if [[ "${counts}" != "4,1,15,19,83,0" ]]; then
    echo "E2E product fixture verification failed: ${counts}." >&2
    exit 1
  fi
}

install_e2e_bundled_assets() {
  local upload_dir="${E2E_UPLOAD_DIR:-/tmp/yixianghui-e2e/uploads}"
  local source="${REPO_ROOT}/shop-mnp/static/home-design/brand-logo-transparent.png"
  local target="${upload_dir}/e2e/brand-logo-transparent.png"
  local asset
  require_file "${source}"
  mkdir -p "$(dirname "${target}")"
  cp "${source}" "${target}"
  for asset in \
    city-kunming-landmark.jpg \
    city-jianshui-landmark.jpg \
    city-tengchong-landmark.jpg \
    city-qujing-landmark.jpg \
    city-dali-landmark.jpg; do
    source="${REPO_ROOT}/e2e/fixtures/assets/${asset}"
    target="${upload_dir}/e2e/${asset}"
    require_file "${source}"
    cp "${source}" "${target}"
  done
}

load_e2e_asset_values() {
  local database="$1"
  local username="$2"
  "${MYSQL_BIN}" -u"${username}" -Nse "
    SELECT CONVERT(ad_image USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_ad_content WHERE ad_image <> ''
    UNION SELECT CONVERT(ad_content USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_ad_content WHERE ad_content LIKE '%/profile/%'
    UNION SELECT CONVERT(activity_cover USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_activity WHERE activity_cover <> ''
    UNION SELECT CONVERT(content USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_activity WHERE content LIKE '%/profile/%'
    UNION SELECT CONVERT(content USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_article WHERE content LIKE '%/profile/%'
    UNION SELECT CONVERT(goods_cover USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods WHERE goods_cover <> ''
    UNION SELECT CONVERT(goods_images USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods WHERE goods_images <> ''
    UNION SELECT CONVERT(content USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods WHERE content LIKE '%/profile/%'
    UNION SELECT CONVERT(content USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods_related WHERE content LIKE '%/profile/%'
    UNION SELECT CONVERT(coupon_content USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods_coupon WHERE coupon_content LIKE '%/profile/%'
    UNION SELECT CONVERT(data_image USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods_sku_data WHERE data_image <> ''
    UNION SELECT CONVERT(content USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_single_page WHERE content LIKE '%/profile/%'
    UNION SELECT CONVERT(qrcode_url USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_user_info WHERE qrcode_url <> ''
    UNION SELECT CONVERT(notice_content USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.sys_notice WHERE notice_content LIKE '%/profile/%';
  "
}

extract_e2e_asset_paths() {
  LC_ALL=C "${GREP_BIN}" -aoE "/profile/[^\"'<>()[:space:],;?#]+" | "${SORT_BIN}" -u
}

is_image_file() {
  [[ -s "$1" ]] &&
    "${FILE_BIN}" --brief --mime-type "$1" | grep -q '^image/'
}

sync_e2e_asset() {
  local asset_path="$1"
  local upload_dir="$2"
  local target="${upload_dir}${asset_path#/profile}"
  if is_image_file "${target}"; then
    return 0
  fi
  rm -f "${target}"
  mkdir -p "$(dirname "${target}")"
  if ! "${CURL_BIN}" --fail --silent --show-error --location \
    --connect-timeout 10 --max-time 60 \
    --output "${target}.tmp" "${E2E_ASSET_BASE_URL}${asset_path}"; then
    rm -f "${target}.tmp"
    echo "E2E image asset is unavailable from source: ${asset_path}" >&2
    return 2
  fi
  if ! is_image_file "${target}.tmp"; then
    rm -f "${target}.tmp"
    echo "E2E asset source did not return an image: ${asset_path}" >&2
    return 2
  fi
  mv "${target}.tmp" "${target}"
  return 1
}

sync_e2e_assets() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  local upload_dir="${E2E_UPLOAD_DIR:-/tmp/yixianghui-e2e/uploads}"
  local asset_values
  local asset_path
  local asset_status
  local downloaded_assets=0
  local unavailable_assets=0
  validate_identifier "${database}"
  validate_identifier "${username}"
  asset_values="$(load_e2e_asset_values "${database}" "${username}")"

  while IFS= read -r asset_path; do
    [[ -n "${asset_path}" ]] || continue
    asset_status=0
    sync_e2e_asset "${asset_path}" "${upload_dir}" || asset_status=$?
    if [[ "${asset_status}" == "1" ]]; then
      downloaded_assets=$((downloaded_assets + 1))
    elif [[ "${asset_status}" == "2" ]]; then
      unavailable_assets=$((unavailable_assets + 1))
    fi
  done < <(printf '%s\n' "${asset_values}" | extract_e2e_asset_paths)
  echo "E2E image assets synced (${downloaded_assets} downloaded, ${unavailable_assets} unavailable)."
  if (( unavailable_assets > 0 )); then
    echo "E2E image asset sync failed because referenced files are unavailable." >&2
    return 1
  fi
}

verify_e2e_assets() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  local upload_dir="${E2E_UPLOAD_DIR:-/tmp/yixianghui-e2e/uploads}"
  local asset_values
  local asset_path
  local target
  local response_file
  local verified_assets=0
  validate_identifier "${database}"
  validate_identifier "${username}"
  asset_values="$(load_e2e_asset_values "${database}" "${username}")"
  response_file="$(mktemp "${RUNTIME_DIR}/asset-response.XXXXXX")"

  while IFS= read -r asset_path; do
    [[ -n "${asset_path}" ]] || continue
    target="${upload_dir}${asset_path#/profile}"
    if ! is_image_file "${target}"; then
      rm -f "${response_file}"
      echo "Synced E2E asset is missing or invalid: ${asset_path}" >&2
      return 1
    fi
    if ! "${CURL_BIN}" --fail --silent --show-error --location \
      --output "${response_file}" \
      "http://127.0.0.1:${BACKEND_PORT}/api${asset_path}"; then
      rm -f "${response_file}"
      echo "Local backend did not serve E2E asset: ${asset_path}" >&2
      return 1
    fi
    if ! is_image_file "${response_file}"; then
      rm -f "${response_file}"
      echo "Local backend returned a non-image E2E asset: ${asset_path}" >&2
      return 1
    fi
    verified_assets=$((verified_assets + 1))
  done < <(printf '%s\n' "${asset_values}" | extract_e2e_asset_paths)

  rm -f "${response_file}"
  echo "E2E image assets verified (${verified_assets} local backend images)."
}

setup() {
  require_executable "${MYSQL_BIN}"
  require_executable "${REDIS_SERVER_BIN}"
  require_executable "${REDIS_CLI_BIN}"
  require_executable "${MAVEN_BIN}"
  require_executable "${JAVA_BIN}"
  require_executable "${CURL_BIN}"
  require_executable "${FILE_BIN}"
  mkdir -p "${RUNTIME_DIR}/logs"
  start_mysql
  start_redis
  setup_database
  apply_goods_order_schema
  apply_activity_schema
  configure_service_availability
  apply_e2e_product_fixture
  apply_e2e_asset_fixtures
  install_e2e_bundled_assets
  sync_e2e_assets
  echo "Backend dependencies are ready."
}

build() {
  setup
  "${MAVEN_BIN}" -q -DskipTests package -f "${REPO_ROOT}/pom.xml"
  echo "Backend build completed."
}

wait_for_backend() {
  local url="http://127.0.0.1:${BACKEND_PORT}/api/captchaImage"
  for _ in {1..60}; do
    if curl --fail --silent --show-error "${url}" >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  echo "Backend did not become ready. See ${BACKEND_LOG}" >&2
  tail -n 80 "${BACKEND_LOG}" >&2
  exit 1
}

start() {
  setup
  if backend_running; then
    echo "Backend is already running on port ${BACKEND_PORT}."
    return
  fi
  if [[ ! -f "${APP_JAR}" ]]; then
    build
  fi
  nohup "${JAVA_BIN}" -DLOG_PATH="${RUNTIME_DIR}/logs" -jar "${APP_JAR}" \
    --spring.profiles.active=e2e \
    --server.port="${BACKEND_PORT}" \
    >"${BACKEND_LOG}" 2>&1 &
  echo $! > "${BACKEND_PID_FILE}"
  wait_for_backend
  echo "Backend is ready at http://127.0.0.1:${BACKEND_PORT}/api"
}

stop() {
  if ! backend_running; then
    rm -f "${BACKEND_PID_FILE}"
    echo "Backend is not running."
    return
  fi
  local pid
  pid="$(<"${BACKEND_PID_FILE}")"
  kill "${pid}"
  rm -f "${BACKEND_PID_FILE}"
  echo "Backend stopped."
}

test_backend() {
  start
  local captcha_url="http://127.0.0.1:${BACKEND_PORT}/api/captchaImage"
  local site_url="http://127.0.0.1:${BACKEND_PORT}/api/mnp/index/get_site_list"
  local category_url="http://127.0.0.1:${BACKEND_PORT}/api/mnp/index/get_goods_category?status=1"
  local education_list_url="http://127.0.0.1:${BACKEND_PORT}/api/mnp/index/queryGoodsList?pageNum=1&pageSize=50"
  local education_detail_url="http://127.0.0.1:${BACKEND_PORT}/api/mnp/index/get_goods_info/38"
  local activity_url="http://127.0.0.1:${BACKEND_PORT}/api/mnp/index/activity_info/1"
  curl --fail --silent --show-error "${captcha_url}" | grep -Eq '"code"[[:space:]]*:[[:space:]]*200'
  curl --fail --silent --show-error "${site_url}" | grep -Eq '"code"[[:space:]]*:[[:space:]]*200'
  curl --fail --silent --show-error "${category_url}" | grep -Eq '"categoryId"[[:space:]]*:'
  curl --fail --silent --show-error -X POST -H 'Content-Type: application/json' \
    --data '{"categoryId":58}' "${education_list_url}" | grep -q '水彩绘画'
  curl --fail --silent --show-error "${education_detail_url}" | grep -Eq '"sectionName"[[:space:]]*:[[:space:]]*"课程内容"'
  curl --fail --silent --show-error "${activity_url}" | grep -Eq '"activityId"[[:space:]]*:[[:space:]]*1'
  verify_e2e_assets
  echo "Backend smoke tests passed."
}

case "${1:-}" in
  setup) setup ;;
  build) build ;;
  start) start ;;
  stop) stop ;;
  test) test_backend ;;
  *) echo "Usage: $0 {setup|build|start|stop|test}" >&2; exit 2 ;;
esac
