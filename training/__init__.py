# 模型训练代码
"""
AI宠物管家 — 行为识别模型训练模块

功能：
- 从后端数据库提取行为样本
- 图像预处理与数据增强
- 模型训练（分类/检测）
- 评估与导出

使用：
  python train.py --config config.yaml
"""

TRAIN_SPLIT = 0.8
VAL_SPLIT = 0.1
TEST_SPLIT = 0.1

BEHAVIOR_CLASSES = [
    "eating",
    "drinking",
    "running",
    "sleeping",
    "eliminating",
    "abnormal",
]
