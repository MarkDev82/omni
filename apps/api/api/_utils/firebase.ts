import * as admin from 'firebase-admin';

// Load the service account dynamically. Vercel allows requiring local JSON files if included in the bundle.
let serviceAccount: any;
try {
  serviceAccount = require('../../firebase-admin-key.json');
} catch (e) {
  console.warn('Firebase service account key not found at apps/api/firebase-admin-key.json. FCM will not work.');
}

if (serviceAccount && !admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

export const firebaseAdmin = admin;
