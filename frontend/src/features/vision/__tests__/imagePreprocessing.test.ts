import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import {
  calculateTargetDimensions,
  urlRegistry,
  preprocessImage,
  revokeProcessedImageUrls,
} from '../utils/imagePreprocessing';

describe('Vision AI — Image Preprocessing Utilities', () => {
  beforeEach(() => {
    urlRegistry.revokeAll();
  });

  afterEach(() => {
    urlRegistry.revokeAll();
  });

  describe('calculateTargetDimensions', () => {
    it('scales down large landscape images preserving aspect ratio', () => {
      // 4000x3000 with max 2048 -> scale = 2048/4000 = 0.512 -> height = 1536
      const res = calculateTargetDimensions({ width: 4000, height: 3000 }, 2048);

      expect(res.dimensions.width).toBe(2048);
      expect(res.dimensions.height).toBe(1536);
      expect(res.wasResized).toBe(true);
    });

    it('scales down large portrait images preserving aspect ratio', () => {
      // 1200x2400 with max 2048 -> scale = 2048/2400 = 0.85333 -> width = 1024
      const res = calculateTargetDimensions({ width: 1200, height: 2400 }, 2048);

      expect(res.dimensions.width).toBe(1024);
      expect(res.dimensions.height).toBe(2048);
      expect(res.wasResized).toBe(true);
    });

    it('NEVER upscales images smaller than the max bounding box', () => {
      const res = calculateTargetDimensions({ width: 640, height: 480 }, 2048);

      expect(res.dimensions.width).toBe(640);
      expect(res.dimensions.height).toBe(480);
      expect(res.wasResized).toBe(false);
    });

    it('calculates display derivative bounds (max 800px) accurately', () => {
      const res = calculateTargetDimensions({ width: 2000, height: 1000 }, 800);

      expect(res.dimensions.width).toBe(800);
      expect(res.dimensions.height).toBe(400);
      expect(res.wasResized).toBe(true);
    });
  });

  describe('urlRegistry (Lifecycle & Memory Leak Prevention)', () => {
    it('tracks created URLs and releases on revoke', () => {
      const blob1 = new Blob(['sample-image-1'], { type: 'image/jpeg' });
      const blob2 = new Blob(['sample-image-2'], { type: 'image/webp' });

      const url1 = urlRegistry.create(blob1);
      const url2 = urlRegistry.create(blob2);

      expect(urlRegistry.getActiveCount()).toBe(2);

      urlRegistry.revoke(url1);
      expect(urlRegistry.getActiveCount()).toBe(1);

      urlRegistry.revoke(url2);
      expect(urlRegistry.getActiveCount()).toBe(0);
    });

    it('cleans up all active URLs on revokeAll()', () => {
      for (let i = 0; i < 5; i++) {
        urlRegistry.create(new Blob([`data-${i}`], { type: 'image/jpeg' }));
      }
      expect(urlRegistry.getActiveCount()).toBe(5);

      urlRegistry.revokeAll();
      expect(urlRegistry.getActiveCount()).toBe(0);
    });
  });

  describe('preprocessImage', () => {
    it('preserves the original untouched File object in memory', async () => {
      const jpegBytes = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10]);
      const originalFile = new File([jpegBytes], 'crop_sample.jpg', { type: 'image/jpeg' });

      const processed = await preprocessImage(originalFile);

      // Verify the original file reference is preserved exactly as passed
      expect(processed.originalFile).toBe(originalFile);
      expect(processed.originalFile.name).toBe('crop_sample.jpg');
      expect(processed.originalFile.type).toBe('image/jpeg');
      expect(processed.metadata.wasUpscaled).toBe(false);
    });

    it('generates both diagnosticSource and displayDerivative derivatives by default', async () => {
      const file = new File(['mock-img-content'], 'leaf_blight.png', { type: 'image/png' });

      const processed = await preprocessImage(file, {
        generateDiagnosticDerivative: true,
      });

      expect(processed.displayDerivative).toBeDefined();
      expect(processed.displayDerivative.mimeType).toBeDefined();
      expect(processed.diagnosticSource).toBeDefined();
      expect(processed.diagnosticSource?.url).toBeDefined();
      expect(processed.previewUrl).toBe(processed.displayDerivative.url);
    });

    it('omits diagnosticSource when generateDiagnosticDerivative=false', async () => {
      const file = new File(['mock-img-content'], 'thumbnail_only.webp', { type: 'image/webp' });

      const processed = await preprocessImage(file, {
        generateDiagnosticDerivative: false,
      });

      expect(processed.displayDerivative).toBeDefined();
      expect(processed.diagnosticSource).toBeUndefined();
    });

    it('records processing metadata and duration accurately', async () => {
      const file = new File(['bytes'], 'tomato.jpg', { type: 'image/jpeg' });

      const processed = await preprocessImage(file);

      expect(processed.metadata.processingDurationMs).toBeGreaterThanOrEqual(0);
      expect(processed.metadata.detectedMimeType).toBe('image/jpeg');
      expect(processed.metadata.timestamp).toBeDefined();
      expect(processed.createdAt).toBeDefined();
    });

    it('releases object URLs cleanly via revokeProcessedImageUrls', async () => {
      const file = new File(['bytes'], 'apple.jpg', { type: 'image/jpeg' });
      const processed = await preprocessImage(file);

      const countBefore = urlRegistry.getActiveCount();
      expect(countBefore).toBeGreaterThan(0);

      revokeProcessedImageUrls(processed);
      expect(urlRegistry.getActiveCount()).toBe(0);
    });

    it('safely tolerates repeated and redundant revoke calls (idempotent)', async () => {
      const file = new File(['bytes'], 'orange.jpg', { type: 'image/jpeg' });
      const processed = await preprocessImage(file);

      // Repeated cleanup calls should not throw or cause errors
      expect(() => {
        revokeProcessedImageUrls(processed);
        revokeProcessedImageUrls(processed);
        revokeProcessedImageUrls(null);
        revokeProcessedImageUrls(undefined);
      }).not.toThrow();

      expect(urlRegistry.getActiveCount()).toBe(0);
    });
  });
});
