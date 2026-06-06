# Phase 4 — 编码

> **输入**：详细设计文档 | **输出物**：可运行系统源码

编码分 4 个子阶段，按优先级递进。

---

## 4-1 MVP 验证（4-6周 | 2人）

**目标**：跑通"摄像头→AI→行为时间线"核心闭环。

### 编码任务

| 模块 | 任务 | 技术要点 |
|:---|:---|:---|
| 后端骨架 | Spring Boot 3 初始化 | pom.xml, application.yml, SecurityConfig |
| 用户 | 注册 + 登录 + JWT | BCrypt, Token 双令牌 |
| 宠物 | CRUD | 1 只限制, 照片上传 MinIO |
| 设备 | RTSP 添加/移除 | 连接测试, 状态管理 |
| FFmpeg | 拉流 + 抽帧 | 每设备一个子进程管理, 断连重试 |
| CV 服务 | Python FastAPI + YOLOv8 | 宠物检测 + 5 类行为分类 |
| 行为事件 | 事件聚合 + 存储 | 去抖动, 视频片段截取 |
| Web 页 | Vue3 简易管理后台 | 登录/宠物/设备/时间线页 |
| 部署 | Docker Compose | 一键启动全部服务 |

### 包结构

```
pet-butler-server/
├── src/main/java/com/petbutler/
│   ├── PetButlerApplication.java
│   ├── config/              # Security, JPA, Redis, CORS, MinIO
│   ├── common/              # Result, BusinessException, 常量
│   ├── auth/                # Login, Register, JWT
│   │   ├── controller/AuthController.java
│   │   ├── service/
│   │   ├── dto/
│   │   └── security/
│   ├── user/                # 用户模块
│   ├── pet/                 # 宠物模块
│   ├── device/              # 设备管理 + FFmpeg 调度
│   ├── behavior/            # 行为事件 + 摘要
│   └── storage/             # MinIO 文件服务
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/migration/        # Flyway SQL
└── pom.xml

cv-inference/
├── app/
│   ├── main.py              # FastAPI 入口
│   ├── detector.py          # YOLOv8
│   ├── classifier.py        # 行为分类
│   └── config.py
├── models/                  # .pt / .onnx
├── requirements.txt
└── Dockerfile
```

### 编码规范

- Java: 阿里巴巴规范, Lombok(@Data, @Slf4j), 接口文档写 Knife4j 注解
- Python: PEP8, Type Hints, FastAPI Pydantic Models
- Git commit: `[模块] 描述` 如 `[device] 实现RTSP拉流子进程管理`

### 验收标准

- [ ] 注册 → 登录 → 加宠物 → 加摄像头 → 看到行为时间线
- [ ] CV 模型 5 类行为识别准确率 > 80%
- [ ] Docker Compose 一键启动全部服务

---

## 4-2 基础版上线（6-8周 | 5人）

**目标**：普通用户全功能可用，App + 微信机器人上线内测。

### 编码任务

| 模块 | 任务 | 技术要点 |
|:---|:---|:---|
| App | Flutter 双端 | Dio, ijkplayer, Provider |
| 用户 | 完善资料 + 头像 + 找回密码 | 邮箱/手机验证码 |
| 设备 | 监控区域配置 | 多边形编辑组件 |
| 健康 | 疫苗/驱虫/体检/用药 CRUD | JSONB 动态字段 |
| 饲养 | 通用指南 + 每日建议 | LLM 生成 |
| 告警 | 异常检测引擎 | Redis Streams + 滑动窗口 |
| 推送 | FCM/APNs/微信三通道 | 统一 PushService 接口 |
| 微信 | Wechaty 机器人 | 绑定/日报/"看看"命令 |
| 报告 | 日报定时生成 | XXL-JOB + LLM |
| 后台 | 用户管理/封禁/数据看板 | Vue3 + Element Plus + ECharts |
| 提醒 | 健康到期 + 生日 + 免打扰 | 定时扫描 + 推送 |

### App 页面结构

```
lib/
├── main.dart
├── pages/
│   ├── login/              # UC-U-01
│   ├── register/           # UC-T-03
│   ├── home/               # 首页: 当前行为卡片 + 快速入口
│   ├── behavior/           # UC-A-01~03 实时状态 + 时间线
│   ├── camera/             # UC-CA-03 实时画面
│   ├── device/             # UC-CA-01~02 设备管理
│   ├── pet/                # UC-P-01~04 宠物档案
│   ├── health/             # UC-H-01~06 健康记录
│   ├── feeding/            # UC-F-01~02 AI 饲养建议
│   ├── reminder/           # UC-R-05 提醒设置
│   └── profile/            # UC-U-03~04 个人中心
├── services/               # API 调用层
├── models/                 # 数据模型
├── providers/              # 状态管理
└── widgets/                # 通用组件
```

### 验收标准

- [ ] App 双端打包 + 上传内测分发
- [ ] 微信机器人正常收发消息
- [ ] 异常告警 < 10秒
- [ ] 28 个普通用户用例 + 10 个管理员用例全部通过

---

## 4-3 VIP 商业化（4-6周 | 3人）

**目标**：付费闭环 + 专家咨询上线。

### 编码任务

| 模块 | 任务 | 技术要点 |
|:---|:---|:---|
| VIP 支付 | 微信/支付宝对接 | 统一下单, 回调验签, 幂等 |
| 续费 | 自动扣款 | 到期前3天检查, 扣款通知 |
| 趋势 | 周/月/季预计算 | fl_chart 渲染, 体重曲线 |
| 健康预警 | 趋势异常检测 | 连续下降/上升规则 + LLM |
| 定制饲养 | 动态喂食计划 | LLM 结构化输出 |
| PDF | 报告导出 | Apache PDFBox + ECharts 截图 |
| 视频 | 云端30天存储 | MinIO 生命周期策略 |
| 多宠 | 不限数量 + 对比 | 多线图表 |
| 提醒 | 阈值自定义 | 用户个性化配置 |
| 专家咨询 | 文字/图片/视频 | WebSocket 信令 + WebRTC |
| 专家后台 | 审核/排班/记录 | 资质审核流程 |

### 验收标准

- [ ] 支付 → 回调 → VIP 生效全流程
- [ ] 11 个 VIP 用例 + 3 个专家管理用例通过

---

## 4-4 内容精细化（3-4周 | 2人）

**目标**：资讯模块 + 体验优化上线。

### 编码任务

| 模块 | 任务 | 技术要点 |
|:---|:---|:---|
| 资讯流 | 推荐 + 分类 + 搜索 | AI 个性化推荐 |
| 百科 | 品种知识库 | 分类浏览 |
| 收藏 | 资讯收藏 + 回顾 | 离线缓存 |
| 内容后台 | 文章 CRUD + 发布 | 编辑器 + 排期发布 |
| 推送优化 | 频率控制 + 轰炸保护 | 每日上限 |
| 性能 | 压测 + GC 调优 | JMeter + Arthas |
| CV 迭代 | 模型 fine-tune | 根据内测数据提升 |

---

## 通用编码规范

- 统一返回体: `Result<T> { code, message, data }`
- 异常码枚举: `ErrorCode.USER_NOT_FOUND(1001, "用户不存在")`
- 分页: MyBatis-Plus Page + 统一 PageVO
- 日志: `log.info("[{}] 操作 - {}", module, detail)`，禁止 log 敏感信息
- Git 分支: `phase{N}/main` + `phase{N}/dev` + `feature/{module-name}`
