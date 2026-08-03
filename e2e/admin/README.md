# 管理端与 H5 E2E

## 前置服务

- Java 后端：`http://127.0.0.1:18080/api`
- 管理端：`http://127.0.0.1:8081`
- H5：`http://127.0.0.1:8080`

管理端测试必须由调用方注入本地测试管理员凭据，测试代码不保存密码：

```bash
export E2E_ADMIN_USER='<local test admin>'
export E2E_ADMIN_PASSWORD='<local test password>'
```

## 安装与执行

```bash
pnpm install --frozen-lockfile
pnpm test
pnpm test:h5
```

`pnpm test` 默认执行认证、管理端、H5 全部项目。只执行管理端时：

```bash
pnpm exec playwright test \
  --project=setup \
  --project=public \
  --project=chromium
```

生成结构化报告数据时传入证据目录：

```bash
pnpm report -- ../../outputs/ui-full-product-qa-YYYYMMDD-HHMMSS
```

微信小程序自动化位于 `../miniprogram`，需先通过 HBuilderX 编译小程序，并在微信开发者工具中开启 CLI/自动化权限。
