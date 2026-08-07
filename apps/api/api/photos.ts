import { Request, Response } from 'express';
import { createClient } from '@supabase/supabase-js';

const supabaseAdmin = createClient(
  process.env.SUPABASE_URL || '',
  process.env.SUPABASE_SERVICE_ROLE_KEY || ''
);

export default async function handler(req: Request, res: Response) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const { device_id, device_secret, camera_type, image_base64 } = req.body;

    if (!device_id || !device_secret || !image_base64) {
      return res.status(400).json({ error: 'Missing parameters' });
    }

    // 1. Authenticate Device
    const { data: device, error: authError } = await supabaseAdmin
      .from('devices')
      .select('id')
      .eq('id', device_id)
      .eq('device_secret', device_secret)
      .single();

    if (authError || !device) {
      return res.status(401).json({ error: 'Unauthorized device' });
    }

    // 2. Decode Base64
    const buffer = Buffer.from(image_base64, 'base64');
    
    // 3. Generate unique filename
    const filename = `${device_id}/${Date.now()}-${camera_type}.jpg`;

    // 4. Upload to Supabase Storage
    const { data: uploadData, error: uploadError } = await supabaseAdmin
      .storage
      .from('omni-photos')
      .upload(filename, buffer, {
        contentType: 'image/jpeg',
        upsert: true
      });

    if (uploadError) {
      console.error('Storage error:', uploadError);
      return res.status(500).json({ error: 'Failed to upload image' });
    }

    // Get public URL
    const { data: publicUrlData } = supabaseAdmin
      .storage
      .from('omni-photos')
      .getPublicUrl(filename);
    
    const photoUrl = publicUrlData.publicUrl;

    // 5. Insert into device_photos
    const { error: dbError } = await supabaseAdmin
      .from('device_photos')
      .insert({
        device_id: device_id,
        url: photoUrl,
        camera_type: camera_type || 'front'
      });

    if (dbError) {
      console.error('DB error:', dbError);
      return res.status(500).json({ error: 'Failed to save photo metadata' });
    }

    return res.status(200).json({ success: true, url: photoUrl });

  } catch (error) {
    console.error('Photos API error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
