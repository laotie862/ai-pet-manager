CREATE INDEX IF NOT EXISTS idx_behavior_event_device_time ON t_behavior_event (device_id, started_at DESC);

ALTER TABLE t_behavior_event ADD COLUMN IF NOT EXISTS model_version VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_behavior_summary_date ON t_behavior_summary (summary_date DESC);
