import { describe, it, expect } from 'vitest';
import { resolveApiBaseUrl, validateFirebaseConfig, validateStorageBucketName, env } from '../env';
import type { FirebaseConfigEnv } from '../env';

describe('Frontend Environment Configuration (env.ts)', () => {
  describe('resolveApiBaseUrl', () => {
    it('returns localhost:8080 fallback in development when rawUrl is missing or blank', () => {
      expect(resolveApiBaseUrl(undefined, false)).toBe('http://localhost:8080');
      expect(resolveApiBaseUrl('', false)).toBe('http://localhost:8080');
      expect(resolveApiBaseUrl('   ', false)).toBe('http://localhost:8080');
    });

    it('returns configured URL in development when rawUrl is provided', () => {
      expect(resolveApiBaseUrl('http://localhost:3000', false)).toBe('http://localhost:3000');
      expect(resolveApiBaseUrl('https://api.dev.farmconnect.com', false)).toBe('https://api.dev.farmconnect.com');
    });

    it('returns trimmed production API URL when valid', () => {
      expect(resolveApiBaseUrl('https://api.farmconnectprices.com', true)).toBe('https://api.farmconnectprices.com');
      expect(resolveApiBaseUrl('   https://api.farmconnectprices.com/api/v1   ', true)).toBe('https://api.farmconnectprices.com/api/v1');
    });

    it('resolves "/" to empty string in production for same-origin proxying', () => {
      expect(resolveApiBaseUrl('/', true)).toBe('');
    });

    it('resolves " / " with whitespace to empty string in production after trimming', () => {
      expect(resolveApiBaseUrl('   /   ', true)).toBe('');
    });

    it('throws error in production when rawUrl is missing or blank', () => {
      expect(() => resolveApiBaseUrl(undefined, true)).toThrowError(
        /CRITICAL CONFIGURATION ERROR: VITE_API_BASE_URL is missing or blank in production/
      );
      expect(() => resolveApiBaseUrl('', true)).toThrowError(
        /CRITICAL CONFIGURATION ERROR: VITE_API_BASE_URL is missing or blank in production/
      );
      expect(() => resolveApiBaseUrl('   ', true)).toThrowError(
        /CRITICAL CONFIGURATION ERROR: VITE_API_BASE_URL is missing or blank in production/
      );
    });

    it('throws error in production when rawUrl points to localhost or 127.0.0.1', () => {
      expect(() => resolveApiBaseUrl('http://localhost:8080', true)).toThrowError(
        /CRITICAL CONFIGURATION ERROR: VITE_API_BASE_URL is configured as localhost/
      );
      expect(() => resolveApiBaseUrl('http://127.0.0.1:8080', true)).toThrowError(
        /CRITICAL CONFIGURATION ERROR: VITE_API_BASE_URL is configured as localhost/
      );
    });
  });

  describe('validateStorageBucketName', () => {
    it('accepts valid single-segment bucket names (3 to 63 chars)', () => {
      expect(() => validateStorageBucketName('my-farm-bucket', 'test')).not.toThrow();
      expect(() => validateStorageBucketName('crops-2026', 'test')).not.toThrow();
      expect(() => validateStorageBucketName('abc', 'test')).not.toThrow();
    });

    it('accepts valid dot-separated bucket names up to 222 chars', () => {
      expect(() => validateStorageBucketName('farmconnectprices.appspot.com', 'test')).not.toThrow();
      expect(() => validateStorageBucketName('farmconnectprices.firebasestorage.app', 'test')).not.toThrow();
      expect(() => validateStorageBucketName('storage.farmconnect.in', 'test')).not.toThrow();
    });

    it('does not reject consecutive underscores solely as a claimed GCS prohibition', () => {
      expect(() => validateStorageBucketName('my__special__bucket', 'test')).not.toThrow();
    });

    it('rejects missing, empty, or whitespace-only bucket names', () => {
      expect(() => validateStorageBucketName('', 'test')).toThrowError(/missing or blank/);
      expect(() => validateStorageBucketName('   ', 'test')).toThrowError(/missing or blank/);
    });

    it('rejects bucket names that are too short (< 3 chars)', () => {
      expect(() => validateStorageBucketName('ab', 'test')).toThrowError(/between 3 and 222 characters/);
    });

    it('rejects single-segment bucket names exceeding 63 characters', () => {
      const longName = 'a'.repeat(64);
      expect(() => validateStorageBucketName(longName, 'test')).toThrowError(/cannot exceed 63 characters/);
    });

    it('rejects URL protocol prefixes (gs://, http://, https://)', () => {
      expect(() => validateStorageBucketName('gs://my-bucket', 'test')).toThrowError(/URL protocol prefix/);
      expect(() => validateStorageBucketName('https://my-bucket.com', 'test')).toThrowError(/URL protocol prefix/);
    });

    it('rejects names beginning with "goog"', () => {
      expect(() => validateStorageBucketName('goog-crops', 'test')).toThrowError(/cannot start with the "goog" prefix/);
      expect(() => validateStorageBucketName('google-bucket', 'test')).toThrowError(/cannot start with the "goog" prefix/);
    });

    it('rejects names containing "google" or close misspellings', () => {
      expect(() => validateStorageBucketName('my-google-bucket', 'test')).toThrowError(/cannot contain "google" or close misspellings/);
      expect(() => validateStorageBucketName('crops-g00gle-bucket', 'test')).toThrowError(/cannot contain "google" or close misspellings/);
      expect(() => validateStorageBucketName('farm-goooogle-data', 'test')).toThrowError(/cannot contain "google" or close misspellings/);
    });

    it('rejects names formatted as dotted-decimal IP addresses', () => {
      expect(() => validateStorageBucketName('192.168.1.1', 'test')).toThrowError(/dotted-decimal IP address/);
      expect(() => validateStorageBucketName('127.0.0.1', 'test')).toThrowError(/dotted-decimal IP address/);
    });

    it('rejects uppercase letters and invalid symbols', () => {
      expect(() => validateStorageBucketName('MyBucket', 'test')).toThrowError(/invalid characters/);
      expect(() => validateStorageBucketName('farm_bucket!', 'test')).toThrowError(/invalid characters/);
    });

    it('rejects non-alphanumeric start or end', () => {
      expect(() => validateStorageBucketName('-my-bucket', 'test')).toThrowError(/must begin and end with an alphanumeric/);
      expect(() => validateStorageBucketName('my-bucket-', 'test')).toThrowError(/must begin and end with an alphanumeric/);
      expect(() => validateStorageBucketName('.my.bucket', 'test')).toThrowError(/empty segments or leading\/trailing dots/);
      expect(() => validateStorageBucketName('my.bucket.', 'test')).toThrowError(/empty segments or leading\/trailing dots/);
    });

    it('rejects invalid adjacent dot separators', () => {
      expect(() => validateStorageBucketName('my..bucket', 'test')).toThrowError(/invalid adjacent dot separators/);
      expect(() => validateStorageBucketName('my.-bucket', 'test')).toThrowError(/invalid adjacent dot separators/);
      expect(() => validateStorageBucketName('my-.bucket', 'test')).toThrowError(/invalid adjacent dot separators/);
    });
  });

  describe('validateFirebaseConfig', () => {
    const validConfig: FirebaseConfigEnv = {
      apiKey: 'AIzaSyA_sample_key_123',
      authDomain: 'farmconnectprices.firebaseapp.com',
      projectId: 'farmconnectprices',
      storageBucket: 'farmconnectprices.appspot.com',
      messagingSenderId: '123456789',
      appId: '1:123456789:web:abcdef',
      useEmulator: false,
      emulatorHost: 'localhost',
      emulatorPort: 8081,
      storageEmulatorPort: 9199,
    };

    it('passes in production with valid apiKey and valid storageBucket', () => {
      expect(() => validateFirebaseConfig(validConfig, true)).not.toThrow();
    });

    it('rejects production configuration when emulator is enabled', () => {
      const prodEmulatorConfig: FirebaseConfigEnv = { ...validConfig, useEmulator: true };
      expect(() => validateFirebaseConfig(prodEmulatorConfig, true)).toThrowError(
        /CRITICAL SECURITY CONFIGURATION ERROR: VITE_USE_FIREBASE_EMULATOR is enabled in production/
      );
    });

    it('throws error in production when apiKey is missing or blank', () => {
      const invalidConfig: FirebaseConfigEnv = { ...validConfig, apiKey: '' };
      expect(() => validateFirebaseConfig(invalidConfig, true)).toThrowError(
        /CRITICAL CONFIGURATION ERROR: VITE_FIREBASE_API_KEY is missing or blank in production/
      );
    });

    it('throws error in production when storageBucket is missing or blank', () => {
      const invalidConfig: FirebaseConfigEnv = { ...validConfig, storageBucket: '' };
      expect(() => validateFirebaseConfig(invalidConfig, true)).toThrowError(
        /CRITICAL CONFIGURATION ERROR: VITE_FIREBASE_STORAGE_BUCKET is missing or blank in production/
      );
    });

    it('passes in development without emulator when bucket is valid', () => {
      const devConfig: FirebaseConfigEnv = { ...validConfig, apiKey: '', useEmulator: false };
      expect(() => validateFirebaseConfig(devConfig, false)).not.toThrow();
    });

    it('throws in development without emulator when bucket is missing', () => {
      const devConfig: FirebaseConfigEnv = { ...validConfig, apiKey: '', storageBucket: '', useEmulator: false };
      expect(() => validateFirebaseConfig(devConfig, false)).toThrowError(
        /VITE_FIREBASE_STORAGE_BUCKET is missing or blank in development \(without emulator\)/
      );
    });

    it('passes in emulator mode when bucket is valid', () => {
      const emulatorConfig: FirebaseConfigEnv = { ...validConfig, apiKey: '', useEmulator: true };
      expect(() => validateFirebaseConfig(emulatorConfig, false)).not.toThrow();
    });

    it('throws in emulator mode when bucket is missing', () => {
      const emulatorConfig: FirebaseConfigEnv = { ...validConfig, apiKey: '', storageBucket: '', useEmulator: true };
      expect(() => validateFirebaseConfig(emulatorConfig, false)).toThrowError(
        /VITE_FIREBASE_STORAGE_BUCKET is missing or blank in emulator mode/
      );
    });
  });

  describe('Storage Emulator Port Configuration', () => {
    it('provides storageEmulatorPort defaulting to 9199', () => {
      expect(env.firebase.storageEmulatorPort).toBe(9199);
    });
  });
});
