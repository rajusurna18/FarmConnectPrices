import { initializeApp, getApps, getApp } from 'firebase/app';
import type { FirebaseApp } from 'firebase/app';
import { getFirestore, connectFirestoreEmulator } from 'firebase/firestore';
import type { Firestore } from 'firebase/firestore';
import { getAuth, connectAuthEmulator } from 'firebase/auth';
import type { Auth } from 'firebase/auth';
import { getStorage, connectStorageEmulator } from 'firebase/storage';
import type { FirebaseStorage } from 'firebase/storage';
import { env } from './env';

const firebaseConfig = {
  apiKey: env.firebase.apiKey,
  authDomain: env.firebase.authDomain,
  projectId: env.firebase.projectId || 'farmconnectprices',
  storageBucket: env.firebase.storageBucket,
  messagingSenderId: env.firebase.messagingSenderId,
  appId: env.firebase.appId,
};

// Singleton initialization ensuring Firebase initializes only once across HMR / renders
export const app: FirebaseApp = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();

export const db: Firestore = getFirestore(app);
export const auth: Auth = getAuth(app);
export const storage: FirebaseStorage = getStorage(app);

export interface EmulatorTarget {
  host: string;
  port: number;
}

const STORAGE_EMULATOR_REGISTRY_SYMBOL = Symbol.for('__FARMLINK_STORAGE_EMULATOR_REGISTRY__');

interface GlobalWithStorageEmulatorRegistry {
  [STORAGE_EMULATOR_REGISTRY_SYMBOL]?: WeakMap<FirebaseStorage, EmulatorTarget>;
}

export function getStorageEmulatorRegistry(): WeakMap<FirebaseStorage, EmulatorTarget> {
  const globalObj = (typeof window !== 'undefined' ? window : globalThis) as unknown as GlobalWithStorageEmulatorRegistry;
  if (!globalObj[STORAGE_EMULATOR_REGISTRY_SYMBOL]) {
    globalObj[STORAGE_EMULATOR_REGISTRY_SYMBOL] = new WeakMap<FirebaseStorage, EmulatorTarget>();
  }
  return globalObj[STORAGE_EMULATOR_REGISTRY_SYMBOL]!;
}

export function initializeStorageEmulator(
  storageInstance: FirebaseStorage,
  host: string,
  port: number,
  connectFn: (storage: FirebaseStorage, host: string, port: number) => void = connectStorageEmulator
): void {
  const trimmedHost = host ? host.trim() : '';
  if (!trimmedHost) {
    throw new Error('CRITICAL INITIALIZATION ERROR: Firebase Storage Emulator host cannot be blank.');
  }
  if (!port || port <= 0 || port > 65535 || isNaN(port)) {
    throw new Error(`CRITICAL INITIALIZATION ERROR: Invalid Firebase Storage Emulator port: ${port}. Must be 1-65535.`);
  }

  const registry = getStorageEmulatorRegistry();
  const existingTarget = registry.get(storageInstance);

  if (existingTarget) {
    if (existingTarget.host === trimmedHost && existingTarget.port === port) {
      // Instance is already registered for this exact target. Safely skip re-connection.
      return;
    }
    throw new Error(
      `CRITICAL INITIALIZATION CONFLICT: FirebaseStorage instance is already connected to emulator at ` +
      `${existingTarget.host}:${existingTarget.port}. Cannot reconnect the same instance to ${trimmedHost}:${port}.`
    );
  }

  try {
    connectFn(storageInstance, trimmedHost, port);
    registry.set(storageInstance, { host: trimmedHost, port });
    console.info(`[Firebase] Connected to Storage Emulator at ${trimmedHost}:${port}`);
  } catch (error: unknown) {
    console.error(`[Firebase] CRITICAL ERROR: Failed to connect to Storage Emulator at ${trimmedHost}:${port}:`, error);
    throw error;
  }
}

// Connect to Emulators in local development mode if configured
// Production isolation: import.meta.env.PROD strictly guards against emulator connections
const canConnectEmulator = env.firebase.useEmulator && !import.meta.env.PROD;

if (canConnectEmulator) {
  try {
    connectFirestoreEmulator(db, env.firebase.emulatorHost, env.firebase.emulatorPort);
    console.info(`[Firebase] Connected to Firestore Emulator at ${env.firebase.emulatorHost}:${env.firebase.emulatorPort}`);
  } catch (error) {
    console.warn('[Firebase] Firestore Emulator connection warning or already connected:', error);
  }

  try {
    connectAuthEmulator(auth, `http://${env.firebase.emulatorHost}:9099`, { disableWarnings: true });
    console.info(`[Firebase] Connected to Auth Emulator at ${env.firebase.emulatorHost}:9099`);
  } catch (error) {
    console.warn('[Firebase] Auth Emulator connection warning or already connected:', error);
  }

  initializeStorageEmulator(storage, env.firebase.emulatorHost, env.firebase.storageEmulatorPort);
}
