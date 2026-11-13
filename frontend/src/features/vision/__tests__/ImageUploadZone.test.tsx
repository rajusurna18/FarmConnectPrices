/**
 * ImageUploadZone UI & Component Tests
 * FarmLink AI — Chapter 25, Part 5, Stage 3.1
 *
 * NOTE ON TESTING:
 * Includes both renderToString markup verification and asynchronous
 * object URL lifecycle and unmount cleanup verification.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderToString } from 'react-dom/server';
import { ImageUploadZone } from '../components/ImageUploadZone';
import { useImageUploadZone } from '../hooks/useImageUploadZone';
import { ImagePreviewCard } from '../components/ImagePreviewCard';
import { urlRegistry, revokeProcessedImageUrls } from '../utils/imagePreprocessing';
import type { ProcessedImage } from '../types';
import { renderHook, act } from './testUtils';

function createMockProcessedImage(id: string, name: string): ProcessedImage {
  const blob = new Blob([`content-${id}`], { type: 'image/webp' });
  const displayUrl = urlRegistry.create(blob);
  const diagUrl = urlRegistry.create(blob);
  const file = new File([`raw-${id}`], name, { type: 'image/jpeg' });

  return {
    id,
    originalFile: file,
    originalDimensions: { width: 1920, height: 1080 },
    displayDerivative: {
      blob,
      url: displayUrl,
      width: 800,
      height: 450,
      sizeBytes: 15000,
      mimeType: 'image/webp',
    },
    diagnosticSource: {
      blob,
      url: diagUrl,
      width: 1920,
      height: 1080,
      sizeBytes: 85000,
      mimeType: 'image/webp',
    },
    previewUrl: displayUrl,
    createdAt: new Date().toISOString(),
    metadata: {
      detectedMimeType: 'image/jpeg',
      outputMimeType: 'image/webp',
      wasResized: true,
      wasUpscaled: false,
      processingDurationMs: 15,
      timestamp: new Date().toISOString(),
    },
  };
}

describe('Vision AI — ImageUploadZone Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    urlRegistry.revokeAll();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    urlRegistry.revokeAll();
  });

  describe('Static Markup & Accessible Attributes', () => {
    it('renders upload zone with accessible label, description, and format tags', () => {
      const html = renderToString(
        <ImageUploadZone
          label="Upload Crop Pathology Photos"
          description="Select high-resolution leaf photos."
          maxFiles={3}
        />
      );

      expect(html).toContain('Upload Crop Pathology Photos');
      expect(html).toContain('Select high-resolution leaf photos.');
      expect(html).toContain('JPEG, PNG, WebP');
      expect(html).toContain('0 / 3 images');
      expect(html).toContain('role="button"');
      expect(html).toContain('aria-label="Upload Crop Pathology Photos"');
    });

    it('renders hidden file input with accepted formats and multiple flag', () => {
      const html = renderToString(<ImageUploadZone maxFiles={5} />);

      expect(html).toContain('type="file"');
      expect(html).toContain('accept="image/jpeg,image/png,image/webp"');
      expect(html).toContain('multiple=""');
      expect(html).toContain('class="hidden"');
    });

    it('renders disabled state with appropriate ARIA and opacity classes', () => {
      const html = renderToString(
        <ImageUploadZone disabled={true} label="Disabled Uploader" />
      );

      expect(html).toContain('aria-disabled="true"');
      expect(html).toContain('opacity-40');
      expect(html).toContain('cursor-not-allowed');
    });

    it('renders single-file mode without multiple attribute when maxFiles=1', () => {
      const html = renderToString(<ImageUploadZone maxFiles={1} />);
      expect(html).toContain('0 / 1 images');
      expect(html).not.toContain('multiple=""');
    });
  });

  describe('ImagePreviewCard Component', () => {
    const mockProcessedImage: ProcessedImage = {
      id: 'img-123',
      originalFile: new File(['bytes'], 'diseased_tomato_leaf.jpg', { type: 'image/jpeg' }),
      originalDimensions: { width: 3840, height: 2160 },
      displayDerivative: {
        blob: new Blob(['thumb'], { type: 'image/webp' }),
        url: 'blob:mock-display-url',
        width: 800,
        height: 450,
        sizeBytes: 45000,
        mimeType: 'image/webp',
      },
      diagnosticSource: {
        blob: new Blob(['diag'], { type: 'image/webp' }),
        url: 'blob:mock-diag-url',
        width: 2048,
        height: 1152,
        sizeBytes: 350000,
        mimeType: 'image/webp',
      },
      previewUrl: 'blob:mock-display-url',
      createdAt: '2026-11-13T19:00:00Z',
      metadata: {
        detectedMimeType: 'image/jpeg',
        outputMimeType: 'image/webp',
        wasResized: true,
        wasUpscaled: false,
        processingDurationMs: 42,
        timestamp: '2026-11-13T19:00:00Z',
      },
    };

    it('renders preview card with thumbnail image and original file name', () => {
      const html = renderToString(
        <ImagePreviewCard image={mockProcessedImage} onRemove={vi.fn()} />
      );

      expect(html).toContain('diseased_tomato_leaf.jpg');
      expect(html).toContain('src="blob:mock-display-url"');
      expect(html).toContain('role="group"');
      expect(html).toContain('aria-label="Image preview: diseased_tomato_leaf.jpg"');
    });

    it('displays format badge, dimensions, and diagnostic preservation notice', () => {
      const html = renderToString(
        <ImagePreviewCard image={mockProcessedImage} onRemove={vi.fn()} />
      );

      expect(html).toContain('WebP');
      expect(html).toContain('3840×2160');
      expect(html).toContain('High-Res Diagnostic Preserved');
    });

    it('renders accessible remove button with descriptive aria-label', () => {
      const html = renderToString(
        <ImagePreviewCard image={mockProcessedImage} onRemove={vi.fn()} />
      );

      expect(html).toContain('aria-label="Remove image diseased_tomato_leaf.jpg"');
    });

    it('respects disabled prop on remove button', () => {
      const html = renderToString(
        <ImagePreviewCard image={mockProcessedImage} onRemove={vi.fn()} disabled={true} />
      );

      expect(html).toContain('disabled=""');
    });
  });

  describe('Async Lifecycle, Unmount & Object URL Cleanup', () => {
    it('revokes newly created object URLs if preprocessing finishes after unmount', async () => {
      let resolvePreprocess: (value: ProcessedImage) => void;
      const preprocessPromise = new Promise<ProcessedImage>((resolve) => {
        resolvePreprocess = resolve;
      });

      const onImagesChange = vi.fn();
      let unmountFn: () => void = () => {};

      const { result, unmount } = renderHook(() =>
        useImageUploadZone({
          onImagesChange,
          validateFn: async () => ({ isValid: true, detectedFormat: 'image/jpeg' }),
          preprocessFn: () => {
            unmountFn();
            return preprocessPromise;
          },
        })
      );
      unmountFn = unmount;

      const mockImage = createMockProcessedImage('img-deferred', 'leaf.jpg');
      expect(urlRegistry.getActiveCount()).toBe(2);

      const file = new File(['test'], 'leaf.jpg', { type: 'image/jpeg' });

      // Start asynchronous file processing
      let handleDone = false;
      const handlePromise = result.current.handleFiles([file]).then(() => {
        handleDone = true;
      });

      // Allow handleFiles microtasks to advance and reach preprocessFn
      await new Promise((r) => setTimeout(r, 15));

      // Resolve the deferred preprocessing promise after unmount
      resolvePreprocess!(mockImage);
      await handlePromise;

      expect(handleDone).toBe(true);

      // Verify that every newly created object URL was immediately revoked
      expect(urlRegistry.getActiveCount()).toBe(0);

      // State updates should NOT occur after unmount
      expect(onImagesChange).not.toHaveBeenCalled();
    });

    it('prevents any state updates after unmount', async () => {
      let resolveValidation: (value: { isValid: boolean; error?: string }) => void;
      const validationPromise = new Promise<{ isValid: boolean; error?: string }>((resolve) => {
        resolveValidation = resolve;
      });

      let unmountFn: () => void = () => {};
      const onImagesChange = vi.fn();
      const onError = vi.fn();
      const { result, unmount } = renderHook(() =>
        useImageUploadZone({
          onImagesChange,
          onError,
          validateFn: () => {
            unmountFn();
            return validationPromise as unknown as Promise<{ isValid: boolean; detectedFormat: null; error?: string }>;
          },
        })
      );
      unmountFn = unmount;

      const file = new File(['test'], 'leaf.jpg', { type: 'image/jpeg' });
      const handlePromise = result.current.handleFiles([file]);

      // Allow event loop tick
      await new Promise((r) => setTimeout(r, 10));

      // Resolve with an error after unmount
      resolveValidation!({ isValid: false, error: 'Invalid file' });
      await handlePromise;

      // When unmounted, onError and state changes should be suppressed
      expect(onImagesChange).not.toHaveBeenCalled();
      expect(onError).not.toHaveBeenCalled();
      expect(result.current.images).toHaveLength(0);
    });

    it('immediately revokes URLs when url creation is followed by cleanup', () => {
      const mockImage = createMockProcessedImage('img-instant', 'crop.jpg');
      expect(urlRegistry.getActiveCount()).toBe(2);

      revokeProcessedImageUrls(mockImage);
      expect(urlRegistry.getActiveCount()).toBe(0);
    });

    it('releases only the removed image URLs while preserving other previews', async () => {
      const img1 = createMockProcessedImage('img-1', 'crop1.jpg');
      const img2 = createMockProcessedImage('img-2', 'crop2.jpg');
      expect(urlRegistry.getActiveCount()).toBe(4);

      let preprocessCallCount = 0;
      const onImagesChange = vi.fn();
      const { result } = renderHook(() =>
        useImageUploadZone({
          maxFiles: 5,
          onImagesChange,
          validateFn: async () => ({ isValid: true, detectedFormat: 'image/jpeg' }),
          preprocessFn: async () => {
            preprocessCallCount++;
            return preprocessCallCount === 1 ? img1 : img2;
          },
        })
      );

      const file1 = new File(['1'], 'crop1.jpg', { type: 'image/jpeg' });
      const file2 = new File(['2'], 'crop2.jpg', { type: 'image/jpeg' });

      await act(async () => {
        await result.current.handleFiles([file1, file2]);
      });

      expect(result.current.images).toHaveLength(2);
      expect(urlRegistry.getActiveCount()).toBe(4);

      // Now remove img-1
      await act(async () => {
        result.current.handleRemoveImage('img-1');
      });

      // img-1 URLs revoked (2), img-2 URLs preserved (2)
      expect(urlRegistry.getActiveCount()).toBe(2);
      expect(result.current.images).toHaveLength(1);
      expect(result.current.images[0].id).toBe('img-2');
    });

    it('safely handles repeated cleanup calls on unmount and image removal without errors', () => {
      const img = createMockProcessedImage('img-safe', 'test.jpg');
      expect(urlRegistry.getActiveCount()).toBe(2);

      expect(() => {
        revokeProcessedImageUrls(img);
        revokeProcessedImageUrls(img); // Second call
        revokeProcessedImageUrls(img); // Third call
      }).not.toThrow();

      expect(urlRegistry.getActiveCount()).toBe(0);

      const { unmount } = renderHook(() => useImageUploadZone());
      expect(() => {
        unmount();
        unmount();
      }).not.toThrow();
    });

    it('revokes partial newly processed images if an error interrupts batch processing', async () => {
      const img1 = createMockProcessedImage('img-partial', 'p1.jpg');
      expect(urlRegistry.getActiveCount()).toBe(2);

      let count = 0;
      const onError = vi.fn();
      const { result } = renderHook(() =>
        useImageUploadZone({
          maxFiles: 5,
          onError,
          validateFn: async () => ({ isValid: true, detectedFormat: 'image/jpeg' }),
          preprocessFn: async () => {
            count++;
            if (count === 1) return img1;
            throw new Error('Preprocessing engine crash');
          },
        })
      );

      const file1 = new File(['1'], 'p1.jpg', { type: 'image/jpeg' });
      const file2 = new File(['2'], 'p2.jpg', { type: 'image/jpeg' });

      await act(async () => {
        await result.current.handleFiles([file1, file2]);
      });

      // File 2 error reported to onError
      expect(onError).toHaveBeenCalledWith(expect.stringContaining('Preprocessing engine crash'));
      // File 1 succeeded and was added
      expect(result.current.images).toHaveLength(1);
      expect(urlRegistry.getActiveCount()).toBe(2);
    });
  });
});
