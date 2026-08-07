-- Create a new table for device photos
CREATE TABLE IF NOT EXISTS public.device_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    camera_type TEXT NOT NULL, -- 'front' or 'back'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS for device_photos
ALTER TABLE public.device_photos ENABLE ROW LEVEL SECURITY;

-- Allow read access to authenticated users
CREATE POLICY "Allow authenticated users to read device_photos"
    ON public.device_photos FOR SELECT
    USING (auth.role() = 'authenticated');

-- Allow insert from anywhere (API key)
CREATE POLICY "Allow anonymous insert to device_photos"
    ON public.device_photos FOR INSERT
    WITH CHECK (true);

-- Enable real-time for the device_photos table
ALTER PUBLICATION supabase_realtime ADD TABLE public.device_photos;

-- Create Storage Bucket
INSERT INTO storage.buckets (id, name, public) 
VALUES ('omni-photos', 'omni-photos', true)
ON CONFLICT (id) DO NOTHING;

-- Allow public read access to the bucket
CREATE POLICY "Public Access"
    ON storage.objects FOR SELECT
    USING ( bucket_id = 'omni-photos' );

-- Allow inserts (the API will upload via service role anyway, but just in case)
CREATE POLICY "Allow insert"
    ON storage.objects FOR INSERT
    WITH CHECK ( bucket_id = 'omni-photos' );
