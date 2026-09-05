# 逸享荟知识库接口

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
