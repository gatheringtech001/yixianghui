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
  local expected_column_count=4
  local actual_column_count
  require_file "${migration}"
  validate_identifier "${database}"
  validate_identifier "${username}"
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

load_e2e_asset_values() {
  local database="$1"
  local username="$2"
  "${MYSQL_BIN}" -u"${username}" -Nse "
    SELECT CONVERT(ad_image USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_ad_content WHERE ad_image <> ''
    UNION SELECT CONVERT(activity_cover USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_activity WHERE activity_cover <> ''
    UNION SELECT CONVERT(article_cover USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_article WHERE article_cover <> ''
    UNION SELECT CONVERT(card_image USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_card WHERE card_image <> ''
    UNION SELECT CONVERT(express_image USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_express WHERE express_image <> ''
    UNION SELECT CONVERT(goods_cover USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods WHERE goods_cover <> ''
    UNION SELECT CONVERT(goods_images USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods WHERE goods_images <> ''
    UNION SELECT CONVERT(data_image USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_goods_sku_data WHERE data_image <> ''
    UNION SELECT CONVERT(page_cover USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.app_single_page WHERE page_cover <> ''
    UNION SELECT CONVERT(avatar USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.sys_auth_user WHERE avatar <> ''
    UNION SELECT CONVERT(avatar USING utf8mb4) COLLATE utf8mb4_general_ci FROM ${database}.sys_user WHERE avatar <> '';
  "
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
  local asset_value
  local asset_path
  local asset_status
  local downloaded_assets=0
  local unavailable_assets=0
  local -a asset_paths
  validate_identifier "${database}"
  validate_identifier "${username}"
  asset_values="$(load_e2e_asset_values "${database}" "${username}")"

  while IFS= read -r asset_value; do
    IFS=',' read -ra asset_paths <<< "${asset_value}"
    for asset_path in "${asset_paths[@]}"; do
      asset_path="${asset_path#"${asset_path%%[![:space:]]*}"}"
      asset_path="${asset_path%"${asset_path##*[![:space:]]}"}"
      [[ "${asset_path}" == /profile/* ]] || continue
      asset_status=0
      sync_e2e_asset "${asset_path}" "${upload_dir}" || asset_status=$?
      if [[ "${asset_status}" == "1" ]]; then
        downloaded_assets=$((downloaded_assets + 1))
      elif [[ "${asset_status}" == "2" ]]; then
        unavailable_assets=$((unavailable_assets + 1))
      fi
    done
  done <<< "${asset_values}"
  echo "E2E image assets synced (${downloaded_assets} downloaded, ${unavailable_assets} unavailable)."
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
  configure_service_availability
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
  curl --fail --silent --show-error "${captcha_url}" | grep -Eq '"code"[[:space:]]*:[[:space:]]*200'
  curl --fail --silent --show-error "${site_url}" | grep -Eq '"code"[[:space:]]*:[[:space:]]*200'
  curl --fail --silent --show-error "${category_url}" | grep -Eq '"categoryId"[[:space:]]*:'
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
