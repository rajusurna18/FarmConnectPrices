import { describe, it, expect } from 'vitest';
import { resolveApiBaseUrl, validateFirebaseConfig } from '../env';
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
    };

    it('passes in development even if apiKey is empty', () => {
      const devConfig: FirebaseConfigEnv = { ...validConfig, apiKey: '' };
      expect(() => validateFirebaseConfig(devConfig, false)).not.toThrow();
    });

    it('passes in production when emulator is enabled', () => {
      const emulatorConfig: FirebaseConfigEnv = { ...validConfig, apiKey: '', useEmulator: true };
      expect(() => validateFirebaseConfig(emulatorConfig, true)).not.toThrow();
    });

    it('passes in production with valid apiKey', () => {
      expect(() => validateFirebaseConfig(validConfig, true)).not.toThrow();
    });

    it('throws error in production when apiKey is missing or blank and emulator is false', () => {
      const invalidConfig: FirebaseConfigEnv = { ...validConfig, apiKey: '' };
      expect(() => validateFirebaseConfig(invalidConfig, true)).toThrowError(
        /CRITICAL CONFIGURATION ERROR: VITE_FIREBASE_API_KEY is missing or blank in production/
      );
    });
  });
});

