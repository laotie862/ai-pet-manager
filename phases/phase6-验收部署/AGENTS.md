# Phase 6 — 验收部署

> **周期**：2-3 周 | **输入**：测试通过的系统 | **输出物**：生产环境 + 运维手册

---

## 一、验收标准

### 1.1 功能验收（按角色）

| 角色 | 用例数 | 验收方式 |
|:---|:---:|:---|
| 游客 | 3 | 手动走查 |
| 普通用户 | 33 | 用例逐条验收 |
| VIP 用户 | 11 | 用例逐条验收 + 支付实测 |
| 管理员 | 16 | 用例逐条验收 |

**核心用例必须 100% 通过**：
- UC-CA-01 添加摄像头
- UC-A-04 异常行为告警
- UC-V-03 AI 定制饲养
- UC-V-08 宠物专家咨询
- UC-R-02 双通道推送链路

### 1.2 非功能验收

| 指标 | 目标 | 实测 |
|:---|:---|:---|
| 视频端到端延迟 | ≤ 5 秒 | |
| 告警推送延迟 | ≤ 10 秒 | |
| CV 行为识别准确率 | ≥ 85% | |
| App 首屏加载 | ≤ 3 秒 | |
| 接口 P95 响应 | ≤ 500ms | |
| 并发摄像头 | ≥ 50 路稳定 | |
| 系统可用性 | 99.9% | |
| 安全扫描 | 无 P0/P1 漏洞 | |

---

## 二、部署方案

### 2.1 生产环境配置

| 服务 | 实例 | 配置 |
|:---|:---:|:---|
| Nginx | 1 | 4C8G, HTTPS + 负载均衡 |
| Spring Boot | 2 | 4C8G each |
| PostgreSQL | 1 | 8C16G + 500GB SSD, 主从 |
| Redis | 1 | 4C8G + 哨兵 |
| MinIO | 1 | 4C8G + 1TB |
| Kafka | 3 节点 | 4C8G each |
| Python CV Service | 1+ | GPU RTX 4090 + 24GB |
| Wechaty Bot | 1 | 2C4G |

### 2.2 部署流程

```
1. 服务器初始化
   - 安装 Docker, Docker Compose
   - 配置 SSH, 防火墙规则
   - 挂载数据盘

2. CI/CD 配置
   - GitHub Actions workflow:
     push phase4/main → build → test → docker build → push registry
     trigger tag v* → deploy staging
     manual trigger → deploy production

3. 数据库初始化
   - Flyway 执行所有 migration SQL
   - 初始化管理员账号
   - 导入品种百科基础数据

4. 服务启动顺序
   PostgreSQL, Redis → MinIO, Kafka → 
   Spring Boot → Python CV → Wechaty Bot → Nginx

5. 健康检查
   GET /actuator/health → {"status":"UP"}
   GET /cv/health → {"status":"ok"}
```

### 2.3 Docker Compose 生产示例

```yaml
version: '3.8'
services:
  nginx:
    image: nginx:alpine
    ports: ["443:443"]
    volumes: [./nginx.conf:/etc/nginx/nginx.conf]
    
  postgres:
    image: postgres:17
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: [pgdata:/var/lib/postgresql/data]
    deploy:
      resources: { limits: { memory: 8G } }
    
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    volumes: [minio-data:/data]
    
  api-1: &api
    image: pet-butler:latest
    environment: [SPRING_PROFILES_ACTIVE=prod]
    
  api-2:
    <<: *api  # 第二实例
    
  cv-service:
    image: pet-cv:latest
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
              
  wechaty:
    image: pet-wechaty:latest
    restart: always

volumes: { pgdata:, minio-data: }
```

---

## 三、上线清单

### 3.1 上线前

- [ ] 全量测试报告签字确认
- [ ] 安全扫描报告 (无 P0/P1)
- [ ] 数据库备份策略确认 (每日自动 + 手动)
- [ ] 监控告警配置 (Prometheus + Grafana + 钉钉/企微通知)
- [ ] 日志采集就绪 (ELK / 阿里云 SLS)
- [ ] 第三方服务确认: 推送证书有效, 支付商户号正确, LLM API Key
- [ ] 苹果审核材料准备 (App Store)
- [ ] 灰度方案: 先开 50 个内测用户 → 观察 1 周 → 全量
- [ ] 回滚方案: 保留上一个稳定版本 Docker Image

### 3.2 上线中

- [ ] 数据库 migration 执行
- [ ] 服务滚动更新 (先停 1 台 → 更新 → 验证 → 再停另一台)
- [ ] 健康检查通过
- [ ] 监控指标正常 (QPS、错误率、延迟)
- [ ] 推送通道验证 (各发一条测试)
- [ ] 微信机器人登录正常

### 3.3 上线后

- [ ] 持续监控 48 小时
- [ ] 用户反馈渠道开通 (客服入口)
- [ ] 首日数据: 注册数、设备接入数、告警量
- [ ] 制定值班表 (前两周 7×24h)

---

## 四、运维手册要点

| 项 | 内容 |
|:---|:---|
| 服务重启 | `docker-compose restart api` |
| 日志查看 | `docker logs -f --tail 200 pet-butler-api-1` |
| 数据库备份 | `pg_dump -U postgres pet_butler > backup.sql` |
| CV 模型更新 | 替换 models/ 下的 .pt 文件, `docker-compose restart cv-service` |
| 紧急回滚 | `docker-compose down && docker-compose up -d` (用上一个版本 tag) |
| 磁盘清理 | MinIO 过期的视频片段自动清理 (lifecycle policy) |
| 常见告警处理 | GPU OOM → 减少并发路数 / CPU 飙高 → 查慢 SQL / 推送失败率 → 查 FCM Token |

---

## 五、交付物

- [ ] 生产环境部署架构图
- [ ] Docker Compose 生产配置文件
- [ ] Nginx 配置
- [ ] CI/CD Pipeline 配置文件
- [ ] 数据库 Migration SQL 全集
- [ ] 运维手册
- [ ] 监控 Dashboard 截图
- [ ] 上线验收签收单
