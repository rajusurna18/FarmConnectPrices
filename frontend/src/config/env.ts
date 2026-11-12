export interface FirebaseConfigEnv {
  apiKey: string;
  authDomain: string;
  projectId: string;
  storageBucket: string;
  messagingSenderId: string;
  appId: string;
  useEmulator: boolean;
  emulatorHost: string;
  emulatorPort: number;
}

export function resolveApiBaseUrl(rawUrl?: string, isProd: boolean = import.meta.env.PROD): string {
  const trimmed = rawUrl ? rawUrl.trim() : '';

  if (isProd) {
    if (!trimmed) {
      throw new Error(
        'CRITICAL CONFIGURATION ERROR: VITE_API_BASE_URL is missing or blank in production. ' +
        'A valid backend API URL (e.g. https://api.farmconnectprices.com) must be configured in environment variables.'
      );
    }
    const lower = trimmed.toLowerCase();
    if (lower.startsWith('http://localhost') || lower.startsWith('http://127.0.0.1')) {
      throw new Error(
        `CRITICAL CONFIGURATION ERROR: VITE_API_BASE_URL is configured as localhost ('${trimmed}') in production. ` +
        'A valid public production backend API URL must be configured.'
      );
    }
    return trimmed;
  }

  return trimmed || 'http://localhost:8080';
}

export function validateFirebaseConfig(config: FirebaseConfigEnv, isProd: boolean = import.meta.env.PROD): void {
  if (isProd && !config.useEmulator) {
    if (!config.apiKey || !config.apiKey.trim()) {
      throw new Error(
        'CRITICAL CONFIGURATION ERROR: VITE_FIREBASE_API_KEY is missing or blank in production. ' +
        'Configure valid Firebase Web SDK credentials for production.'
      );
    }
  }
}

const isProduction = import.meta.env.PROD;
const rawApiUrl = import.meta.env.VITE_API_BASE_URL;

const firebaseEnv: FirebaseConfigEnv = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || '',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || 'farmconnectprices',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || '',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || '',
  useEmulator: import.meta.env.VITE_USE_FIREBASE_EMULATOR === 'true',
  emulatorHost: import.meta.env.VITE_FIREBASE_EMULATOR_HOST || 'localhost',
  emulatorPort: Number(import.meta.env.VITE_FIREBASE_EMULATOR_PORT || 8081),
};

// Validate environment in production runtime
validateFirebaseConfig(firebaseEnv, isProduction);

export const env = {
  apiBaseUrl: resolveApiBaseUrl(rawApiUrl, isProduction),
  firebase: firebaseEnv,
} as const;
