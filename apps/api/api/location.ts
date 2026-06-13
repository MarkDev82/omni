import { Request, Response } from 'express';
import { supabaseAdmin } from './_utils/supabase';
import { getUserFromRequest, handleCors } from './_utils/auth';

export default async function handler(req: Request, res: Response) {
  if (handleCors(req, res)) return;

  if (req.method === 'GET') {
    // Fetch device state
    const deviceId = req.query.device_id;
    if (!deviceId || typeof deviceId !== 'string') return res.status(400).json({ error: 'Missing device_id' });

    try {
      const user = await getUserFromRequest(req, res);
      if (!user) return;

      const { data, error } = await supabaseAdmin
        .from('device_states')
        .select('*')
        .eq('device_id', deviceId)
        .single();

      // It's ok if not found, it just means no state reported yet
      return res.status(200).json(data || {});
    } catch (e: any) {
      return res.status(500).json({ error: e.message });
    }
  }

  if (req.method === 'POST') {
    // Update device state (called by Android App, authenticated via device_secret)
    let body: any = {};
    try {
      body = req.body || {};
      if (typeof body === 'string') {
        try { body = JSON.parse(body); } catch(e) {}
      }
    } catch (e) {
      return res.status(400).json({ error: 'Malformed request body' });
    }

    const { device_id, device_secret, lat, lng } = body;
    if (!device_id || !device_secret || lat === undefined || lng === undefined) {
      return res.status(400).json({ error: 'Missing parameters' });
    }

    // Verify device secret
    const { data: device } = await supabaseAdmin
      .from('devices')
      .select('id')
      .eq('id', device_id)
      .eq('device_secret', device_secret)
      .single();

    if (!device) {
      return res.status(401).json({ error: 'Unauthorized device' });
    }

    const { error } = await supabaseAdmin
      .from('device_states')
      .upsert({
        device_id: device_id,
        lat: lat,
        lng: lng,
        last_seen_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      }, { onConflict: 'device_id' });

    if (error) {
      console.error('Error updating location:', error);
      return res.status(500).json({ error: 'Failed to update location' });
    }

    return res.status(200).json({ success: true });
  }

  return res.status(405).json({ error: 'Method not allowed' });
}
