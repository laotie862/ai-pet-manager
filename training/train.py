"""训练入口"""
import argparse
import yaml

def train(config):
    print(f"Training with config: {config}")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default="config.yaml")
    args = parser.parse_args()
    with open(args.config) as f:
        config = yaml.safe_load(f)
    train(config)

if __name__ == "__main__":
    main()
