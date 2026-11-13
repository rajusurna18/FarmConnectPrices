import { useState, useRef, useEffect, useCallback } from 'react';
import type {
  ProcessedImage,
  ImageUploadZoneProps,
  ImageValidationOptions,
  ImageValidationResult,
  ImagePreprocessingOptions,
} from '../types';
import { MAX_IMAGE_SIZE_BYTES } from '../types';
import * as imageValidation from '../utils/imageValidation';
import * as imagePreprocessing from '../utils/imagePreprocessing';

export interface UseImageUploadZoneReturn {
  images: ProcessedImage[];
  isDragging: boolean;
  isProcessing: boolean;
  errorMessage: string | null;
  fileInputRef: React.RefObject<HTMLInputElement | null>;
  handleFiles: (fileList: FileList | File[]) => Promise<void>;
  handleRemoveImage: (id: string) => void;
  handleDrop: (e: React.DragEvent<HTMLDivElement>) => void;
  handleDragOver: (e: React.DragEvent<HTMLDivElement>) => void;
  handleDragLeave: (e: React.DragEvent<HTMLDivElement>) => void;
  handleZoneClick: () => void;
  handleKeyDown: (e: React.KeyboardEvent<HTMLDivElement>) => void;
  isAtCapacity: boolean;
}

export interface UseImageUploadZoneOptions extends Partial<ImageUploadZoneProps> {
  validateFn?: (file: File, options?: ImageValidationOptions) => Promise<ImageValidationResult>;
  preprocessFn?: (file: File, options?: ImagePreprocessingOptions) => Promise<ProcessedImage>;
}

export function useImageUploadZone({
  onImagesChange,
  onError,
  maxFiles = 5,
  maxSizeBytes = MAX_IMAGE_SIZE_BYTES,
  disabled = false,
  generateDiagnosticDerivative = true,
  validateFn = imageValidation.validateImageFile,
  preprocessFn = imagePreprocessing.preprocessImage,
}: UseImageUploadZoneOptions = {}): UseImageUploadZoneReturn {
  const [images, setImages] = useState<ProcessedImage[]>([]);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const isMountedRef = useRef<boolean>(true);
  const imagesRef = useRef<ProcessedImage[]>(images);
  imagesRef.current = images;

  // Cleanup all object URLs on component unmount and track unmounted lifecycle
  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
      imagesRef.current.forEach((img) => {
        imagePreprocessing.revokeProcessedImageUrls(img);
      });
    };
  }, []);

  const handleFiles = useCallback(
    async (fileList: FileList | File[]) => {
      if (disabled || isProcessing || !isMountedRef.current) return;

      if (isMountedRef.current) {
        setErrorMessage(null);
      }
      const incomingFiles = Array.from(fileList);

      if (incomingFiles.length === 0) return;

      // Check max files quota
      const remainingSlots = maxFiles - imagesRef.current.length;
      if (remainingSlots <= 0) {
        const msg = `Maximum limit of ${maxFiles} images already reached. Remove an image before adding more.`;
        if (isMountedRef.current) {
          setErrorMessage(msg);
        }
        onError?.(msg);
        return;
      }

      const filesToProcess = incomingFiles.slice(0, remainingSlots);
      if (incomingFiles.length > remainingSlots) {
        if (isMountedRef.current) {
          setErrorMessage(`Only ${remainingSlots} image(s) could be added to stay within the limit of ${maxFiles}.`);
        }
      }

      if (isMountedRef.current) {
        setIsProcessing(true);
      }

      const newlyProcessed: ProcessedImage[] = [];
      let abortedOrUnmounted = false;

      try {
        for (const file of filesToProcess) {
          if (!isMountedRef.current) {
            abortedOrUnmounted = true;
            break;
          }

          // 1. Validate file (Size, binary magic bytes signature, dimensions)
          const validation = await validateFn(file, { maxSizeBytes });
          if (!isMountedRef.current) {
            abortedOrUnmounted = true;
            break;
          }

          if (!validation.isValid) {
            const errorText = `${file.name}: ${validation.error || 'Invalid image file.'}`;
            if (isMountedRef.current) {
              setErrorMessage(errorText);
            }
            onError?.(errorText);
            continue;
          }

          // 2. Preprocess through dual-derivative pipeline
          try {
            const processed = await preprocessFn(file, {
              generateDiagnosticDerivative,
            });

            // If component unmounted while preprocessing was pending:
            if (!isMountedRef.current) {
              // Immediately revoke newly created object URLs
              imagePreprocessing.revokeProcessedImageUrls(processed);
              abortedOrUnmounted = true;
              break;
            }

            newlyProcessed.push(processed);
          } catch (err: unknown) {
            if (!isMountedRef.current) {
              abortedOrUnmounted = true;
              break;
            }
            const msg = err instanceof Error ? err.message : 'Failed to process image.';
            if (isMountedRef.current) {
              setErrorMessage(`${file.name}: ${msg}`);
            }
            onError?.(`${file.name}: ${msg}`);
          }
        }
      } catch (err: unknown) {
        abortedOrUnmounted = true;
        // Clean up any newly created images in case of unexpected exception
        newlyProcessed.forEach((img) => imagePreprocessing.revokeProcessedImageUrls(img));
        throw err;
      } finally {
        if (!isMountedRef.current || abortedOrUnmounted) {
          // Preprocessing finished after unmount or was interrupted!
          // Immediately revoke all newly created object URLs
          newlyProcessed.forEach((img) => {
            imagePreprocessing.revokeProcessedImageUrls(img);
          });
          // Do NOT perform any React state updates after unmount
        } else {
          setIsProcessing(false);

          if (newlyProcessed.length > 0) {
            const updated = [...imagesRef.current, ...newlyProcessed];
            setImages(updated);
            onImagesChange?.(updated);
          }

          // Reset file input value so selecting the same file again triggers onChange
          if (fileInputRef.current) {
            fileInputRef.current.value = '';
          }
        }
      }
    },
    [disabled, isProcessing, maxFiles, maxSizeBytes, generateDiagnosticDerivative, onError, onImagesChange, validateFn, preprocessFn]
  );

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (isMountedRef.current) {
      setIsDragging(false);
    }

    if (disabled || !isMountedRef.current) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled && !isDragging && isMountedRef.current) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (isMountedRef.current) {
      setIsDragging(false);
    }
  };

  const handleRemoveImage = (id: string) => {
    const target = imagesRef.current.find((img) => img.id === id);
    if (target) {
      imagePreprocessing.revokeProcessedImageUrls(target);
    }

    const updated = imagesRef.current.filter((img) => img.id !== id);
    if (isMountedRef.current) {
      setImages(updated);
      setErrorMessage(null);
    }
    onImagesChange?.(updated);
  };

  const handleZoneClick = () => {
    if (!disabled && !isProcessing && isMountedRef.current && fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (disabled || isProcessing || !isMountedRef.current) return;
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleZoneClick();
    }
  };

  const isAtCapacity = images.length >= maxFiles;

  return {
    images,
    isDragging,
    isProcessing,
    errorMessage,
    fileInputRef,
    handleFiles,
    handleRemoveImage,
    handleDrop,
    handleDragOver,
    handleDragLeave,
    handleZoneClick,
    handleKeyDown,
    isAtCapacity,
  };
}
