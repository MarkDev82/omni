import { Request, Response } from 'express';
import { supabaseAdmin } from './_utils/supabase';
import { getUserFromRequest, handleCors } from './_utils/auth';

export default async function handler(req: Request, res: Response) {
  if (handleCors(req, res)) return;

  if (req.method !== 'GET' && req.method !== 'DELETE' && req.method !== 'PATCH') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const user = await getUserFromRequest(req, res);
    if (!user) return; // Error handled inside helper

    if (req.method === 'DELETE') {
      const deviceId = req.query.id;
      if (!deviceId || typeof deviceId !== 'string') {
        return res.status(400).json({ error: 'Missing device id parameter' });
      }

      // Manually cascade delete dependent rows in case ON DELETE CASCADE is not set in Supabase
      await supabaseAdmin.from('device_states').delete().eq('device_id', deviceId);
      await supabaseAdmin.from('action_requests').delete().eq('device_id', deviceId);

      const { error } = await supabaseAdmin
        .from('devices')
        .delete()
        .eq('id', deviceId)
        .eq('user_id', user.id);

      if (error) {
        console.error('Supabase error deleting device:', error);
        return res.status(500).json({ error: 'Failed to delete device' });
      }

      return res.status(200).json({ success: true });
    }

    if (req.method === 'PATCH') {
      let body: any = {};
      try {
        body = req.body || {};
        if (typeof body === 'string') {
          try { body = JSON.parse(body); } catch(e) {}
        }
      } catch (e) {}

      const { id, alias } = body;
      if (!id || typeof alias !== 'string') {
        return res.status(400).json({ error: 'Missing id or alias parameter' });
      }

      const { error } = await supabaseAdmin
        .from('devices')
        .update({ alias })
        .eq('id', id)
        .eq('user_id', user.id);

      if (error) {
        console.error('Supabase error updating device:', error);
        return res.status(500).json({ error: 'Failed to update device alias' });
      }

      return res.status(200).json({ success: true });
    }

    // Handle GET
    const { data: devices, error } = await supabaseAdmin
      .from('devices')
      .select('id, model_name, os_version, updated_at, created_at, alias')
      .eq('user_id', user.id)
      .order('created_at', { ascending: false });

    if (error) {
      console.error('Supabase error fetching devices:', error);
      return res.status(500).json({ error: 'Failed to fetch devices' });
    }

    return res.status(200).json(devices);
  } catch (err: any) {
    console.error('Unhandled exception in /api/devices:', err);
    return res.status(500).json({ error: 'Internal server error', details: err.message });
  }
}
