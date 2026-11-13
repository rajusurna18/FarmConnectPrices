/**
 * Image Preprocessing Utilities & Dual-Derivative Pipeline
 * FarmLink AI — Chapter 25, Part 5, Stage 3.1
 *
 * PRESERVATION PRINCIPLE:
 * The untouched `originalFile` is always retained in memory.
 * Downstream computer vision models (crop disease detection, weed detection, pest identification)
 * require fine-grained textures, leaf margins, and spore details that downsampling destroys.
 * We generate:
 * 1. An optional high-fidelity `diagnosticSource` derivative (max 2048px bounding box, Q=0.92).
 * 2. An optimized `displayDerivative` (max 800px bounding box, Q=0.80) for thumbnail rendering.
 * Smaller images are NEVER upscaled.
 */

import type {
  ImageDimension,
  ImageDerivative,
  ProcessedImage,
  ImagePreprocessingOptions,
  ImageProcessingMetadata,
} from '../types';
import { extractImageDimensions } from './imageValidation';

/**
 * Registry to track all created Object URLs for leak-free lifecycle cleanup.
 */
class ObjectUrlRegistry {
  private activeUrls = new Set<string>();

  public create(blob: Blob): string {
    const url =
      typeof window !== 'undefined' &&
      typeof window.URL !== 'undefined' &&
      typeof window.URL.createObjectURL === 'function'
        ? window.URL.createObjectURL(blob)
        : `blob:mock-url-${Math.random().toString(36).substring(2, 9)}`;
    this.activeUrls.add(url);
    return url;
  }

  public revoke(url: string): void {
    if (!url) return;
    if (this.activeUrls.has(url)) {
      if (
        typeof window !== 'undefined' &&
        typeof window.URL !== 'undefined' &&
        typeof window.URL.revokeObjectURL === 'function'
      ) {
        try {
          window.URL.revokeObjectURL(url);
        } catch {
          // Ignore errors in mock or strict environments
        }
      }
      this.activeUrls.delete(url);
    }
  }

  public revokeAll(): void {
    this.activeUrls.forEach((url) => {
      if (
        typeof window !== 'undefined' &&
        typeof window.URL !== 'undefined' &&
        typeof window.URL.revokeObjectURL === 'function'
      ) {
        try {
          window.URL.revokeObjectURL(url);
        } catch {
          // Ignore errors
        }
      }
    });
    this.activeUrls.clear();
  }

  public getActiveCount(): number {
    return this.activeUrls.size;
  }
}

export const urlRegistry = new ObjectUrlRegistry();

/**
 * Calculates scaled dimensions while preserving aspect ratio.
 * NEVER upscales images smaller than the maximum bounding box.
 */
export function calculateTargetDimensions(
  original: ImageDimension,
  maxBoundingBox: number
): { dimensions: ImageDimension; wasResized: boolean } {
  const { width, height } = original;

  if (width <= 0 || height <= 0) {
    return { dimensions: { width: 1, height: 1 }, wasResized: false };
  }

  // Never upscale smaller images
  if (width <= maxBoundingBox && height <= maxBoundingBox) {
    return { dimensions: { width, height }, wasResized: false };
  }

  const scale = Math.min(maxBoundingBox / width, maxBoundingBox / height);
  const targetWidth = Math.max(1, Math.round(width * scale));
  const targetHeight = Math.max(1, Math.round(height * scale));

  return {
    dimensions: { width: targetWidth, height: targetHeight },
    wasResized: true,
  };
}

/**
 * Decodes a File or Blob into an ImageBitmap or HTMLImageElement for canvas rendering.
 */
async function decodeImageSource(file: File | Blob): Promise<ImageBitmap | HTMLImageElement> {
  // 1. Prefer createImageBitmap
  if (typeof window !== 'undefined' && typeof window.createImageBitmap === 'function') {
    try {
      return await window.createImageBitmap(file, { imageOrientation: 'from-image' });
    } catch {
      // Fall through to HTMLImageElement on browser decode quirks
    }
  }

  // 2. Fallback to HTMLImageElement
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined' || typeof window.Image === 'undefined') {
      // Node.js mock fallback
      const mockElement = { width: 800, height: 600 } as unknown as HTMLImageElement;
      resolve(mockElement);
      return;
    }

    const img = new window.Image();
    const tempUrl = urlRegistry.create(file);

    img.onload = () => {
      urlRegistry.revoke(tempUrl);
      resolve(img);
    };

    img.onerror = () => {
      urlRegistry.revoke(tempUrl);
      reject(new Error('Failed to load image element for canvas processing.'));
    };

    img.src = tempUrl;
  });
}

/**
 * Encodes a canvas to Blob with WebP preference and automatic JPEG fallback.
 * Accurately detects and returns the resulting MIME type.
 */
async function canvasToBlobSafe(
  canvas: HTMLCanvasElement,
  preferWebP: boolean,
  quality: number
): Promise<{ blob: Blob; mimeType: string }> {
  const targetMime = preferWebP ? 'image/webp' : 'image/jpeg';

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob && blob.size > 0) {
          // Verify if browser genuinely encoded as WebP or silently fell back
          const actualMime = blob.type || targetMime;
          resolve({ blob, mimeType: actualMime });
          return;
        }

        // Fallback: If WebP failed or returned null, retry with standard JPEG
        if (preferWebP) {
          canvas.toBlob(
            (fallbackBlob) => {
              if (fallbackBlob && fallbackBlob.size > 0) {
                resolve({ blob: fallbackBlob, mimeType: 'image/jpeg' });
              } else {
                reject(new Error('Canvas export to JPEG fallback failed.'));
              }
            },
            'image/jpeg',
            quality
          );
        } else {
          reject(new Error('Canvas export to blob failed.'));
        }
      },
      targetMime,
      quality
    );
  });
}

/**
 * Generates an optimized derivative on canvas.
 */
async function generateDerivative(
  source: ImageBitmap | HTMLImageElement,
  targetDimensions: ImageDimension,
  preferWebP: boolean,
  quality: number
): Promise<ImageDerivative> {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    // Node.js test environment mock
    const mockBlob = new Blob(['mock-binary-data'], { type: preferWebP ? 'image/webp' : 'image/jpeg' });
    const url = urlRegistry.create(mockBlob);
    return {
      blob: mockBlob,
      url,
      width: targetDimensions.width,
      height: targetDimensions.height,
      sizeBytes: mockBlob.size,
      mimeType: mockBlob.type,
    };
  }

  const canvas = document.createElement('canvas');
  canvas.width = targetDimensions.width;
  canvas.height = targetDimensions.height;

  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('Could not acquire 2D rendering context from canvas.');
  }

  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(source, 0, 0, targetDimensions.width, targetDimensions.height);

  const { blob, mimeType } = await canvasToBlobSafe(canvas, preferWebP, quality);
  const url = urlRegistry.create(blob);

  return {
    blob,
    url,
    width: targetDimensions.width,
    height: targetDimensions.height,
    sizeBytes: blob.size,
    mimeType,
  };
}

/**
 * Preprocesses an image file through the dual-derivative pipeline.
 * Preserves the original file untouched in component memory.
 */
export async function preprocessImage(
  file: File,
  options: ImagePreprocessingOptions = {}
): Promise<ProcessedImage> {
  const startTime = Date.now();
  const {
    diagnosticMaxDimension = 2048,
    diagnosticQuality = 0.92,
    displayMaxDimension = 800,
    displayQuality = 0.80,
    generateDiagnosticDerivative = true,
    preferWebP = true,
  } = options;

  // 1. Extract dimensions from original file
  const originalDimensions = await extractImageDimensions(file);

  // 2. Decode source for canvas rendering
  const source = await decodeImageSource(file);

  try {
    // 3. Generate Display Derivative (Max 800px)
    const displayTarget = calculateTargetDimensions(originalDimensions, displayMaxDimension);
    const displayDerivative = await generateDerivative(
      source,
      displayTarget.dimensions,
      preferWebP,
      displayQuality
    );

    // 4. Generate Diagnostic Derivative (Max 2048px, high quality)
    let diagnosticSource: ImageDerivative | undefined;
    if (generateDiagnosticDerivative) {
      try {
        const diagTarget = calculateTargetDimensions(originalDimensions, diagnosticMaxDimension);
        diagnosticSource = await generateDerivative(
          source,
          diagTarget.dimensions,
          preferWebP,
          diagnosticQuality
        );
      } catch (diagErr) {
        // Clean up display derivative so failed preprocessing leaves zero orphaned URLs
        urlRegistry.revoke(displayDerivative.url);
        throw diagErr;
      }
    }

    const duration = Date.now() - startTime;

    const metadata: ImageProcessingMetadata = {
      detectedMimeType: file.type || 'image/jpeg',
      outputMimeType: displayDerivative.mimeType,
      wasResized: displayTarget.wasResized,
      wasUpscaled: false,
      processingDurationMs: duration,
      timestamp: new Date().toISOString(),
    };

    return {
      id: `img_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      originalFile: file,
      originalDimensions,
      diagnosticSource,
      displayDerivative,
      previewUrl: displayDerivative.url,
      createdAt: new Date().toISOString(),
      metadata,
    };
  } finally {
    // Clean up ImageBitmap memory if applicable
    if (typeof (source as ImageBitmap).close === 'function') {
      (source as ImageBitmap).close();
    }
  }
}

/**
 * Safely releases object URLs for a ProcessedImage instance.
 */
export function revokeProcessedImageUrls(image?: ProcessedImage | null): void {
  if (!image) return;
  if (image.displayDerivative?.url) {
    urlRegistry.revoke(image.displayDerivative.url);
  }
  if (image.diagnosticSource?.url) {
    urlRegistry.revoke(image.diagnosticSource.url);
  }
}
