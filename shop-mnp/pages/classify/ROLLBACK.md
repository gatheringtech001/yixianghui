# 服务页省-市-商品联动改造 · 回退说明

若新方案不符合预期，可用以下方式恢复改造前的版本。

## 方式一：使用 legacy 备份（推荐）

在项目根目录执行（PowerShell）：

```powershell
Copy-Item shop-mnp/pages/classify/classify.vue.legacy shop-mnp/pages/classify/classify.vue -Force
Copy-Item shop-mnp/pages/classify/classify.scss.legacy shop-mnp/pages/classify/classify.scss -Force
Copy-Item shop-mnp/packagesPublic/site/index.vue.legacy shop-mnp/packagesPublic/site/index.vue -Force
Copy-Item shop-mnp/api/index.js.legacy shop-mnp/api/index.js -Force
Remove-Item shop-mnp/utils/serviceFilter.js -ErrorAction SilentlyContinue
```

然后重新编译小程序即可。后端新增接口可保留，不影响旧版前端运行。

## 方式二：Git 回退

若改造前已提交，可对相关文件执行：

```bash
git checkout HEAD -- shop-mnp/pages/classify/classify.vue
git checkout HEAD -- shop-mnp/pages/classify/classify.scss
git checkout HEAD -- shop-mnp/packagesPublic/site/index.vue
git checkout HEAD -- shop-mnp/api/index.js
```

并删除 `shop-mnp/utils/serviceFilter.js`。

## 本次改造涉及文件

| 文件 | 说明 |
|------|------|
| `shop-mnp/pages/classify/classify.vue` | 服务页：①省 ②市 ③商品 |
| `shop-mnp/pages/classify/classify.scss` | 空态样式 |
| `shop-mnp/utils/serviceFilter.js` | 统一筛选状态（新增） |
| `shop-mnp/api/index.js` | getProvinces / getCities |
| `shop-mnp/packagesPublic/site/index.vue` | 省份选择页 |
| `shop-mnp/pages/home/home.vue` | 首页热门城市同步状态 |
| 后端 `AppIndexController` 等 | get_provinces / get_cities |

## 后台数据要求

- `sys_dept` 需为 **省/区域 → 城市（is_site=1）** 两级结构
- 商品 `app_goods.dept_id` 需指向具体城市站点
- 若 `get_provinces` 返回空，请检查部门树配置
