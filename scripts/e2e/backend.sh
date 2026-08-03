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

disable_unavailable_services() {
  local database="${E2E_DB_NAME:-yixianghui_e2e}"
  local username="${E2E_DB_USERNAME:-yixianghui_e2e}"
  validate_identifier "${database}"
  validate_identifier "${username}"
  "${MYSQL_BIN}" -u"${username}" "${database}" -e "
    UPDATE sys_menu
    SET status = '1'
    WHERE perms IN (
      'system:app_goods_order:list',
      'system:app_goods_comment:list',
      'system:app_goods_sku:list',
      'system:app_goods_collect:list',
      'system:app_activity:list',
      'system:app_activity_order:list'
    );
  "
}

sync_city_assets() {
  local upload_dir="${E2E_UPLOAD_DIR:-/tmp/yixianghui-e2e/uploads}"
  local asset_path
  local target
  local assets=(
    '/profile/upload/2025/09/14/贵州_20250914153850A009.jpg'
    '/profile/upload/2025/09/14/云南_20250914160307A010.png'
    '/profile/upload/2025/09/14/广州_20250914160323A011.png'
    '/profile/upload/2025/09/14/广西_20250914160651A012.png'
    '/profile/upload/2025/09/14/全国_20250914161325A013.png'
  )

  for asset_path in "${assets[@]}"; do
    target="${upload_dir}${asset_path#/profile}"
    if [[ -s "${target}" ]]; then
      continue
    fi
    mkdir -p "$(dirname "${target}")"
    "${CURL_BIN}" --fail --silent --show-error --location \
      --connect-timeout 10 --max-time 60 \
      --output "${target}.tmp" "${E2E_ASSET_BASE_URL}${asset_path}"
    mv "${target}.tmp" "${target}"
  done
  echo "City image assets are ready."
}

setup() {
  require_executable "${MYSQL_BIN}"
  require_executable "${REDIS_SERVER_BIN}"
  require_executable "${REDIS_CLI_BIN}"
  require_executable "${MAVEN_BIN}"
  require_executable "${JAVA_BIN}"
  require_executable "${CURL_BIN}"
  mkdir -p "${RUNTIME_DIR}/logs"
  start_mysql
  start_redis
  setup_database
  disable_unavailable_services
  sync_city_assets
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
