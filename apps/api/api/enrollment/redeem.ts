import { Request, Response } from 'express';
import * as crypto from 'crypto';
import { supabaseAdmin } from '../_utils/supabase';
import { handleCors } from '../_utils/auth';

export default async function handler(req: Request, res: Response) {
  if (handleCors(req, res)) return;

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  let body: any = {};
  try {
    body = req.body || {};
    if (typeof body === 'string') {
      try { body = JSON.parse(body); } catch(e) {}
    }
  } catch (e) {
    console.error('Error parsing body:', e);
    return res.status(400).json({ error: 'Malformed request headers or body' });
  }

  const { pin, fcm_token, model_name, os_version } = body;

  if (!pin || typeof pin !== 'string') {
    return res.status(400).json({ error: 'Invalid PIN' });
  }

  const hash = crypto.createHash('sha256').update(pin).digest('hex');

  const { data: codes, error } = await supabaseAdmin
    .from('enrollment_codes')
    .select('*')
    .eq('code_hash', hash)
    .gt('expires_at', new Date().toISOString());

  if (error || !codes || codes.length === 0) {
    return res.status(401).json({ error: 'Invalid or expired PIN' });
  }

  const validCode = codes[0];

  const { data: device, error: deviceError } = await supabaseAdmin
    .from('devices')
    .insert({
      user_id: validCode.user_id,
      fcm_token: fcm_token || null,
      model_name: model_name || 'Android Device',
      os_version: os_version || 'Unknown'
    })
    .select('id, device_secret')
    .single();

  if (deviceError || !device) {
    console.error('Device registration failed:', deviceError);
    return res.status(500).json({ error: 'Failed to register device' });
  }

  // Delete the code to prevent reuse
  await supabaseAdmin.from('enrollment_codes').delete().eq('id', validCode.id);

  return res.status(200).json({
    device_id: device.id,
    device_secret: device.device_secret
  });
}
