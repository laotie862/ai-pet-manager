ALTER TABLE t_device DROP CONSTRAINT IF EXISTS t_device_pet_id_fkey;
ALTER TABLE t_device
    ADD CONSTRAINT t_device_pet_id_fkey
    FOREIGN KEY (pet_id) REFERENCES t_pet(id) ON DELETE CASCADE;

ALTER TABLE t_behavior_event DROP CONSTRAINT IF EXISTS t_behavior_event_pet_id_fkey;
ALTER TABLE t_behavior_event
    ADD CONSTRAINT t_behavior_event_pet_id_fkey
    FOREIGN KEY (pet_id) REFERENCES t_pet(id) ON DELETE CASCADE;

ALTER TABLE t_behavior_event DROP CONSTRAINT IF EXISTS t_behavior_event_device_id_fkey;
ALTER TABLE t_behavior_event
    ADD CONSTRAINT t_behavior_event_device_id_fkey
    FOREIGN KEY (device_id) REFERENCES t_device(id) ON DELETE SET NULL;

ALTER TABLE t_behavior_summary DROP CONSTRAINT IF EXISTS t_behavior_summary_pet_id_fkey;
ALTER TABLE t_behavior_summary
    ADD CONSTRAINT t_behavior_summary_pet_id_fkey
    FOREIGN KEY (pet_id) REFERENCES t_pet(id) ON DELETE CASCADE;

ALTER TABLE t_behavior_sample DROP CONSTRAINT IF EXISTS t_behavior_sample_pet_id_fkey;
ALTER TABLE t_behavior_sample
    ADD CONSTRAINT t_behavior_sample_pet_id_fkey
    FOREIGN KEY (pet_id) REFERENCES t_pet(id) ON DELETE CASCADE;

ALTER TABLE t_behavior_sample DROP CONSTRAINT IF EXISTS t_behavior_sample_device_id_fkey;
ALTER TABLE t_behavior_sample
    ADD CONSTRAINT t_behavior_sample_device_id_fkey
    FOREIGN KEY (device_id) REFERENCES t_device(id) ON DELETE SET NULL;

ALTER TABLE t_behavior_sample DROP CONSTRAINT IF EXISTS t_behavior_sample_event_id_fkey;
ALTER TABLE t_behavior_sample
    ADD CONSTRAINT t_behavior_sample_event_id_fkey
    FOREIGN KEY (event_id) REFERENCES t_behavior_event(id) ON DELETE SET NULL;

ALTER TABLE t_alert DROP CONSTRAINT IF EXISTS t_alert_pet_id_fkey;
ALTER TABLE t_alert
    ADD CONSTRAINT t_alert_pet_id_fkey
    FOREIGN KEY (pet_id) REFERENCES t_pet(id) ON DELETE CASCADE;
