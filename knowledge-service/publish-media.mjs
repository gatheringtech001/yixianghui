import fs from "node:fs";
import zlib from "node:zlib";
import crypto from "node:crypto";
import { spawnSync } from "node:child_process";
import { pathToFileURL } from "node:url";

const COMMAND_URL = "https://management.azure.com/subscriptions/3475925c-a76c-4904-850e-8071a6bed970/resourceGroups/gatheringtech/providers/Microsoft.Compute/virtualMachines/gatheringtech/runCommands/yxh-media-sync?api-version=2024-11-01";

function azure(args, input) {
  const result = spawnSync("az", args, { input, encoding: "utf8", timeout: 90_000, maxBuffer: 1024 * 1024 });
  if (result.status !== 0) throw new Error("Azure command failed; check login and VM access");
  return result.stdout;
}

async function publishSnapshot(snapshot) {
  const raw = Buffer.from(JSON.stringify(snapshot));
  if (snapshot.version !== 1 || !Array.isArray(snapshot.records)) throw new Error("Invalid snapshot");
  const hash = crypto.createHash("sha256").update(raw).digest("hex");
  const parts = zlib.gzipSync(raw).toString("base64").match(/.{1,40000}/g);
  const script = `set -eu
umask 077
node <<'DECODE'
const fs=require('fs'),z=require('zlib'),c=require('crypto');
const packed=Array.from({length:${parts.length}},(_,i)=>process.env['MEDIA_PART_'+i]).join('');
const bytes=z.gunzipSync(Buffer.from(packed,'base64'));
if(c.createHash('sha256').update(bytes).digest('hex')!=='${hash}')throw Error('Transport checksum mismatch');
fs.writeFileSync('/var/lib/yixianghui-knowledge/media-snapshot.json.next',bytes,{mode:0o600});
fs.renameSync('/var/lib/yixianghui-knowledge/media-snapshot.json.next','/var/lib/yixianghui-knowledge/media-snapshot.json');
DECODE
cd /opt/yixianghui-knowledge
node --env-file=/etc/yixianghui-knowledge.env --input-type=module <<'REFRESH'
if (${snapshot.refreshText !== false}) {
const response=await fetch('http://127.0.0.1:3210/admin/sync',{method:'POST',headers:{authorization:'Bearer '+process.env.KNOWLEDGE_ADMIN_TOKEN},signal:AbortSignal.timeout(900000)});
const result=await response.json();if(!response.ok)throw Error('Text refresh failed: '+response.status);console.log(JSON.stringify({text:result}));
}
REFRESH
node --env-file=/etc/yixianghui-knowledge.env media-sync.mjs /var/lib/yixianghui-knowledge/media-snapshot.json
`;
  const body = { location: "eastasia", properties: { source: { script },
    protectedParameters: parts.map((value, index) => ({ name: `MEDIA_PART_${index}`, value })),
    timeoutInSeconds: 1800, asyncExecution: false } };
  azure(["rest", "--method", "put", "--url", COMMAND_URL, "--body", "@/dev/stdin", "-o", "none"], JSON.stringify(body));
  for (let attempt = 0; attempt < 90; attempt++) {
    await new Promise((resolve) => setTimeout(resolve, 20_000));
    const result = JSON.parse(azure(["rest", "--method", "get", "--url", COMMAND_URL + "&$expand=instanceView", "-o", "json"]));
    const view = result.properties.instanceView;
    if (view?.executionState === "Succeeded" && view.exitCode === 0) {
      console.log(JSON.stringify({ published: true, records: snapshot.records.length, output: view.output }));
      return;
    }
    if (["Failed", "Canceled", "TimedOut"].includes(view?.executionState)) throw new Error(`Media publish failed: ${view.executionState}`);
    if (attempt % 3 === 0) console.log(JSON.stringify({ mediaPublish: "running" }));
  }
  throw new Error("Media publish did not finish within 30 minutes");
}

export async function publishMedia(file) {
  const snapshot = JSON.parse(fs.readFileSync(file, "utf8"));
  if (!Array.isArray(snapshot.records) || new Set(snapshot.records.map((record) => record.id)).size !== snapshot.records.length) {
    throw new Error("Invalid or duplicate media records");
  }
  const batches = [];
  const divide = (records) => {
    if (zlib.gzipSync(JSON.stringify({ ...snapshot, records })).length <= 600_000) return batches.push(records);
    if (records.length <= 1) throw new Error("A media record exceeds transport limits");
    const middle = Math.ceil(records.length / 2);
    divide(records.slice(0, middle));
    divide(records.slice(middle));
  };
  divide(snapshot.records);
  for (let index = 0; index < batches.length; index++) {
    await publishSnapshot({ ...snapshot, records: batches[index], partial: batches.length > 1 || snapshot.partial === true,
      refreshText: index === 0 });
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) await publishMedia(process.argv[2]);
