/**
 * Computer Vision & Image Processing Domain Types
 * FarmLink AI — Chapter 25, Part 5, Stage 3.1
 */

export type SupportedImageFormat = 'image/jpeg' | 'image/png' | 'image/webp';

export const SUPPORTED_MIME_TYPES: readonly SupportedImageFormat[] = [
  'image/jpeg',
  'image/png',
  'image/webp',
];

export const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
export const MAX_SAFE_IMAGE_PIXELS = 25_000_000; // 25 Megapixels (decompression bomb protection cap)
export const MAX_IMAGE_DIMENSION_PX = 6000; // Maximum single axis dimension in pixels
export const MAX_ASPECT_RATIO = 20; // Reject extreme aspect ratios (e.g. 1 x 50000 px)

export interface ImageDimension {
  width: number;
  height: number;
}

export interface ImageDerivative {
  blob: Blob;
  url: string;
  width: number;
  height: number;
  sizeBytes: number;
  mimeType: string;
}

export interface ImageProcessingMetadata {
  detectedMimeType: string;
  outputMimeType: string;
  wasResized: boolean;
  wasUpscaled: boolean;
  processingDurationMs: number;
  timestamp: string;
}

export interface ProcessedImage {
  id: string;
  originalFile: File;
  originalDimensions: ImageDimension;
  /**
   * High-fidelity derivative preserving microscopic lesion/spore visual detail for downstream CV models.
   * Max 2048px bounding box, unscaled if original is smaller.
   */
  diagnosticSource?: ImageDerivative;
  /**
   * Lightweight display derivative optimized for UI rendering and thumbnail cards.
   * Max 800px bounding box.
   */
  displayDerivative: ImageDerivative;
  previewUrl: string;
  createdAt: string;
  metadata: ImageProcessingMetadata;
}

export interface ImageValidationResult {
  isValid: boolean;
  detectedFormat: SupportedImageFormat | null;
  dimensions?: ImageDimension;
  error?: string;
}

export interface ImageValidationOptions {
  maxSizeBytes?: number;
  maxPixels?: number;
  maxDimensionPx?: number;
  maxAspectRatio?: number;
}

export interface ImagePreprocessingOptions {
  diagnosticMaxDimension?: number;
  diagnosticQuality?: number;
  displayMaxDimension?: number;
  displayQuality?: number;
  generateDiagnosticDerivative?: boolean;
  preferWebP?: boolean;
}

export interface ImageUploadZoneProps {
  onImagesChange?: (images: ProcessedImage[]) => void;
  onError?: (error: string) => void;
  maxFiles?: number;
  maxSizeBytes?: number;
  disabled?: boolean;
  className?: string;
  label?: string;
  description?: string;
  generateDiagnosticDerivative?: boolean;
}

export interface ImagePreviewCardProps {
  image: ProcessedImage;
  onRemove: (id: string) => void;
  disabled?: boolean;
}

