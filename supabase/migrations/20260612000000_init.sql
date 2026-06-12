-- Init script for Omni Device Recovery

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table: devices
CREATE TABLE public.devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    fcm_token TEXT,
    model_name TEXT,
    os_version TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- Table: device_states
CREATE TABLE public.device_states (
    device_id UUID PRIMARY KEY REFERENCES public.devices(id) ON DELETE CASCADE,
    battery_level INTEGER CHECK (battery_level >= 0 AND battery_level <= 100),
    is_charging BOOLEAN DEFAULT false,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    accuracy_meters DOUBLE PRECISION,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- Table: action_requests
CREATE TYPE public.action_status AS ENUM ('PENDING', 'DELIVERED', 'COMPLETED', 'FAILED');
CREATE TYPE public.action_type AS ENUM ('LOCATE', 'RING');

CREATE TABLE public.action_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    action_type public.action_type NOT NULL,
    status public.action_status DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- Table: enrollment_codes
CREATE TABLE public.enrollment_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    code_hash TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- Table: audit_logs
CREATE TABLE public.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_id UUID REFERENCES public.devices(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    ip_address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- Realtime Config
ALTER PUBLICATION supabase_realtime ADD TABLE public.device_states;

-- RLS Policies (Draft for v1)
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.action_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.enrollment_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- Allow users to read their own devices
CREATE POLICY "Users can read own devices"
    ON public.devices FOR SELECT
    USING (auth.uid() = user_id);

-- Allow users to read own device states
CREATE POLICY "Users can read own device states"
    ON public.device_states FOR SELECT
    USING (EXISTS (SELECT 1 FROM public.devices WHERE devices.id = device_states.device_id AND devices.user_id = auth.uid()));

-- API Proxy handles all writes (Serverless function with service role key)
-- So we only need SELECT policies for the Angular client to listen to Realtime.
