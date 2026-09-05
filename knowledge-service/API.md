# 逸享荟知识库接口

## 外部素材索引写入（不上传原文件）

`PUT https://gatheringtech.com/knowledge/assets/{source}/{id}`

使用独立的写入令牌：`Authorization: Bearer <KNOWLEDGE_WRITE_TOKEN>`。现有查询令牌不能写入；每个来源的令牌只能写入/回读自己的 `source`。初始来源为 `external-materials`。

接口同步完成索引：首次写入返回 `201 / created`，更新返回 `200 / updated`，相同记录重试返回 `200 / unchanged`。相同 `source + id` 只占一个索引点。只改链接、访问方式等元数据不重复向量化；更新描述/标题/标签才重新生成向量。
不同来源或不同ID即使原件哈希相同，也分别保留，不自动合并来源或更新权。

```bash
curl --fail-with-body --max-time 160 --request PUT \
  "https://gatheringtech.com/knowledge/assets/$KNOWLEDGE_WRITE_SOURCE/room-001" \
  -H "Authorization: Bearer $KNOWLEDGE_WRITE_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{
    "kind": "image",
    "title": "基地双床客房照片",
    "content": "客房内有两张床，窗外可见花园。用于核对房型与窗外环境。",
    "url": "https://your-storage.example/materials/room-001.jpg",
    "tags": ["客房", "双床", "花园"],
    "updatedAt": "2026-09-06T00:00:00+08:00",
    "media": {"access": "restricted"}
  }'
```

示例原件地址必须替换，`updatedAt` 使用素材索引的真实版本时间。令牌与来源名保存在本机受保护的连接文件中，不应粘进前端代码或Git。

| 字段 | 约束 |
| --- | --- |
| `source`（路径） | 小写字母开头，2–48位字母/数字/下划线/连字符；必须属于此写入令牌 |
| `id`（路径） | 1–128位字母/数字/点/下划线/连字符，字母或数字开头；稳定不变，视频片段可用 `video-001-segment-030` |
| `kind` | 必填：`text`、`image`、`video`、`pdf` |
| `title` / `content` | 必填，标题最多200字符、描述最多2700字符；标题、描述、标签组成的检索文本合计最多3000字符 |
| `url` | 必填，稳定HTTPS原件或来源地址，最多2048字符；禁止嵌入密码、密钥或临时签名 |
| `tags` | 可选，最多20个，每个最多40字符；自动去重、排序 |
| `updatedAt` | 必填，带时区的ISO时间；更新必须晚于现有版本，不接受超前5分钟以上的时间 |
| `checksum` | 可选，调用方提供的原件SHA-256；服务不读取原件核验此哈希 |
| `media.access` | 默认`restricted`，只提供来源链接；确实公开的HTTPS图片/视频直链可设`public`，由浏览器直接加载 |
| `media.fileToken` | 可选，已配置飞书租户的文件标识；文档图片使用docx来源地址，独立文件使用与token对应的file来源地址 |
| `media.startSeconds` / `endSeconds` | 视频片段可选，需成对提供，0≤起点<终点≤864000秒 |
| `media.page` | PDF可选，正整数页码 |

不接收文件、Base64、任意扩展字段，也不自动抓网页或执行图片识别、视频转写。请由素材采集工具提交可搜索的描述、OCR/转写摘要；长内容按片段ID拆开提交，接口不会静默截断。

回读单条索引：`GET /knowledge/assets/{source}/{id}`，使用同一来源的写入令牌，返回规范化的 `asset`、`pointId` 和 `indexedAt`。目前不开放批量删除或DELETE接口。

写入后直接使用已有 `/knowledge/search`、`/knowledge/ask` 查询；结果中的 `asset` 包含来源、素材ID、标签及版本信息。外部索引采用独立 `external_asset` 类型，不冒充语雀来源，也不会被语雀/飞书定时同步或商品同步覆盖。

注意：索引成功只证明提交的文本与元数据已入库，不证明原件链接可访问或内容真实。公开链接预览会连接原件所在站点；私有链接不会由服务器任意下载或携带你的其他凭据访问。已接入的飞书预览继续使用原有保护。

写入错误：`400` 参数/JSON错误，`401` 写令牌错误或来源不匹配，`404` 索引不存在，`409 stale_revision` 版本冲突，`409 write_in_progress` 同一素材正在写入，`413` 请求超过32 KiB，`415` 非JSON文件上传，`429` 写入限流（每来源每分钟60次、进程最多4条并发），`502` 依赖失败或写入结果未确认。
网络中断、`write_in_progress` 或依赖故障后，使用**原来的ID、内容和版本时间**重试；不要为重试生成新ID。版本冲突需要先回读核实，不能盲目刷新时间覆盖。

服务端 `KNOWLEDGE_INGEST_TOKENS` 为“来源→独立密钥”的JSON配置，密钥不能与查询/管理令牌重复。新增其他工具来源时由管理员配置新来源及专属密钥；运行模式保持单个PM2实例，由进程内并发保护串行化同一素材写入。

## 网页测试入口

`https://gatheringtech.com/knowledge/console/` 提供单轮问答、仅检索、引用原文、媒体时间段和原始 JSON 检查。
使用现有 `KNOWLEDGE_API_TOKEN` 登录；登录后换取有效期 8 小时的 HttpOnly / Secure / SameSite=Strict 会话，网页不将令牌保存到 localStorage 或 sessionStorage。
Azure 和 Qdrant 密钥只在服务端。服务重启会使网页会话失效；原有 Bearer API 不受影响。
页面不存储会话历史、不更改业务数据。引用和商品信息来自同步快照，不代表实时价格、库存或媒体回填已全部完成。

已登录测试者可以直接预览命中的飞书原图、播放原视频并跳转引用时间段。网页查询返回 `media.previewUrl`，仅对获得该结果的登录会话有效；图片上限 20 MiB、视频上限 1 GiB，最多保留最近 100 个媒体预览。浏览器可使用 Range 读取视频，媒体不写入服务器磁盘。退出登录、服务重启或预览淘汰后需重新查询。
原件由服务端使用已有飞书应用读取权限转发，不向浏览器下发飞书令牌，也不接受任意 URL/fileToken 下载请求。权限不足、损坏文件、外链素材或浏览器不支持的视频格式会明确显示预览不可用，仍可打开原来源。原有 Bearer `/ask`、`/search` 不产生浏览器会话预览地址。

## 合并问答

`POST https://gatheringtech.com/knowledge/ask`

请求头：`Authorization: Bearer <知识库查询令牌>`、`Content-Type: application/json`。
令牌与原 `/knowledge/search` 相同，不是 Azure 模型密钥。应由调用工具的服务端保管。

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| question | 是 | 字符串，去除首尾空白后 2–500 字符 |
| maxSources | 否 | 引用来源上限，整数 1–10，默认 5；不是检索候选数 |

```bash
curl --fail-with-body --max-time 80 \
  https://gatheringtech.com/knowledge/ask \
  -H "Authorization: Bearer $KNOWLEDGE_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"白拇指玉米的规格和价格是什么？","maxSources":3}'
```

响应字段：

- `answer`：中文答案，引用标记形如 `[S1]`。
- `grounded`：`true` 表示模型选择了支持答案的原文；`false` 表示资料不足，此时 `sources` 为空。不是经过独立事实审核的置信度。
- `sources`：实际引用的来源，含 `id`、`title`、`url`、`quote`（程序从索引直接取出的原文片段，不由模型抄写）、`sourceId`；商品来源还包含 `entityId`、`entityTable`、`productStatus`、`snapshotAt`。
- `model`：使用的 Luna 部署名；没有检索候选、不调用模型时可缺省。
- `retrievedCount`：召回候选数，当前最多 30。
- `usage.inputTokens`、`usage.outputTokens`：这次 Luna 调用的 token 用量，不包含 embedding 用量；无模型调用时省略。

服务依次执行问题 embedding、Qdrant 混合召回、**一次 Luna 调用完成资料筛选和回答**。问答不会再单独调用 rerank。
系统校验引用 ID 及答案内的引用标记，直接返回该来源的原文片段；这不能自动证明每句话都正确，重要业务结论仍需核实。
价格、库存、上下架状态来自同步快照，不能作为实时下单承诺。接口当前为单轮问答，不接收会话历史。

## 仅检索资料

`POST https://gatheringtech.com/knowledge/search`

请求：`{"question":"昆明有哪些旅居基地？","limit":5}`，`limit` 为 1–10，默认 8。
返回 Luna 排序后的资料片段，不生成答案；按 `results[].rank` 使用，`score=null`，`retrievalScore` 为原始召回分数。

## 错误与工具配置

- `400`：问答请求 JSON 或参数无效。
- `401`：查询令牌缺失或错误。
- `403`：公网接口使用了 POST 以外的方法。
- `404`：路径不存在；同步管理接口不开放公网。
- `413`：请求超过公网 64 KiB 限制。
- `429`：上游配额限流；遵守 `Retry-After` 和 `retryAfterSeconds`，不要立即无限重试。
- `502`：Luna 请求失败、输出不完整或引用校验失败；没有静默回退别的模型。
- `500`：其他依赖或服务错误。

在 n8n/Dify 等工具中使用 HTTP 请求节点：POST、上述 URL、Bearer Auth（查询令牌）、JSON 请求体；读取 `answer` 作为回复并展示 `sources`。无需给这些工具 Azure 或 Qdrant 管理密钥。

本机连接文件位于 `/Users/kevin/.codex/yixianghui/connections/gatheringtech-knowledge.env`，权限 600，包含 `KNOWLEDGE_API_TOKEN` 与接口地址。此文件不进入 Git。复制到其他机器时同样限制读取权限。

## 图片、视频与 PDF 来源

多媒体索引使用可观察画面描述、OCR 和视频自动转写形成文字向量；不是图像相似度向量。原件仍保存在飞书/MACE，查询使用原有 `/search` 与 `/ask`。

- `/search` 的 `results[].media`、`/ask` 的 `sources[].media` 包含媒体元数据。
- `media.kind` 为 `image`、`video` 或 `pdf`；`fileToken` 是飞书文件标识，不是访问密钥。
- 视频返回 `startSeconds`、`endSeconds`；当前每 10 秒取关键帧并补片尾，按 30 秒区间索引，可能遗漏短暂画面。自动语音转写可能有识别误差。
- PDF 返回 `page`。目录素材保留 `folderPath`，不通过相似文件名推断其属于某个基地。
- `media.url` 是原媒体或飞书文件链接，源站权限与有效期仍适用；`sourceUrl`/`url` 保留可回查的飞书来源。
- 首轮回填或源站失败时，已完成的记录可用，但不能据此认为媒体全量已经完成；以同步任务覆盖报告和失败清单为准。
