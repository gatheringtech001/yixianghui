#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
APP_DIR="${REPO_ROOT}/shop-mnp"
MINIPROGRAM_OUTPUT="${APP_DIR}/unpackage/dist/dev/mp-weixin"
HBUILDER_CLI="${HBUILDER_CLI:-/Applications/HBuilderX.app/Contents/MacOS/cli}"
WECHAT_DEVTOOLS_CLI="${WECHAT_DEVTOOLS_CLI:-/Applications/wechatwebdevtools.app/Contents/MacOS/cli}"
HBUILDER_CONTENTS="$(cd "$(dirname "${HBUILDER_CLI}")/.." && pwd)"
HBUILDER_PLUGINS="${HBUILDER_CONTENTS}/HBuilderX/plugins"
HBUILDER_NODE="${HBUILDER_PLUGINS}/node/node"
UNIAPP_CLI="${HBUILDER_PLUGINS}/uniapp-cli/bin/uniapp-cli.js"
UNIAPP_KILL_SCRIPT="${HBUILDER_PLUGINS}/uniapp-extension/static/kill.js"
H5_OUTPUT="${APP_DIR}/unpackage/dist/dev/h5"

require_executable() {
  if [[ ! -x "$1" ]]; then
    echo "Required executable is missing: $1" >&2
    exit 1
  fi
}

require_file() {
  if [[ ! -f "$1" ]]; then
    echo "Required file is missing: $1" >&2
    exit 1
  fi
}

prepare_hbuilder() {
  require_executable "${HBUILDER_CLI}"
  "${HBUILDER_CLI}" open >/dev/null
  "${HBUILDER_CLI}" project open --path "${APP_DIR}" >/dev/null
}

compile_target() {
  local target="$1"
  local expected_file="$2"
  local output
  local args=(launch "${target}" --project "${APP_DIR}" --compile true --continue-on-error false)
  if [[ "${target}" == "mp-weixin" ]]; then
    args+=(--runtime-log true)
  fi
  prepare_hbuilder
  output="$("${HBUILDER_CLI}" "${args[@]}" 2>&1)"
  printf '%s\n' "${output}"
  if ! grep -q "编译成功" <<<"${output}"; then
    echo "HBuilderX did not report a successful ${target} build." >&2
    exit 1
  fi
  if [[ -n "${expected_file}" && ! -f "${expected_file}" ]]; then
    echo "Expected build artifact is missing: ${expected_file}" >&2
    exit 1
  fi
}

compile_miniprogram() {
  compile_target "mp-weixin" "${MINIPROGRAM_OUTPUT}/app.js"
}

run_h5() {
  require_executable "${HBUILDER_NODE}"
  require_file "${UNIAPP_CLI}"
  require_file "${UNIAPP_KILL_SCRIPT}"
  mkdir -p "${H5_OUTPUT}"
  echo "Starting the H5 development server. Press Ctrl-C to stop."
  cd "${APP_DIR}"
  NODE_ENV=development \
    UNI_PLATFORM=h5 \
    UNI_INPUT_DIR="${APP_DIR}" \
    UNI_OUTPUT_DIR="${H5_OUTPUT}" \
    UNI_HBUILDERX_PLUGINS="${HBUILDER_PLUGINS}" \
    VUE_CLI_CONTEXT="${HBUILDER_PLUGINS}/uniapp-cli" \
    exec "${HBUILDER_NODE}" \
      --max-old-space-size=5120 \
      --no-warnings \
      -r "${UNIAPP_KILL_SCRIPT}" \
      "${UNIAPP_CLI}" \
      -p h5
}

install_automation() {
  pnpm --dir "${REPO_ROOT}/e2e/miniprogram" install --frozen-lockfile
}

test_miniprogram() {
  require_executable "${WECHAT_DEVTOOLS_CLI}"
  "${REPO_ROOT}/scripts/e2e/backend.sh" test
  compile_miniprogram
  install_automation
  WECHAT_DEVTOOLS_CLI="${WECHAT_DEVTOOLS_CLI}" \
    MINIPROGRAM_PROJECT_PATH="${MINIPROGRAM_OUTPUT}" \
    pnpm --dir "${REPO_ROOT}/e2e/miniprogram" test
}

case "${1:-}" in
  build-mp) compile_miniprogram ;;
  run-h5) run_h5 ;;
  install) install_automation ;;
  test-mp) test_miniprogram ;;
  *) echo "Usage: $0 {build-mp|run-h5|install|test-mp}" >&2; exit 2 ;;
esac
