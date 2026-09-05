import { chunkDocument, contentHash, pointId } from "./lib.mjs";

export const CATALOG_TABLES = [
  "app_goods", "app_goods_category", "app_goods_category_attr", "app_goods_sku",
  "app_goods_sku_option", "app_goods_sku_data", "app_goods_related", "app_goods_education_ext",
  "app_activity", "app_activity_category", "app_travel_base", "app_activity_plan_feishu",
];
const CHILDREN = ["app_goods_sku", "app_goods_sku_option", "app_goods_sku_data",
  "app_goods_related", "app_goods_education_ext"];
const TYPES = { hotel: "旅居住宿", education: "课程教育", online: "商城实物", o2o: "到店商品" };
const LABELS = {
  goods_name: "商品名称", goods_type: "商品类型", goods_id: "商品ID", category_id: "分类ID",
  category_name: "分类名称", description: "简介", content: "详细内容", price: "价格",
  vip_price: "会员价格", unit: "单位", specifications: "规格", stock: "库存", tags: "标签",
  status: "状态", sku_name: "房型或套餐名称", sku_id: "SKU_ID", par_sku_id: "父房型SKU_ID",
  option_name: "选项名称", option_value: "选项值", option_value_unit: "选项单位",
  valid_time: "生效日期", invalid_time: "失效日期", sale_price: "销售价格",
  data_values: "规格组合", data_price: "组合价格", data_stock: "组合库存",
  section_name: "详情分区", course_time: "上课时间", course_place: "上课地点",
  teacher_name: "授课教师", lesson_count: "课时数", start_date: "开课日期",
  signup_start: "报名开始", signup_end: "报名截止", material_note: "课程材料说明",
  consult_phone: "课程公开咨询电话", activity_name: "活动名称", activity_time: "活动时间",
  activity_end_time: "活动结束", address: "活动场地", sign_end_time: "报名截止",
  is_free: "是否免费", max_count: "人数上限", goods_cover: "商品封面", goods_images: "商品图片",
  fs_fldldlchk8: "基地名称", fs_fldugcrrzs: "酒店名称", fs_fldgyglmkr: "城市",
  fs_fldj6dy18l: "房间数", fs_fldgm82uyi: "房型", fs_fldm6v2bs9: "订单数量",
  fs_fldq17qpv1: "创建时间", fs_fldpm3filh: "最后更新时间", fs_fldc9hgz0b: "活动日期",
  fs_fldydtzj1z: "所属站点", fs_fldwqgidau: "活动地址", fs_fld23z5aho: "活动记录",
  fs_fldjmddb2q: "活动供应商", fs_flds6mtsub: "是否结算", fs_fldzg799h5: "父活动记录",
};
const SKU_TYPES = { 200: "房型", 201: "日历价格", 202: "固定套餐" };
const OPTION_TYPES = { 301: "均价", 302: "套餐总价", 303: "天数", 304: "套餐说明", 305: "房型信息" };

function plain(value) {
  if (value == null) return "未填写";
  const raw = typeof value === "object" ? JSON.stringify(value) : String(value);
  return raw.replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, "")
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, "")
    .replace(/<[^>]*>/g, " ").replace(/&nbsp;|&#160;/gi, " ")
    .replace(/&amp;/gi, "&").replace(/&lt;/gi, "<").replace(/&gt;/gi, ">")
    .replace(/&quot;/gi, '"').replace(/&#39;/gi, "'");
}

function rowText(row) {
  return Object.entries(row).map(([key, value]) => {
    let rendered = plain(value);
    if (key === "status") rendered += String(value) === "1" ? "（启用/上架）" : "（未启用/下架）";
    if (key === "goods_type") rendered += ` ${TYPES[value] ?? ""}`;
    if (key === "sku_type") rendered += ` ${SKU_TYPES[value] ?? ""}`;
    if (key === "option_type") rendered += ` ${OPTION_TYPES[value] ?? ""}`;
    return `${LABELS[key] ?? key}: ${rendered}`;
  }).join("\n");
}

export function validateSnapshot(snapshot) {
  if (snapshot?.version !== 1 || snapshot.source !== "production_mysql"
      || !Number.isFinite(Date.parse(snapshot.exportedAt))) throw new Error("Invalid catalog snapshot");
  if (snapshot.fingerprint !== contentHash(JSON.stringify(snapshot.tables))) throw new Error("Catalog hash mismatch");
  if (Object.keys(snapshot.tables).length !== CATALOG_TABLES.length) throw new Error("Incomplete catalog table set");
  for (const table of CATALOG_TABLES) {
    const rows = snapshot.tables[table];
    const shape = snapshot.schemas?.[table];
    if (!Array.isArray(rows) || !shape?.primaryKey) throw new Error(`Missing catalog table/schema: ${table}`);
    const ids = rows.map((row) => String(row[shape.primaryKey]));
    if (ids.some((id) => !/^\d+$/.test(id)) || new Set(ids).size !== rows.length) {
      throw new Error(`Invalid or duplicate primary keys: ${table}`);
    }
  }
}

function categoryChain(categories, row) {
  const byId = new Map(categories.map((category) => [String(category.category_id), category]));
  const ids = new Set(String(row.category_ids ?? "").split(/[,|]/));
  ids.add(String(row.category_id));
  for (const id of ids) {
    const parent = String(byId.get(id)?.parent_id ?? "0");
    if (parent !== "0") ids.add(parent);
  }
  return categories.filter((category) => ids.has(String(category.category_id)));
}

export function buildCatalog(snapshot) {
  validateSnapshot(snapshot);
  const { tables, schemas } = snapshot;
  const documents = [];
  const represented = new Set();
  const reference = (table, row) => `${table}:${row[schemas[table].primaryKey]}`;
  const add = (table, row, groups, orphan = false) => {
    const id = String(row[schemas[table].primaryKey]);
    const title = plain(row.goods_name ?? row.activity_name ?? row.category_name
      ?? row.sku_name ?? row.fs_fldldlchk8 ?? row.option_name ?? `${table} ${id}`);
    for (const [name, rows] of Object.entries(groups)) {
      for (const item of rows) represented.add(reference(name, item));
    }
    const raw = Object.entries(groups).flatMap(([name, rows]) => rows.map((item) =>
      `来源表: ${name}\n${rowText(item)}`
    )).join("\n\n");
    const sourceId = `catalog:${table}:${id}`;
    const statusLabel = row.status == null ? "内部资料" : String(row.status) === "1" ? "上架" : "下架";
    const heading = `${title} | ${TYPES[row.goods_type] ?? table} | ${statusLabel}${orphan ? " | 关联缺失，仅供管理核查" : ""}`;
    documents.push({ sourceId, title, heading, raw, groups, orphan, table, id,
      status: row.status ?? null, goodsType: row.goods_type ?? null, goodsId: row.goods_id ?? null,
      hash: contentHash(JSON.stringify(groups)),
      sourceUrl: `https://shzxj.lk01.cn/${table === "app_activity" ? "activity/app_activity"
        : table === "app_travel_base" || table === "app_activity_plan_feishu" ? "system/feishu_migration" : "goods/app_goods"}`,
    });
  };
  for (const row of tables.app_goods) {
    const groups = { app_goods: [row], app_goods_category: categoryChain(tables.app_goods_category, row) };
    for (const table of CHILDREN) groups[table] = tables[table].filter((child) => String(child.goods_id) === String(row.goods_id));
    add("app_goods", row, groups);
  }
  for (const row of tables.app_activity) {
    add("app_activity", row, { app_activity: [row],
      app_activity_category: categoryChain(tables.app_activity_category, row),
      app_activity_plan_feishu: tables.app_activity_plan_feishu.filter((plan) =>
        plan.canonical_table === "app_activity" && String(plan.canonical_id) === String(row.activity_id)),
    });
  }
  // Keep unused categories, independent bases and pre-existing orphan rows discoverable.
  for (const table of CATALOG_TABLES) {
    for (const row of tables[table]) {
      if (!represented.has(reference(table, row))) add(table, row, { [table]: [row] }, CHILDREN.includes(table));
    }
  }
  const counts = Object.fromEntries(CATALOG_TABLES.map((table) => [table, tables[table].length]));
  if (represented.size !== Object.values(counts).reduce((sum, count) => sum + count, 0)) {
    throw new Error("Catalog record coverage mismatch");
  }
  return { documents, counts, represented: represented.size, orphans: documents.filter((doc) => doc.orphan).length };
}

export function catalogPoints(document, snapshotAt) {
  const parts = chunkDocument(document.heading, document.raw);
  return parts.map((part) => ({
    id: pointId(document.sourceId, part.index), text: part.text,
    payload: {
      source_type: "mysql_catalog", source_id: document.sourceId, title: document.title,
      content: part.text, content_hash: contentHash(part.text), chunk_index: part.index,
      source_url: document.sourceUrl, permission_scope: "internal",
      entity_table: document.table, entity_id: document.id, goods_id: document.goodsId,
      goods_type: document.goodsType, product_status: document.status, snapshot_at: snapshotAt,
      orphan_relation: document.orphan, structured_point_id: pointId(document.sourceId, 0),
      ...(part.index === 0 ? { structured_data: document.groups } : {}),
    },
  }));
}
