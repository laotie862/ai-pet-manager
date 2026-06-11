# AI 宠物管家 (AI Pet Manager)

基于 AI 计算机视觉的智能宠物监护与管理平台。通过摄像头/视频源实时识别宠物行为（进食、饮水、运动、睡眠等），提供健康监测、异常告警、数据分析等一站式服务。

---

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端 | Spring Boot 4.0.6 + Java 25 |
| 安全 | Spring Security + JWT |
| 数据库 | PostgreSQL 16 + Flyway 迁移 |
| 缓存 | Redis 7 (Session/状态缓存) |
| 前端 | Vue 3 + TypeScript + Pinia + Vite |
| CV服务 | Python FastAPI + OpenCV |
| 模型训练 | PyTorch 2.x + TorchVision (ResNet) |
| 容器化 | Docker + Compose (5服务编排) |

## 架构

Frontend (Vue 3 :8081) ↔ Backend (Spring Boot :8080) ↔ PostgreSQL + Redis
                                                                   ↕
                                                          CV Service (FastAPI :8000)

## 功能

- **设备管理**: RTSP摄像头/本地摄像头/视频文件模拟接入，WebSocket实时推流
- **宠物管理**: 档案管理、身份识别（CV嵌入向量匹配）
- **AI行为识别**: 进食/饮水/运动/睡眠/排泄/异常 6类行为实时检测
- **告警系统**: 行为异常检测、设备离线告警、自定义规则
- **数据分析**: 行为统计报表、健康趋势

## 项目结构

```
├── backend/          Spring Boot 后端 (Auth/Pet/Device/Behavior/Storage)
├── frontend/         Vue 3 前端 (6个页面 + API层 + Pinia状态)
├── cv-service/       Python CV分析服务 (FastAPI + OpenCV)
├── training/         PyTorch训练管道 (train.py/dataset.py/evaluate.py)
├── docs/             变更管理文档
├── diagrams/         PlantUML设计图
├── phases/           GB8567标准文档
├── scripts/          运维脚本
├── dataset/          行为样本数据集
└── docker-compose.yml
```

## 快速开始

```bash
git clone https://github.com/laotie862/ai-pet-manager.git
cd ai-pet-manager
docker compose up -d
# 前端 http://localhost:8081 | API http://localhost:8080 | CV http://localhost:8000
```

## 分支

| 分支 | 内容 |
|------|------|
| master | 基础后端 + 文档 |
| feature/backend-updates | 后端增强(样本采集/帧检测) |
| feature/backend-enhancements-2 | 宠物身份/CV嵌入/设备自启 |
| feature/frontend | Vue3前端 |
| feature/cv-service | CV分析服务 |
| feature/deploy | Docker编排 |
| feature/documents | 变更管理文档 |
| feature/training | 训练管道+数据集 |

## License

MIT
