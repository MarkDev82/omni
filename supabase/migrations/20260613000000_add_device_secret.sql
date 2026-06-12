-- Add a secure secret for device-to-API authentication
ALTER TABLE public.devices ADD COLUMN device_secret UUID DEFAULT uuid_generate_v4();
