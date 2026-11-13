import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';
import { resolveApiBaseUrl } from '../env';

interface CloudRunTarget {
  serviceId: string;
  region: string;
  pinTag?: boolean;
}

interface RewriteRule {
  source: string;
  destination?: string;
  run?: CloudRunTarget;
}

interface HeaderEntry {
  key: string;
  value: string;
}

interface HeaderRule {
  source: string;
  headers: HeaderEntry[];
}

interface FirebaseHostingConfig {
  public: string;
  ignore?: string[];
  rewrites?: RewriteRule[];
  headers?: HeaderRule[];
}

interface FirebaseConfig {
  hosting?: FirebaseHostingConfig;
}

describe('Firebase Hosting Configuration (firebase.json)', () => {
  const rootDir = path.resolve(__dirname, '../../../../');
  const firebaseJsonPath = path.join(rootDir, 'firebase.json');

  it('firebase.json exists at repository root and is valid JSON', () => {
    expect(fs.existsSync(firebaseJsonPath)).toBe(true);
    const rawContent = fs.readFileSync(firebaseJsonPath, 'utf-8');
    expect(() => JSON.parse(rawContent)).not.toThrow();
  });

  const parsedConfig: FirebaseConfig = JSON.parse(fs.readFileSync(firebaseJsonPath, 'utf-8'));
  const hosting = parsedConfig.hosting;

  it('points hosting.public to frontend/dist', () => {
    expect(hosting).toBeDefined();
    expect(hosting?.public).toBe('frontend/dist');
  });

  describe('Rewrites Configuration', () => {
    it('defines rewrites array with API rewrite preceding SPA fallback', () => {
      const rewrites = hosting?.rewrites;
      expect(Array.isArray(rewrites)).toBe(true);
      expect(rewrites?.length).toBeGreaterThanOrEqual(2);

      // Rule 1: /api/** -> Cloud Run
      const apiRule = rewrites![0];
      expect(apiRule.source).toBe('/api/**');
      expect(apiRule.run).toBeDefined();
      expect(apiRule.run?.serviceId).toBe('farmlink-api');
      expect(apiRule.run?.region).toBe('asia-south1');
      expect(apiRule.run?.pinTag).toBeUndefined();

      // Rule 2: ** -> /index.html (SPA fallback)
      const spaRule = rewrites![1];
      expect(spaRule.source).toBe('**');
      expect(spaRule.destination).toBe('/index.html');
    });
  });

  describe('Caching & Security Headers Configuration', () => {
    it('configures long-lived immutable caching for hashed /assets/**', () => {
      const headers = hosting?.headers;
      expect(Array.isArray(headers)).toBe(true);

      const assetsRule = headers?.find((r) => r.source === '/assets/**');
      expect(assetsRule).toBeDefined();

      const cacheControlHeader = assetsRule?.headers.find((h) => h.key === 'Cache-Control');
      expect(cacheControlHeader).toBeDefined();
      expect(cacheControlHeader?.value).toBe('public, max-age=31536000, immutable');
    });

    it('configures revalidation no-cache policy for /index.html entry document', () => {
      const headers = hosting?.headers;
      const indexRule = headers?.find((r) => r.source === '/index.html');
      expect(indexRule).toBeDefined();

      const cacheControlHeader = indexRule?.headers.find((h) => h.key === 'Cache-Control');
      expect(cacheControlHeader).toBeDefined();
      expect(cacheControlHeader?.value).toBe('no-cache, max-age=0, must-revalidate');
    });

    it('does not configure unrequested global headers for **', () => {
      const headers = hosting?.headers;
      const wildcardRule = headers?.find((r) => r.source === '**');
      expect(wildcardRule).toBeUndefined();
    });
  });

  describe('Frontend Same-Origin Production URL Compatibility', () => {
    it('resolves production VITE_API_BASE_URL="/" to empty string for Axios same-origin requests', () => {
      const resolved = resolveApiBaseUrl('/', true);
      expect(resolved).toBe('');

      // When frontend services make requests like '/api/v1/health'
      const endpoint = '/api/v1/health';
      const fullUrl = resolved + endpoint;
      expect(fullUrl).toBe('/api/v1/health');
      expect(fullUrl.startsWith('/api/api/')).toBe(false);
    });
  });
});

