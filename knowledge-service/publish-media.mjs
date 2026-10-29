import fs from "node:fs";
import zlib from "node:zlib";
import crypto from "node:crypto";
import { spawnSync } from "node:child_process";
import { pathToFileURL } from "node:url";
import { fileURLToPath } from 'node:url';

const COMMAND_URL = "https://management.azure.com/subscriptions/3475925c-a76c-4904-850e-8071a6bed970/resourceGroups/gatheringtech/providers/Microsoft.Compute/virtualMachines/gatheringtech/runCommands/yxh-media-sync?api-version=2024-11-01";

const MAX_PACKED_BYTES = 600_000;
const MAX_COLD_WORK = 128;
const sourceHash = record => crypto.createHash('sha256').update(JSON.stringify(record)).digest('hex');
const safeDetail = value => String(value || '').replace(/https?:\/\/\S+/g, '[url]')
  .replace(/(?:Bearer\s+|(?:api[-_]?key|token|secret|password)[=:]\s*)[^\s,;]+/gi, '[redacted]').slice(-1800);

export function publicationResult(view, hash) {
  for (const line of String(view?.output || '').trim().split('\n').reverse()) {
    try { const result = JSON.parse(line); if (result.publicationHash === hash) return result; } catch {}
  }
  return null; // ARM can briefly return the preceding command's successful instanceView.
}

export function publicationBatches(snapshot, warmIds = new Set()) {
  const groups = new Map();
  for (const record of snapshot.records) {
    // Keep sibling video evidence together so a subtitle ban cannot be lost at a batch boundary.
    const key = record.media?.kind === 'video' ? 'video:' + (record.media.sha256 || record.media.fileToken || record.id) : 'record:' + record.id;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(record);
  }
  const work = record => warmIds.has(record.id) ? 0 : record.media?.kind === 'video'
    ? Math.max(1, Math.ceil((record.media.endSeconds - record.media.startSeconds) / 2) + 1) : 1;
  const batches = []; let batch = [], cost = 0;
  const pack = groups => {
    const records = groups.flat();
    if (zlib.gzipSync(JSON.stringify({ ...snapshot, records })).length <= MAX_PACKED_BYTES) {
      batches.push(records); return;
    }
    if (groups.length === 1) throw new Error('A media asset group exceeds transport limits');
    const middle = Math.ceil(groups.length / 2);
    pack(groups.slice(0, middle)); pack(groups.slice(middle));
  };
  for (const group of groups.values()) {
    const extra = group.reduce((sum, record) => sum + work(record), 0);
    if (batch.length && cost + extra > MAX_COLD_WORK) {
      pack(batch); batch = []; cost = 0;
    }
    batch.push(group); cost += extra;
  }
  if (batch.length) pack(batch);
  return batches;
}

function azure(args, input) {
  let result = spawnSync("az", args, { input, encoding: "utf8", timeout: 90_000, maxBuffer: 1024 * 1024 });
  if (result.error?.code === 'ENOENT') {
    const value = flag => args[args.indexOf(flag) + 1];
    result = spawnSync('python3', [fileURLToPath(new URL('./arm-command.py', import.meta.url))], {
      input: JSON.stringify({ method: value('--method'), url: value('--url'), body: input, output: value('-o') }),
      encoding: 'utf8', timeout: 90_000, maxBuffer: 1024 * 1024,
    });
  }
  if (result.status !== 0) throw new Error('Azure command failed: ' + safeDetail(result.error?.code || result.stderr || 'nonzero exit'));
  return result.stdout;
}

export async function publishSnapshot(snapshot, { command = azure, wait = ms => new Promise(resolve => setTimeout(resolve, ms)), attempts = 96 } = {}) {
  const raw = Buffer.from(JSON.stringify(snapshot));
  if (snapshot.version !== 1 || !Array.isArray(snapshot.records)) throw new Error("Invalid snapshot");
  const hash = crypto.createHash("sha256").update(raw).digest("hex");
  const parts = zlib.gzipSync(raw).toString("base64").match(/.{1,40000}/g);
  const script = `set -eu
umask 077
exec 9>/var/lib/yixianghui-knowledge/media-publication.lock
flock -n 9 || { echo 'Another media publication owns the remote lock' >&2; exit 75; }
node <<'DECODE'
const fs=require('fs'),z=require('zlib'),c=require('crypto');
const packed=Array.from({length:${parts.length}},(_,i)=>process.env['MEDIA_PART_'+i]).join('');
const bytes=z.gunzipSync(Buffer.from(packed,'base64'));
if(c.createHash('sha256').update(bytes).digest('hex')!=='${hash}')throw Error('Transport checksum mismatch');
fs.writeFileSync('/var/lib/yixianghui-knowledge/media-snapshot.json.next',bytes,{mode:0o600});
fs.renameSync('/var/lib/yixianghui-knowledge/media-snapshot.json.next','/var/lib/yixianghui-knowledge/media-snapshot.json');
DECODE
cd /opt/yixianghui-knowledge
if ${snapshot.planRecords ? 'true' : 'false'}; then
node <<'PLAN'
const fs=require('fs');
const snapshot=JSON.parse(fs.readFileSync('/var/lib/yixianghui-knowledge/media-snapshot.json','utf8'));
let manifest={records:{}};try{manifest=JSON.parse(fs.readFileSync('/var/lib/yixianghui-knowledge/media-manifest.json','utf8'))}catch(e){if(e.code!=='ENOENT')throw e}
const bits=Buffer.alloc(Math.ceil(snapshot.planRecords.length/8));
snapshot.planRecords.forEach((row,i)=>{if(manifest.records[row.id]?.sourceHash===row.sourceHash&&!manifest.failures?.[row.id])bits[i>>3]|=1<<(i%8)});
console.log(JSON.stringify({planned:true,warmBits:bits.toString('base64'),publicationHash:'${hash}'}));
PLAN
exit 0
fi
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
  const previous = JSON.parse(command(['rest', '--method', 'get', '--url', COMMAND_URL + '&$expand=instanceView', '-o', 'json']));
  if (['Running', 'Pending'].includes(previous.properties?.instanceView?.executionState)) throw new Error('Remote media publication is already running; refusing to replace its command');
  const submittedAt = Date.now() - 2000;
  command(["rest", "--method", "put", "--url", COMMAND_URL, "--body", "@/dev/stdin", "-o", "none"], JSON.stringify(body));
  let pollErrors = 0;
  for (let attempt = 0; attempt < attempts; attempt++) {
    await wait(20_000);
    let result;
    try {
      result = JSON.parse(command(["rest", "--method", "get", "--url", COMMAND_URL + "&$expand=instanceView", "-o", "json"]));
      pollErrors = 0;
    } catch (error) {
      if (++pollErrors >= 3) throw error;
      console.log(JSON.stringify({ mediaPublish: 'status_read_retry', attempt: pollErrors }));
      continue; // A transient ARM read failure must not discard a running batch.
    }
    const view = result.properties.instanceView;
    const receipt = publicationResult(view, hash);
    if (view?.executionState === "Succeeded" && view.exitCode === 0) {
      if (!receipt || (view.startTime && Date.parse(view.startTime) < submittedAt)) continue;
      console.log(JSON.stringify({ published: !snapshot.planRecords, planned: !!snapshot.planRecords, records: snapshot.records.length, ...receipt }));
      return receipt;
    }
    if (["Failed", "Canceled", "TimedOut"].includes(view?.executionState)
        && (receipt || Date.parse(view.startTime) >= submittedAt)) throw new Error(`Media publish failed: ${view.executionState}; exit=${view.exitCode}; ` + safeDetail(view.error || view.executionMessage) + '; ' + safeDetail(view.output));
    if (attempt % 3 === 0) console.log(JSON.stringify({ mediaPublish: "running" }));
  }
  throw new Error('Media publish did not finish within the remote execution and status-polling deadline');
}

export async function publishMedia(file, publish = publishSnapshot) {
  const bytes = fs.readFileSync(file);
  const snapshot = JSON.parse(bytes);
  if (!Array.isArray(snapshot.records) || new Set(snapshot.records.map((record) => record.id)).size !== snapshot.records.length) {
    throw new Error("Invalid or duplicate media records");
  }
  if (snapshot.replacement && (snapshot.partial || snapshot.failures?.length || !snapshot.records.length)) {
    throw new Error("Refuse replacement from an incomplete or empty media snapshot");
  }
  const warmIds = new Set();
  if (publish === publishSnapshot) {
    // This is a workload estimate only: warm records are STILL ingested and the final
    // replacement rechecks every source hash and vector. No authority/cache shortcut.
    const plan = await publish({ version: 1, records: [], planRecords: snapshot.records.map(record => ({ id: record.id, sourceHash: sourceHash(record) })) });
    const bits = Buffer.from(plan.warmBits, 'base64');
    if (!plan.planned || bits.length !== Math.ceil(snapshot.records.length / 8)) throw new Error('Invalid media workload plan');
    snapshot.records.forEach((record, i) => { if (bits[i >> 3] & (1 << (i % 8))) warmIds.add(record.id); });
  }
  const batches = publicationBatches(snapshot, warmIds);
  for (let index = 0; index < batches.length; index++) {
    await publish({ ...snapshot, replacement: false, records: batches[index], partial: true,
      refreshText: index === 0 });
  }
  // A separate finalization receipt includes identities and hashes from ALL
  // batches. Never infer full coverage from the last partial batch.
  let final;
  if (snapshot.replacement) final = await publish({ ...snapshot, records: [], partial: false, refreshText: false,
    replacementRecords: snapshot.records.map(record => ({ id: record.id,
      sourceHash: sourceHash(record) })) });
  if (publish === publishSnapshot && snapshot.replacement && final?.replacement !== true) throw new Error('Missing media replacement finalization receipt');
  const result = { mediaPublicationComplete: true, snapshotSha256: crypto.createHash('sha256').update(bytes).digest('hex'),
    finalized: snapshot.replacement === true && final?.replacement === true, retiredSourceIds: final?.retiredSourceIds || [] };
  return result;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) console.log(JSON.stringify(await publishMedia(process.argv[2])));
