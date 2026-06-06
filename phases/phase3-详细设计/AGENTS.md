# Phase 3 — 详细设计

> **周期**：2-3 周 | **输入**：概要设计 | **输出物**：详细设计文档 + 接口规格书

---

## 一、核心接口详细设计

### 1.1 认证模块

```
POST /api/auth/login
  Request:  { phone, password }
  Response: { accessToken, refreshToken, expiresIn }
  
POST /api/auth/register
  Request:  { phone, password, nickname }
  Validate: 密码 >=8位 含字母数字, 手机号格式
  Response: 同 login

POST /api/auth/refresh
  Header:   Authorization: Bearer {refreshToken}
  Response: { accessToken, expiresIn }
  
内部机制：
  - accessToken 2小时, refreshToken 7天
  - JWT payload: { userId, role, iat, exp }
  - Redis 黑名单: token 注销后加入, 过期时间 = token 剩余有效期
  - @AuthCheck(role="VIP") 注解式鉴权
```

### 1.2 设备管理 + 拉流调度

```
POST /api/devices
  Request:  { name, rtspUrl, username, password, petId }
  Process:
    1. 校验 RTSP URL 格式 (rtsp://ip:port/path)
    2. 尝试 RTSP 连接测试 (5秒超时)
    3. 免费版检查已有设备数 <= 1
    4. 写 t_device, status=online
    5. 异步启动 FFmpeg 拉流子进程
  Response: { deviceId, status }

启动 FFmpeg 命令模板:
  ffmpeg -rtsp_transport tcp -i {rtsp_url} 
         -vf "fps={fps}" 
         -s {resolution} 
         -q:v 3
         {output_dir}/dev_{deviceId}_%d.jpg
  参数可配: fps=1/5 (5秒1帧), resolution=1280x720

GET /api/devices/{id}/stream/live
  Response: WebSocket 实时推流 (MJPG over WebSocket)
  实现: 后端转发 RTSP → 转 Base64 JPEG → WebSocket 推送
```

### 1.3 CV 推理服务

```
Python FastAPI :9001

POST /cv/detect
  Request:  { image_base64, device_id, roi_polygon? }
  Process:
    1. Base64 → numpy array
    2. 若配置 ROI, 裁剪至多边形区域
    3. YOLOv8 检测: 定位宠物 bounding box
    4. 分类: ResNet/MediaPipe → 行为标签
    5. 返回结果
  Response: {
    found: true,
    behavior: "eating",
    confidence: 0.92,
    bbox: { x, y, w, h },
    timestamp: "2025-06-04T12:00:00Z"
  }
  
  异常返回:
    found=false: 未检测到宠物 → 启动 missing 倒计时
    confidence<0.5: 低置信度 → 标记为 uncertain
```

### 1.4 行为事件聚合

```
行为状态机:
  [持续检测] → 相同行为跨帧合并 → 行为切换时关闭上一个事件, 开启新事件
  
  事件最小持续时长: 5秒 (过滤抖动)
  视频片段截取: 事件 start_time-2s ~ end_time+2s

Kafka/Redis Streams 消息:
  Topic: behavior-events
  Message: { deviceId, petId, behavior, confidence, timestamp, frameUrl }

消费 → 写入 t_behavior_event
      → 滑动窗口判异 → 是否触发告警
      → 聚合到 t_behavior_summary (每分钟更新)
```

### 1.5 异常告警链路

```
判定规则（可配置）:
  1. 连续30分钟未检测到 eating → "进食异常"
  2. 连续2小时未检测到 drinking → "饮水异常"  
  3. 连续4小时未检测到 exercising → "运动异常"
  4. 连续10分钟未检测到宠物 → "宠物失踪"

告警链路:
  behavior-event → 滑动窗口判异 → 触发 → 写 t_alert
  → notification-service
    → 1) FCM/APNs 推送 (实时)
    → 2) 微信 WebSocket 推送 (5分钟后未读)
    → 3) 短信 (15分钟后未读, VIP only)

推送消息模板:
  标题: {pet_name}{alert_type}
  内容: {pet_name}已{alert_condition_description}, 点击查看详情
  DeepLink: petbutler://alert/{alert_id}
```

### 1.6 每日报告生成

```
定时: XXL-JOB cron "0 0 22 * * ?" (每晚22:00)
触发条件: 该宠物当日至少有3条行为事件

流程:
  1. 查询 t_behavior_summary WHERE date = today AND pet_id = {id}
  2. 查询昨日摘要, 计算日环比
  3. 查询宠物信息: {品种, 年龄, 体重}
  4. 组装提示词 → 调 LLM API
  5. 解析 LLM 返回的 JSON → 写 t_daily_report
  6. 调 notification-service 推双通道

LLM 提示词模板:
"""
你是AI宠物管家。根据以下数据生成一份温馨的每日报告。
宠物信息：{name}, {breed}, {age}个月, {weight}kg
今日行为：进食{eat_count}次, 饮水{drink_count}次, 
          运动{exercise_min}分钟, 睡眠{sleep_min}分钟, 排泄{defecate_count}次
昨日对比：进食{delta}次, 运动{delta}分钟...
输出JSON格式：
{ "summary": "一句话总结", "highlights": ["亮点1","亮点2"], 
  "suggestion": "明日建议", "mood_score": 85 }
"""
```

### 1.7 专家咨询

```
WebSocket 消息格式:
  { "type": "text|image|video_offer|video_answer|ice_candidate|system",
    "payload": { ... },
    "senderId": "xxx",
    "timestamp": 1717500000000 }

信令流程:
  用户 → 发起咨询 → 服务端 → 推送给专家
  专家 → 接单 → 双向 WebSocket 通道建立
  文字/图片: 直接通过 WebSocket JSON 传输
  视频通话: WebRTC 
    - 用户端 createOffer → 专家端 setRemote + createAnswer
    - ICE candidates 通过 WebSocket 转发
    - STUN 服务器: Google STUN (stun:stun.l.google.com:19302)
    - TURN 服务器: 自建 coturn (生产环境)

费用计算:
  start_time = 专家接单时间
  end_time = 任一方点击"结束咨询"
  duration = end_time - start_time
  fee = rate_per_min * ceil(duration / 60)
  用户确认支付 → 平台分成 (config: platform_commission_rate)
```

---

## 二、安全设计

| 层 | 措施 |
|:---|:---|
| 传输 | 全站 HTTPS + WebSocket WSS |
| 认证 | JWT + Token 刷新 + Redis 黑名单 |
| 授权 | Spring Security @PreAuthorize + RBAC |
| 数据 | 敏感字段 AES-256 加密 (phone, email, rtsp_password) |
| 支付 | 回调验签, 金额服务端计算, 幂等防重 |
| 文件 | 上传白名单 (jpg/png/mp4), 病毒扫描 |
| 限流 | 登录/注册/短信接口: Redis + Lua 令牌桶 |
| 审计 | 管理员所有操作 + 支付操作 → t_audit_log |

---

## 三、交付物

- [ ] API 详细接口文档 (Knife4j + Markdown)
- [ ] 核心流程时序图（登录、拉流、告警、支付、咨询）
- [ ] 数据库完整 DDL + 索引策略
- [ ] 安全设计文档
- [ ] LLM 提示词模板库
