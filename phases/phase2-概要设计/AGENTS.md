# Phase 2 — 概要设计

> **周期**：2-3 周 | **输入**：SRS V1.1 | **输出物**：系统架构设计文档

---

## 一、系统架构

### 1.1 整体架构图

```
                         ┌──────────┐
                         │  Nginx   │  (443, 反向代理 + HTTPS)
                         └────┬─────┘
                              │
         ┌──────────┬─────────┼─────────┬──────────┐
         │          │         │         │          │
    ┌────▼────┐ ┌───▼───┐ ┌──▼────┐ ┌──▼────┐ ┌───▼────┐
    │ Flutter │ │ Vue 3 │ │Spring │ │Python │ │Wechaty │
    │  App    │ │ Admin │ │ Boot  │ │  CV   │ │  Bot   │
    │ (移动端) │ │(后台) │ │(8080) │ │(9001) │ │(Node)  │
    └─────────┘ └───────┘ └─┬──┬──┘ └───────┘ └────────┘
                            │  │
              ┌─────────────┘  └──────────────┐
              │                               │
    ┌─────────▼──────┐              ┌─────────▼──────┐
    │   PostgreSQL   │              │     Redis      │
    │     (5432)     │              │    (6379)      │
    └────────────────┘              └────────────────┘
         ┌────────────┐              ┌────────────┐
         │   MinIO    │              │   Kafka /   │
         │  (9000)    │              │ Redis Stream│
         └────────────┘              └────────────┘
```

### 1.2 技术选型

| 层 | 选型 | 理由 |
|:---|:---|:---|
| **后端框架** | Spring Boot 3 + Java 17 | 团队主力语言，生态成熟 |
| **安全** | Spring Security + JWT + RBAC | 角色权限模型天然支持 |
| **ORM** | MyBatis-Plus | 复杂查询灵活，分页插件成熟 |
| **数据库** | PostgreSQL 17 | 已安装，JSONB 字段适合健康记录 |
| **缓存** | Redis | Token 黑名单、验证码、行为状态缓存 |
| **消息队列** | Kafka（生产）/ Redis Streams（MVP） | 告警事件异步解耦 |
| **对象存储** | MinIO（自建）→ 阿里云 OSS（生产） | S3 兼容，平滑迁移 |
| **App** | Flutter 3.x | 双端统一，视频播放生态好 |
| **后台** | Vue 3 + Element Plus | 快速开发，组件丰富 |
| **CV 推理** | Python FastAPI + YOLOv8 + OpenCV | GPU 推理微服务 |
| **视频处理** | FFmpeg 子进程 | RTSP 拉流 + 抽帧 |
| **微信机器人** | Wechaty (Node.js/TypeScript) | 社区活跃，Puppet 方案多 |
| **推送** | FCM + APNs + 厂商通道 | Android 国内需厂商通道 |
| **实时通信** | Spring WebSocket + WebRTC | 专家视频咨询 |
| **定时任务** | XXL-JOB | 可视化调度，失败重试 |
| **持续集成** | GitHub Actions | 免费配额足够初期使用 |
| **容器化** | Docker + Docker Compose | 环境统一 |

### 1.3 核心数据模型（ER 概要）

```
t_user 1 ──── N t_pet 1 ──── N t_behavior_event
  │                │                 │
  │                │ N               │ N
  │                ├──────── t_behavior_summary
  │                │
  │                ├──────── t_health_record
  │                │
  │                ├──────── t_feeding_plan (VIP)
  │                │
  │                ├──────── t_behavior_trend (VIP)
  │                │
  │                │ N       1
  │                └──────── t_device
  │
  ├────────── t_vip_order (VIP)
  │
  ├────────── t_alert
  │
  ├────────── N t_consultation 1 ──── t_expert
  │                │
  │                │ N
  │                └──── t_consult_msg
  │
  └────────── (wechat_user_id / push_token)

t_admin ──────── t_audit_log
```

### 1.4 模块划分

```
pet-butler/
├── auth-service          # 认证 + 授权 + JWT
├── user-service          # 用户 CRUD + VIP 状态 + 微信绑定
├── pet-service           # 宠物档案 CRUD
├── device-service        # 摄像头管理 + 拉流调度
├── behavior-service      # 行为事件 + 摘要 + 告警引擎
├── health-service        # 健康记录 CRUD
├── reminder-service      # 提醒配置 + 定时推送
├── feeding-service       # AI 饲养建议（VIP）
├── trend-service         # 趋势计算 + 健康预警（VIP）
├── expert-service        # 专家管理 + 咨询 + 排班
├── news-service          # 资讯管理 + 推荐
├── vip-service           # VIP 订单 + 支付 + 续费
├── storage-service       # MinIO/OSS 文件上传下载
├── notification-service  # 推送统一接口（FCM/APNs/微信）
└── admin-service         # 管理员后台（封禁/配置/统计）
```

---

## 二、接口设计（概要）

| 模块 | 接口前缀 | 主要接口 |
|:---|:---|:---|
| 认证 | `/api/auth` | POST login, POST register, POST refresh-token |
| 用户 | `/api/users` | GET me, PUT me, POST wechat-bind |
| 宠物 | `/api/pets` | CRUD |
| 设备 | `/api/devices` | CRUD + GET status + PUT region |
| 行为 | `/api/behaviors` | GET current, GET timeline, GET summary |
| 健康 | `/api/health-records` | CRUD + filter by type/date |
| 提醒 | `/api/reminders` | GET config, PUT config |
| 饲养 | `/api/feeding` | GET guide, GET daily-advice, POST plan (VIP) |
| 趋势 | `/api/trends` | GET weekly/monthly/quarterly (VIP) |
| 专家 | `/api/experts` | GET list, GET detail, POST consultation (VIP) |
| 资讯 | `/api/news` | GET feed, GET by category, GET search, POST favorite |
| VIP | `/api/vip` | POST order, GET callback, PUT auto-renew |
| 后台 | `/api/admin` | CRUD users, PUT ban, PUT expert-audit, GET stats |

统一返回格式：
```json
{ "code": 200, "message": "success", "data": {} }
```

---

## 三、部署拓扑

```
┌─ 云服务器 1 (应用) ─────────────┐
│  Nginx :443                     │
│  Spring Boot :8080              │
│  Wechaty Bot (Node.js)          │
└─────────────────────────────────┘
┌─ 云服务器 2 (数据) ─────────────┐
│  PostgreSQL :5432               │
│  Redis :6379                    │
│  MinIO :9000                    │
│  Kafka / Redis Streams          │
└─────────────────────────────────┘
┌─ GPU 推理机 (RTX 4090) ────────┐
│  Python CV Service :9001        │
│  FFmpeg 进程池                  │
└─────────────────────────────────┘
```

---

## 四、交付物

- [ ] 系统架构设计文档
- [ ] 数据库 ER 图 + 完整建表 SQL
- [ ] API 接口清单（Knife4j 自动生成）
- [ ] 部署拓扑图
- [ ] 技术选型评审纪要
