import http from "node:http";
import { pathToFileURL } from "node:url";
import { LunaReranker, LunaRerankError } from "./luna.mjs";
import { answerQuestion, QuestionInputError } from "./answer.mjs";
import { createConsoleHandler } from "./console.mjs";
import { openFeishuMedia } from "./preview.mjs";
import {
  AzureModels,
  FeishuSource,
  KnowledgeService,
  QdrantStore,
} from "./service.mjs";

function required(name) {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`missing environment variable: ${name}`);
  return value;
}

function config() {
  const apiKey = required("AZURE_OPENAI_KEY");
  return {
    port: Number(process.env.PORT ?? 3210),
    apiToken: required("KNOWLEDGE_API_TOKEN"),
    adminToken: required("KNOWLEDGE_ADMIN_TOKEN"),
    source: {
      appId: required("FEISHU_APP_ID"),
      appSecret: required("FEISHU_APP_SECRET"),
      folderToken: required("FEISHU_FOLDER_TOKEN"),
    },
    models: {
      baseUrl: required("AZURE_OPENAI_BASE_URL"),
      apiKey,
      apiVersion: required("AZURE_OPENAI_API_VERSION"),
      embeddingModel: process.env.AZURE_EMBEDDING_MODEL ?? "text-embedding-3-large",
    },
    rerank: {
      url: required("LUNA_RERANK_URL"),
      apiKey: required("LUNA_RERANK_KEY"),
      model: required("LUNA_RERANK_MODEL"),
    },
    qdrant: {
      url: process.env.QDRANT_URL ?? "http://127.0.0.1:6333",
      apiKey: process.env.QDRANT_API_KEY ?? "",
      collection: process.env.QDRANT_COLLECTION ?? "yixianghui_travel_kb",
      dimensions: Number(process.env.EMBEDDING_DIMENSIONS ?? 3072),
    },
    manifestFile: process.env.MANIFEST_FILE ?? "/var/lib/yixianghui-knowledge/manifest.json",
  };
}

async function readBody(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > 64 * 1024) throw new Error("request body exceeds 64 KiB");
    chunks.push(chunk);
  }
  return chunks.length ? JSON.parse(Buffer.concat(chunks).toString("utf8")) : {};
}

function reply(response, status, body) {
  response.writeHead(status, { "content-type": "application/json; charset=utf-8" });
  response.end(`${JSON.stringify(body)}\n`);
}

function authorized(request, token) {
  return request.headers.authorization === `Bearer ${token}`;
}

export function createServer(settings = config()) {
  const store = new QdrantStore(settings.qdrant);
  const service = new KnowledgeService({
    source: new FeishuSource(settings.source),
    models: new AzureModels(settings.models),
    reranker: new LunaReranker(settings.rerank),
    store,
    manifestFile: settings.manifestFile,
  });
  let syncing = null;
  const consoleHandler = createConsoleHandler({ token: settings.apiToken,
    openMedia: (descriptor, options) => openFeishuMedia(service.source, descriptor, options),
    origin: process.env.KNOWLEDGE_CONSOLE_ORIGIN ?? "https://gatheringtech.com",
    query: async ({ mode, question, limit }) => mode === "ask"
      ? answerQuestion(service, question, limit)
      : { question, rerankModel: settings.rerank.model, scoreType: "rank_only", results: await service.search(question, limit) },
  });
  return http.createServer(async (request, response) => {
    try {
      if (await consoleHandler(request, response)) return;
      if (request.method === "GET" && request.url === "/health") {
        return reply(response, 200, { ok: true, qdrant: await store.health() });
      }
      if (request.method === "POST" && request.url === "/search") {
        if (!authorized(request, settings.apiToken)) return reply(response, 401, { error: "unauthorized" });
        const body = await readBody(request);
        const results = await service.search(body.question, Number(body.limit ?? 8));
        return reply(response, 200, {
          question: body.question, rerankModel: settings.rerank.model, scoreType: "rank_only", results,
        });
      }
      if (request.method === "POST" && request.url === "/ask") {
        if (!authorized(request, settings.apiToken)) return reply(response, 401, { error: "unauthorized" });
        const body = await readBody(request);
        if (!body || typeof body !== "object" || Array.isArray(body)) throw new QuestionInputError("Expected a JSON object");
        return reply(response, 200, await answerQuestion(service, body.question, body.maxSources ?? 5));
      }
      if (request.method === "POST" && request.url === "/admin/sync") {
        if (!authorized(request, settings.adminToken)) return reply(response, 401, { error: "unauthorized" });
        if (syncing) return reply(response, 409, { error: "sync already running" });
        syncing = service.sync().finally(() => { syncing = null; });
        return reply(response, 200, await syncing);
      }
      return reply(response, 404, { error: "not found" });
    } catch (error) {
      console.error(new Date().toISOString(), error.message);
      if (error instanceof QuestionInputError || error instanceof SyntaxError) {
        return reply(response, 400, { error: error.message });
      }
      if (error instanceof LunaRerankError) {
        if (error.retryAfter) response.setHeader("retry-after", String(error.retryAfter));
        return reply(response, error.statusCode, {
          error: error.message, retryAfterSeconds: error.retryAfter,
        });
      }
      return reply(response, 500, { error: error.message });
    }
  });
}

// PM2 uses its loader as argv[1]; pm_exec_path identifies the actual entrypoint.
const entrypoint = process.env.pm_exec_path ?? process.argv[1];
if (entrypoint && import.meta.url === pathToFileURL(entrypoint).href) {
  const settings = config();
  createServer(settings).listen(settings.port, "127.0.0.1", () => {
    console.log(`knowledge service listening on 127.0.0.1:${settings.port}; reranker=${settings.rerank.model}`);
  });
}
