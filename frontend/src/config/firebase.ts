import { initializeApp, getApps, getApp } from 'firebase/app';
import type { FirebaseApp } from 'firebase/app';
import { getFirestore, connectFirestoreEmulator } from 'firebase/firestore';
import type { Firestore } from 'firebase/firestore';
import { env } from './env';

const firebaseConfig = {
  apiKey: env.firebase.apiKey,
  authDomain: env.firebase.authDomain,
  projectId: env.firebase.projectId || 'farmconnectprices-dev',
  storageBucket: env.firebase.storageBucket,
  messagingSenderId: env.firebase.messagingSenderId,
  appId: env.firebase.appId,
};

// Singleton initialization ensuring Firebase initializes only once across HMR / renders
export const app: FirebaseApp = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();

export const db: Firestore = getFirestore(app);

// Connect to Firestore Emulator in local development mode if configured
if (env.firebase.useEmulator) {
  try {
    connectFirestoreEmulator(db, env.firebase.emulatorHost, env.firebase.emulatorPort);
    console.info(`[Firebase] Connected to Firestore Emulator at ${env.firebase.emulatorHost}:${env.firebase.emulatorPort}`);
  } catch (error) {
    console.warn('[Firebase] Firestore Emulator connection warning or already connected:', error);
  }
}
