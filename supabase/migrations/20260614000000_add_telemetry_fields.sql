-- Migration to add advanced telemetry fields to device_states

ALTER TABLE public.device_states
ADD COLUMN IF NOT EXISTS network_type TEXT,
ADD COLUMN IF NOT EXISTS wifi_ssid TEXT,
ADD COLUMN IF NOT EXISTS screen_on BOOLEAN,
ADD COLUMN IF NOT EXISTS speed_mps DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS uptime_seconds BIGINT;
