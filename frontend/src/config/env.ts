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
  storageEmulatorPort: number;
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
    if (trimmed === '/') {
      return '';
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

const IP_ADDRESS_REGEX = /^(?:\d{1,3}\.){3}\d{1,3}$/;
const GOOGLE_MISSPELLINGS_REGEX = /google|g[o0]{2,}gle|go{2,}gl|g[o0]g[o0]le/i;

/**
 * Validates a Google Cloud Storage / Firebase Storage bucket name against Google Cloud Storage's
 * documented naming rules.
 *
 * Requirements:
 * - 3 to 63 characters for names without dots.
 * - Up to 222 characters for names containing dots, with each dot-separated segment no longer than 63 characters.
 * - Allowed characters: lowercase letters, numbers, hyphens, underscores, and dots.
 * - Each segment must begin and end with an alphanumeric character (a-z, 0-9).
 * - Cannot begin with the 'goog' prefix.
 * - Cannot contain 'google' or close misspellings of 'google'.
 * - Cannot be represented as an IP address in dotted-decimal notation.
 * - Cannot contain invalid dotted separators (e.g., '..', '.-', '-.').
 *
 * Note: Dotted bucket names (e.g., 'storage.example.com') require domain ownership verification
 * in Google Search Console / Google Cloud before bucket creation.
 */
export function validateStorageBucketName(bucketName: string, environmentLabel: string): void {
  const name = bucketName ? bucketName.trim() : '';

  if (!name) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: VITE_FIREBASE_STORAGE_BUCKET is missing or blank in ${environmentLabel}. ` +
      'An explicit, valid Firebase Storage bucket name must be configured.'
    );
  }

  if (name.length < 3 || name.length > 222) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: Invalid bucket name "${name}" in ${environmentLabel}. ` +
      `Length must be between 3 and 222 characters (current: ${name.length}).`
    );
  }

  if (name.startsWith('gs://') || name.startsWith('http://') || name.startsWith('https://')) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: Bucket name "${name}" in ${environmentLabel} contains a URL protocol prefix. ` +
      'Specify only the bucket name, without "gs://" or "https://".'
    );
  }

  if (name.startsWith('goog')) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: Bucket name "${name}" in ${environmentLabel} cannot start with the "goog" prefix.`
    );
  }

  if (GOOGLE_MISSPELLINGS_REGEX.test(name)) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: Bucket name "${name}" in ${environmentLabel} cannot contain "google" or close misspellings.`
    );
  }

  if (IP_ADDRESS_REGEX.test(name)) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: Bucket name "${name}" in ${environmentLabel} cannot be formatted as a dotted-decimal IP address.`
    );
  }

  if (!/^[a-z0-9._-]+$/.test(name)) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: Bucket name "${name}" in ${environmentLabel} contains invalid characters. ` +
      'Only lowercase letters, numbers, hyphens (-), underscores (_), and dots (.) are allowed.'
    );
  }

  if (name.includes('..') || name.includes('.-') || name.includes('-.')) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: Bucket name "${name}" in ${environmentLabel} contains invalid adjacent dot separators.`
    );
  }

  const segments = name.split('.');
  if (segments.length === 1 && name.length > 63) {
    throw new Error(
      `CRITICAL CONFIGURATION ERROR: Single-segment bucket name "${name}" in ${environmentLabel} cannot exceed 63 characters.`
    );
  }

  for (const segment of segments) {
    if (segment.length === 0) {
      throw new Error(
        `CRITICAL CONFIGURATION ERROR: Bucket name "${name}" in ${environmentLabel} contains empty segments or leading/trailing dots.`
      );
    }
    if (segment.length > 63) {
      throw new Error(
        `CRITICAL CONFIGURATION ERROR: Segment "${segment}" in bucket name "${name}" exceeds 63 characters.`
      );
    }
    if (!/^[a-z0-9](?:.*[a-z0-9])?$/.test(segment)) {
      throw new Error(
        `CRITICAL CONFIGURATION ERROR: Segment "${segment}" in bucket name "${name}" must begin and end with an alphanumeric character.`
      );
    }
  }
}

export function validateFirebaseConfig(config: FirebaseConfigEnv, isProd: boolean = import.meta.env.PROD): void {
  if (isProd) {
    if (config.useEmulator) {
      throw new Error(
        'CRITICAL SECURITY CONFIGURATION ERROR: VITE_USE_FIREBASE_EMULATOR is enabled in production! ' +
        'Production builds must NEVER connect to Firebase emulators. Disable VITE_USE_FIREBASE_EMULATOR.'
      );
    }
    if (!config.apiKey || !config.apiKey.trim()) {
      throw new Error(
        'CRITICAL CONFIGURATION ERROR: VITE_FIREBASE_API_KEY is missing or blank in production. ' +
        'Configure valid Firebase Web SDK credentials for production.'
      );
    }
    validateStorageBucketName(config.storageBucket, 'production');
    return;
  }

  // Development without emulator: requires explicit valid bucket
  if (!config.useEmulator) {
    validateStorageBucketName(config.storageBucket, 'development (without emulator)');
    return;
  }

  // Emulator mode: requires explicit valid bucket matching emulator configuration
  validateStorageBucketName(config.storageBucket, 'emulator mode');
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
  storageEmulatorPort: Number(import.meta.env.VITE_FIREBASE_STORAGE_EMULATOR_PORT || 9199),
};

// Validate environment in production runtime
validateFirebaseConfig(firebaseEnv, isProduction);

export const env = {
  apiBaseUrl: resolveApiBaseUrl(rawApiUrl, isProduction),
  firebase: firebaseEnv,
} as const;
