-- Migration to add device alias support
ALTER TABLE public.devices
ADD COLUMN IF NOT EXISTS alias TEXT;
