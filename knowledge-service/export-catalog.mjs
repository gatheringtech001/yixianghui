import fs from "node:fs/promises";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { pathToFileURL } from "node:url";
import { contentHash } from "./lib.mjs";
import { CATALOG_TABLES as TABLES } from "./catalog.mjs";

const CLI = "/Users/kevin/.codex/skills/manage-yixianghui/scripts/yxh_db.py";
// People, bookings and settlement references belong to operational data, not the catalog.
const EXCLUDED = { app_travel_base: [
  "fs_fldsjmwgzz", "fs_fldvvxukkl", "fs_fld7nikyec", "fs_fldd4dinc0",
] };
const PUBLIC_CONTACTS = new Set(["app_activity.address", "app_goods_education_ext.consult_phone"]);
const SENSITIVE = /password|passwd|secret|token|openid|unionid|idcard|identity|mobile|phone|bank|account|address|notify_content/i;

function cli(args) {
  return execFileSync("python3", [CLI, ...args, "--env", "production"], {
    encoding: "utf8", maxBuffer: 32 * 1024 * 1024, timeout: 90_000,
  }).trimEnd();
}

function query(sql, sensitive = false) {
  const args = ["query", "--sql", sql];
  if (sensitive) args.push("--include-sensitive", "--reason",
    "Complete catalog export requested by user: public course contact and activity venue only; no customer or member data");
  return cli(args).split("\n").slice(1);
}

function schema(table) {
  const columns = cli(["schema", "--table", table]).split("\n").slice(1)
    .map((line) => {
      const [name, type, , , key] = line.split("\t");
      if (!/^[a-z][a-z0-9_]*$/.test(name)) throw new Error(`Invalid column in ${table}`);
      return { name, type, key };
    });
  const keys = columns.filter((column) => column.key === "PRI");
  if (keys.length !== 1) throw new Error(`Expected one primary key in ${table}`);
  const selected = columns.filter((column) => !(EXCLUDED[table] ?? []).includes(column.name));
  for (const column of selected) {
    if (SENSITIVE.test(column.name) && !PUBLIC_CONTACTS.has(`${table}.${column.name}`)) {
      throw new Error(`Review sensitive catalog column: ${table}.${column.name}`);
    }
  }
  return { primaryKey: keys[0].name, columns: selected, excluded: EXCLUDED[table] ?? [] };
}

function readTable(table, shape) {
  const fields = shape.columns.map(({ name, type }) =>
    `'${name}',${/bigint|decimal/.test(type) ? `CAST(${name} AS CHAR)` : name}`
  ).join(",");
  const sensitive = shape.columns.some(({ name }) => SENSITIVE.test(name));
  const rows = [];
  let cursor = "0";
  while (true) {
    const page = query(`SELECT JSON_OBJECT(${fields}) AS row_json FROM ${table} WHERE ${shape.primaryKey}>${cursor} ORDER BY ${shape.primaryKey} LIMIT 200`, sensitive)
      .filter(Boolean).map((line) => JSON.parse(line));
    rows.push(...page);
    if (page.length < 200) break;
    const next = String(page.at(-1)[shape.primaryKey]);
    if (!/^\d+$/.test(next) || BigInt(next) <= BigInt(cursor)) throw new Error("Invalid page cursor");
    cursor = next;
  }
  const count = Number(query(`SELECT COUNT(*) AS total FROM ${table} LIMIT 1`)[0]);
  if (count !== rows.length) throw new Error(`Count drift in ${table}: ${rows.length}/${count}`);
  return rows;
}

export async function exportCatalog(output) {
  const tables = {};
  const schemas = {};
  for (const table of TABLES) {
    schemas[table] = schema(table);
    tables[table] = readTable(table, schemas[table]);
    console.log(JSON.stringify({ table, rows: tables[table].length }));
  }
  // A second complete read detects changes across these separate read-only queries.
  for (const table of TABLES) {
    if (JSON.stringify(schemas[table]) !== JSON.stringify(schema(table))) {
      throw new Error(`Catalog schema changed during export: ${table}`);
    }
    if (contentHash(JSON.stringify(tables[table])) !== contentHash(JSON.stringify(readTable(table, schemas[table])))) {
      throw new Error(`Catalog changed during export; rerun: ${table}`);
    }
  }
  const snapshot = { version: 1, source: "production_mysql", exportedAt: new Date().toISOString(),
    tables, schemas, fingerprint: contentHash(JSON.stringify(tables)) };
  await fs.mkdir(path.dirname(output), { recursive: true, mode: 0o700 });
  await fs.writeFile(output, JSON.stringify(snapshot), { mode: 0o600, flag: "wx" });
  console.log(JSON.stringify({ output, fingerprint: snapshot.fingerprint,
    counts: Object.fromEntries(TABLES.map((table) => [table, tables[table].length])), verifiedReads: 2 }));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const output = process.argv[2];
  if (!output || !path.isAbsolute(output)) throw new Error("Provide an absolute snapshot output path");
  await exportCatalog(output);
}
