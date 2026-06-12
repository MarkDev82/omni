import { Request, Response } from 'express';
import { supabaseAdmin } from './_utils/supabase';
import { getUserFromRequest, handleCors } from './_utils/auth';

export default async function handler(req: Request, res: Response) {
  if (handleCors(req, res)) return;

  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const user = await getUserFromRequest(req, res);
    if (!user) return; // Error handled inside helper

    const { data: devices, error } = await supabaseAdmin
      .from('devices')
      .select('id, model_name, os_version, updated_at, created_at')
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
