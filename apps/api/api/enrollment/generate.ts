import { Request, Response } from 'express';
import * as crypto from 'crypto';
import { supabaseAdmin } from '../_utils/supabase';
import { getUserFromRequest, handleCors } from '../_utils/auth';

export default async function handler(req: Request, res: Response) {
  if (handleCors(req, res)) return;

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const user = await getUserFromRequest(req, res);
  if (!user) return; // Error handled inside helper

  // Generate secure 6-digit PIN
  const pin = Math.floor(100000 + Math.random() * 900000).toString();
  const hash = crypto.createHash('sha256').update(pin).digest('hex');

  const expiresAt = new Date();
  expiresAt.setMinutes(expiresAt.getMinutes() + 15);

  const { error } = await supabaseAdmin.from('enrollment_codes').insert({
    user_id: user.id,
    code_hash: hash,
    expires_at: expiresAt.toISOString()
  });

  if (error) {
    console.error('Failed to generate PIN:', error);
    return res.status(500).json({ error: 'Failed to generate enrollment code' });
  }

  return res.status(200).json({ pin, expires_at: expiresAt.toISOString() });
}
