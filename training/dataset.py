"""数据集加载与预处理"""
import os
from torch.utils.data import Dataset

class PetBehaviorDataset(Dataset):
    def __init__(self, root_dir, transform=None):
        self.root_dir = root_dir
        self.transform = transform
        self.samples = []
        self._load_samples()

    def _load_samples(self):
        for label in os.listdir(self.root_dir):
            label_dir = os.path.join(self.root_dir, label)
            if not os.path.isdir(label_dir):
                continue
            for fname in os.listdir(label_dir):
                self.samples.append((os.path.join(label_dir, fname), label))

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        path, label = self.samples[idx]
        return path, label
