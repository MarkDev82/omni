import { Request, Response } from 'express';
import { supabaseAdmin } from './_utils/supabase';
import { getUserFromRequest, handleCors } from './_utils/auth';
import { firebaseAdmin } from './_utils/firebase';

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
    return res.status(400).json({ error: 'Malformed request body' });
  }

  const { device_id, command } = body;
  if (!device_id || !command) {
    return res.status(400).json({ error: 'Missing device_id or command' });
  }

  try {
    const user = await getUserFromRequest(req, res);
    if (!user) return; // handled

    // Verify user owns device
    const { data: device, error } = await supabaseAdmin
      .from('devices')
      .select('id, fcm_token')
      .eq('id', device_id)
      .eq('user_id', user.id)
      .single();

    if (error || !device) {
      return res.status(404).json({ error: 'Device not found or unauthorized' });
    }

    if (!device.fcm_token) {
      return res.status(400).json({ error: 'Device has no FCM token registered' });
    }

    // Insert action request into DB for history
    // Convert commands to Enum values natively supported by the DB
    let actionType = command.toUpperCase();
    if (actionType === 'LOCATION') actionType = 'LOCATE';
    if (actionType === 'ALARM') actionType = 'RING';
    // LOCK and WIPE might still throw a DB warning if not in the ENUM, but the push will be sent.

    const { data: actionReq, error: insertError } = await supabaseAdmin
      .from('action_requests')
      .insert({
        device_id: device.id,
        action_type: actionType,
        status: 'PENDING'
      })
      .select('id')
      .single();

    if (insertError) {
      console.warn('Could not record action request in DB. Check action_type enum.', insertError);
      // We continue to try sending the push anyway
    }

    // Send FCM
    const messageId = await firebaseAdmin.messaging().send({
      token: device.fcm_token,
      data: {
        command: command,
        action_id: actionReq?.id || ''
      },
      // Use high priority to wake up Android device
      android: {
        priority: 'high'
      }
    });

    return res.status(200).json({ success: true, messageId });
  } catch (err: any) {
    console.error('FCM Error:', err);
    return res.status(500).json({ error: 'Failed to dispatch command', details: err.message });
  }
}
