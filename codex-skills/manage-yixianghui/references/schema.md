# Business schema map

## Content and catalog

- Products: `app_goods` -> `app_goods_category`, `app_goods_sku`, `app_goods_sku_option`, `app_goods_sku_data`, `app_goods_related`, `app_goods_education_ext`.
- Activities: `app_activity` -> `app_activity_category`, `app_activity_order`.
- Ads/assets: `app_ad_position` -> `app_ad_content`; resolve ad placement by stable `position_code`, not numeric ID.
- Editorial: `app_article` -> `app_article_category`; `app_single_page`; `app_site_nav`.

## Commerce

- Orders: `app_goods_order` -> `app_goods_order_detail`.
- Payments: `app_pay_log`; refunds: `app_pay_refund_log`; after-sale: `app_goods_order_after`.
- Coupons: `app_goods_coupon` -> `app_goods_coupon_got`.
- Treat every commerce write as high risk and outside the generic editor.

## People and operations

- Members: `app_user_info` plus address/bank/card/cash/charge/log tables.
- CRM: `app_customer`, `app_customer_income`, `app_consultant`, `app_supplier`.
- These tables contain PII or financial state. Query minimally and do not mutate through this suite's generic editor.

## Platform

- `sys_*`: users, roles, menus, permissions, dictionaries, logs, departments, and config.
- `qrtz_*`: scheduler state; `gen_*`: code generator metadata.
- Do not use generic data operations on platform tables.

Always inspect live schema before generating a query. The local E2E database contains recovery migrations and fixtures, so a column existing locally does not prove it exists in production.
