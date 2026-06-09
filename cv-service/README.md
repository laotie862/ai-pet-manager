# CV Service

FastAPI 行为识别服务。当前定位是项目的视觉中枢：接收后端抽帧图片，返回宠物行为标签；同时为后续样本沉淀和本地模型训练保留稳定接口。

当前提供两个接口：

```text
GET  /cv/health
POST /cv/detect
```

`/cv/detect` 当前是确定性占位分类器：会解码后端传来的 JPEG，按画面亮度和颜色给出一个稳定的行为标签。后续会扩展为 provider 机制，接口保持不变。

支持的 provider：

```text
placeholder  本地占位分类，方便后端联调和演示
bailian      调阿里云百炼 / DashScope Qwen-VL 视觉模型
local        后续使用本地训练好的 ResNet50/YOLO/视频行为模型推理
```

固定行为标签：

```text
eating
drinking
exercising
sleeping
defecating
uncertain
```

后端期望的请求：

```json
{
  "image_base64": "...",
  "device_id": "1",
  "roi_polygon": []
}
```

响应：

```json
{
  "found": true,
  "behavior": "eating",
  "confidence": 0.82,
  "boxes": [],
  "model_version": "local-placeholder-2026-06-09"
}
```

## 持续训练闭环

短期不追求“一边识别一边实时训练模型”，而是采用更稳的闭环：

```text
抽帧图片
  -> AI API / 本地模型识别
  -> 返回标签和置信度
  -> 保存图片、标签、置信度、设备ID、宠物ID、时间
  -> 高置信度样本进入训练候选集
  -> 低置信度样本进入人工确认
  -> 定期训练本地模型
  -> 新模型评估通过后切换到 local provider
```

这样可以避免错误标签被模型反复学习。AI API 前期更像“老师模型”，负责帮助项目快速跑通识别和积累数据；本地模型后期再逐步替代 API。

训练数据目录约定：

```text
dataset/behavior-classifier/eating/
dataset/behavior-classifier/drinking/
dataset/behavior-classifier/exercising/
dataset/behavior-classifier/sleeping/
dataset/behavior-classifier/defecating/
dataset/behavior-classifier/uncertain/
```

下一步优先实现：

```text
1. 保存识别样本元数据
2. 增加样本确认/修正流程
3. 定期训练本地行为分类模型
```

## 阿里云百炼识别

使用百炼视觉识别时，`.env` 配置：

```text
CV_PROVIDER=bailian
BAILIAN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
BAILIAN_MODEL=qwen3-vl-plus
BAILIAN_API_KEY=你的百炼 API Key
```

也可以使用 DashScope 官方环境变量名：

```text
DASHSCOPE_API_KEY=你的百炼 API Key
```

`/cv/detect` 会把后端传入的抽帧图片转成 JPEG Base64 data URL，调用百炼 OpenAI 兼容接口 `/chat/completions`，并要求模型只返回：

```json
{
  "found": true,
  "behavior": "eating",
  "confidence": 0.82
}
```

如果未配置 API Key、调用失败或模型返回格式无法解析，服务会返回 `found=false`、`behavior=uncertain`、`confidence=0.0`，保证后端主链路不中断。
