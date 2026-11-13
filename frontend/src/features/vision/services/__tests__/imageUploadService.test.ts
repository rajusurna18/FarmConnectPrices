import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  validateEntityId,
  resolveStorageFileName,
  getAuthoritativeAuthenticatedUid,
  uploadDiagnosticImage,
  uploadListingStagingImage,
  uploadDisputeEvidenceImage,
  StorageAuthenticationError,
  StorageValidationError,
  StorageUploadAbortedError,
  StoragePermissionError,
} from '../imageUploadService';
import type { ProcessedImage } from '../../types';
import type { FirebaseStorage, StorageReference, UploadTask, UploadTaskSnapshot } from 'firebase/storage';
import type { Auth, User } from 'firebase/auth';

describe('Computer Vision Storage Upload Service (imageUploadService.ts)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  // Helper to build a valid ProcessedImage fixture
  function createProcessedImageFixture(overrides?: Partial<ProcessedImage>): ProcessedImage {
    const originalFile = new File(['mock-raw-binary'], 'sample_leaf.jpg', { type: 'image/jpeg' });
    const originalDimensions = { width: 3000, height: 2000 };
    const displayBlob = new Blob(['mock-display-binary'], { type: 'image/webp' });
    const diagnosticBlob = new Blob(['mock-diagnostic-binary'], { type: 'image/webp' });

    return {
      id: 'img-fixture-1',
      originalFile,
      originalDimensions,
      diagnosticSource: {
        blob: diagnosticBlob,
        url: 'blob:http://localhost/diagnostic-uuid',
        width: 2048,
        height: 1365,
        sizeBytes: diagnosticBlob.size,
        mimeType: 'image/webp',
      },
      displayDerivative: {
        blob: displayBlob,
        url: 'blob:http://localhost/display-uuid',
        width: 800,
        height: 533,
        sizeBytes: displayBlob.size,
        mimeType: 'image/webp',
      },
      previewUrl: 'blob:http://localhost/preview-uuid',
      createdAt: '2026-11-13T20:00:00.000Z',
      metadata: {
        detectedMimeType: 'image/jpeg',
        outputMimeType: 'image/webp',
        wasResized: true,
        wasUpscaled: false,
        processingDurationMs: 45,
        timestamp: '2026-11-13T20:00:00.000Z',
      },
      ...overrides,
    };
  }

  // Mock UploadTask generator
  function createMockUploadTask(options?: {
    shouldFail?: boolean;
    errorCode?: string;
    errorMessage?: string;
    emitProgress?: boolean;
    cancelBehavior?: () => void;
  }): {
    uploadTask: UploadTask;
    triggerSuccess: () => void;
    triggerError: (code: string, message: string) => void;
  } {
    let stateChangedCb: ((snapshot: UploadTaskSnapshot) => void) | undefined;
    let errorCb: ((err: { code?: string; message?: string }) => void) | undefined;
    let completeCb: (() => void) | undefined;

    const cancelFn = vi.fn(() => {
      if (options?.cancelBehavior) {
        options.cancelBehavior();
      }
      if (errorCb) {
        errorCb({ code: 'storage/canceled', message: 'User canceled upload' });
      }
    });

    const uploadTask: Partial<UploadTask> = {
      cancel: cancelFn,
      on: vi.fn((event: string, onProgress?: unknown, onError?: unknown, onComplete?: unknown) => {
        if (event === 'state_changed') {
          stateChangedCb = onProgress as (snapshot: UploadTaskSnapshot) => void;
          errorCb = onError as (err: { code?: string; message?: string }) => void;
          completeCb = onComplete as () => void;

          // Progress trigger
          if (options?.emitProgress && stateChangedCb) {
            stateChangedCb({
              bytesTransferred: 50,
              totalBytes: 100,
            } as UploadTaskSnapshot);
          }

          // Automatic resolution / rejection if configured
          if (options?.shouldFail) {
            setTimeout(() => {
              if (errorCb) {
                errorCb({
                  code: options.errorCode || 'storage/unauthorized',
                  message: options.errorMessage || 'Storage access denied',
                });
              }
            }, 0);
          } else if (options?.shouldFail === false) {
            setTimeout(() => {
              if (completeCb) completeCb();
            }, 0);
          }
        }
        return () => {};
      }) as unknown as UploadTask['on'],
    };

    return {
      uploadTask: uploadTask as UploadTask,
      triggerSuccess: () => {
        if (completeCb) completeCb();
      },
      triggerError: (code: string, message: string) => {
        if (errorCb) errorCb({ code, message });
      },
    };
  }

  describe('validateEntityId', () => {
    it('accepts valid 1-64 character alphanumeric, hyphen and underscore IDs', () => {
      expect(() => validateEntityId('diag-123_abc', 'diagnosticId')).not.toThrow();
      expect(() => validateEntityId('a', 'draftId')).not.toThrow();
      expect(() => validateEntityId('a'.repeat(64), 'imageId')).not.toThrow();
    });

    it('rejects invalid, empty, or path-traversing identifiers', () => {
      expect(() => validateEntityId('', 'diagnosticId')).toThrowError(StorageValidationError);
      expect(() => validateEntityId('   ', 'draftId')).toThrowError(StorageValidationError);
      expect(() => validateEntityId('../traversal', 'imageId')).toThrowError(StorageValidationError);
      expect(() => validateEntityId('nested/path', 'disputeId')).toThrowError(StorageValidationError);
      expect(() => validateEntityId('dotted.id', 'evidenceId')).toThrowError(StorageValidationError);
      expect(() => validateEntityId('a'.repeat(65), 'diagnosticId')).toThrowError(StorageValidationError);
    });
  });

  describe('resolveStorageFileName', () => {
    it('resolves original filenames with jpg, png, or webp extension', () => {
      expect(resolveStorageFileName('original', 'image/jpeg')).toBe('original.jpg');
      expect(resolveStorageFileName('original', 'image/png')).toBe('original.png');
      expect(resolveStorageFileName('original', 'image/webp')).toBe('original.webp');
    });

    it('resolves diagnostic filenames with webp or jpg extension', () => {
      expect(resolveStorageFileName('diagnostic', 'image/webp')).toBe('diagnostic.webp');
      expect(resolveStorageFileName('diagnostic', 'image/jpeg')).toBe('diagnostic.jpg');
    });

    it('rejects PNG for diagnostic and display derivatives', () => {
      expect(() => resolveStorageFileName('diagnostic', 'image/png')).toThrowError(
        /Derivatives cannot be PNG format/
      );
      expect(() => resolveStorageFileName('display', 'image/png')).toThrowError(
        /Derivatives cannot be PNG format/
      );
    });

    it('rejects unsupported MIME types', () => {
      expect(() => resolveStorageFileName('original', 'image/gif')).toThrowError(
        /Unsupported MIME type for storage upload/
      );
      expect(() => resolveStorageFileName('original', 'application/pdf')).toThrowError(
        /Unsupported MIME type for storage upload/
      );
    });
  });

  describe('getAuthoritativeAuthenticatedUid', () => {
    it('awaits authStateReady and returns the current user UID', async () => {
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-uid-123' } as User,
      } as unknown as Auth;

      const uid = await getAuthoritativeAuthenticatedUid(mockAuth);
      expect(uid).toBe('farmer-uid-123');
      expect(mockAuth.authStateReady).toHaveBeenCalledTimes(1);
    });

    it('throws StorageAuthenticationError when currentUser is null after authStateReady', async () => {
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: null,
      } as unknown as Auth;

      await expect(getAuthoritativeAuthenticatedUid(mockAuth)).rejects.toThrowError(
        StorageAuthenticationError
      );
    });

    it('throws StorageAuthenticationError when authStateReady rejects', async () => {
      const mockAuth = {
        authStateReady: vi.fn().mockRejectedValue(new Error('IndexedDB network failure')),
        currentUser: null,
      } as unknown as Auth;

      await expect(getAuthoritativeAuthenticatedUid(mockAuth)).rejects.toThrowError(
        /Firebase Auth initialization failure/
      );
    });
  });

  describe('uploadDiagnosticImage', () => {
    it('uploads raw original file and both derivatives to users/{uid}/diagnostics/{diagnosticId}/', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const uploadedRefs: string[] = [];

      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => {
        return {
          fullPath: path || '',
          name: path?.split('/').pop() || '',
        } as StorageReference;
      });

      const uploadBytesResumableFn = vi.fn((ref: StorageReference) => {
        uploadedRefs.push(ref.fullPath);
        const { uploadTask, triggerSuccess } = createMockUploadTask();
        setTimeout(() => triggerSuccess(), 0);
        return uploadTask;
      });

      const onProgress = vi.fn();
      const result = await uploadDiagnosticImage(
        image,
        'diag-crop-100',
        { onProgress },
        { authInstance: mockAuth, storageInstance: mockStorage, refFn, uploadBytesResumableFn }
      );

      expect(result.diagnosticId).toBe('diag-crop-100');
      expect(result.original.storagePath).toBe('users/farmer-1/diagnostics/diag-crop-100/original.jpg');
      expect(result.original.mimeType).toBe('image/jpeg');
      expect(result.diagnostic.storagePath).toBe('users/farmer-1/diagnostics/diag-crop-100/diagnostic.webp');
      expect(result.diagnostic.mimeType).toBe('image/webp');
      expect(result.display.storagePath).toBe('users/farmer-1/diagnostics/diag-crop-100/display.webp');
      expect(result.display.mimeType).toBe('image/webp');

      expect(uploadedRefs).toEqual([
        'users/farmer-1/diagnostics/diag-crop-100/original.jpg',
        'users/farmer-1/diagnostics/diag-crop-100/diagnostic.webp',
        'users/farmer-1/diagnostics/diag-crop-100/display.webp',
      ]);
    });

    it('throws StorageValidationError if diagnosticSource is missing on ProcessedImage', async () => {
      const image = createProcessedImageFixture({ diagnosticSource: undefined });
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      await expect(
        uploadDiagnosticImage(image, 'diag-crop-1', undefined, { authInstance: mockAuth })
      ).rejects.toThrowError(/Diagnostic upload requires a diagnosticSource derivative/);
    });

    it('rolls back completed files from current invocation when a subsequent derivative fails', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const deletedPaths: string[] = [];
      const deleteObjectFn = vi.fn(async (fileRef: StorageReference) => {
        deletedPaths.push(fileRef.fullPath);
      });

      let callCount = 0;
      const uploadBytesResumableFn = vi.fn(() => {
        callCount++;
        const { uploadTask, triggerSuccess, triggerError } = createMockUploadTask();
        if (callCount <= 2) {
          // original and diagnostic succeed
          setTimeout(() => triggerSuccess(), 0);
        } else {
          // display derivative fails
          setTimeout(() => triggerError('storage/unauthorized', 'Permission denied'), 0);
        }
        return uploadTask;
      });

      await expect(
        uploadDiagnosticImage(image, 'diag-fail-1', undefined, {
          authInstance: mockAuth,
          storageInstance: mockStorage,
          refFn,
          uploadBytesResumableFn,
          deleteObjectFn,
        })
      ).rejects.toThrowError(StoragePermissionError);

      // Verify rollback only deleted the files uploaded in THIS call
      expect(deletedPaths).toEqual([
        'users/farmer-1/diagnostics/diag-fail-1/original.jpg',
        'users/farmer-1/diagnostics/diag-fail-1/diagnostic.webp',
      ]);
    });
  });

  describe('uploadListingStagingImage', () => {
    it('uploads display derivative only when includeOriginal is omitted/false', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const uploadedRefs: string[] = [];

      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const uploadBytesResumableFn = vi.fn((ref: StorageReference) => {
        uploadedRefs.push(ref.fullPath);
        const { uploadTask, triggerSuccess } = createMockUploadTask();
        setTimeout(() => triggerSuccess(), 0);
        return uploadTask;
      });

      const result = await uploadListingStagingImage(image, 'draft-42', 'img-001', undefined, {
        authInstance: mockAuth,
        storageInstance: mockStorage,
        refFn,
        uploadBytesResumableFn,
      });

      expect(result.draftId).toBe('draft-42');
      expect(result.imageId).toBe('img-001');
      expect(result.display.storagePath).toBe('users/farmer-1/staging/listings/draft-42/img-001/display.webp');
      expect(result.original).toBeUndefined();
      expect(uploadedRefs).toEqual(['users/farmer-1/staging/listings/draft-42/img-001/display.webp']);
    });

    it('uploads both display and original when includeOriginal is true', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const uploadedRefs: string[] = [];

      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const uploadBytesResumableFn = vi.fn((ref: StorageReference) => {
        uploadedRefs.push(ref.fullPath);
        const { uploadTask, triggerSuccess } = createMockUploadTask();
        setTimeout(() => triggerSuccess(), 0);
        return uploadTask;
      });

      const result = await uploadListingStagingImage(
        image,
        'draft-42',
        'img-001',
        { includeOriginal: true },
        { authInstance: mockAuth, storageInstance: mockStorage, refFn, uploadBytesResumableFn }
      );

      expect(result.display.storagePath).toBe('users/farmer-1/staging/listings/draft-42/img-001/display.webp');
      expect(result.original?.storagePath).toBe('users/farmer-1/staging/listings/draft-42/img-001/original.jpg');
      expect(uploadedRefs).toEqual([
        'users/farmer-1/staging/listings/draft-42/img-001/display.webp',
        'users/farmer-1/staging/listings/draft-42/img-001/original.jpg',
      ]);
    });
  });

  describe('uploadDisputeEvidenceImage', () => {
    it('uploads original and display derivative to users/{uid}/disputes/{disputeId}/{evidenceId}/', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'buyer-99' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const uploadedRefs: string[] = [];

      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const uploadBytesResumableFn = vi.fn((ref: StorageReference) => {
        uploadedRefs.push(ref.fullPath);
        const { uploadTask, triggerSuccess } = createMockUploadTask();
        setTimeout(() => triggerSuccess(), 0);
        return uploadTask;
      });

      const result = await uploadDisputeEvidenceImage(image, 'disp-7', 'ev-1', undefined, {
        authInstance: mockAuth,
        storageInstance: mockStorage,
        refFn,
        uploadBytesResumableFn,
      });

      expect(result.disputeId).toBe('disp-7');
      expect(result.evidenceId).toBe('ev-1');
      expect(result.original.storagePath).toBe('users/buyer-99/disputes/disp-7/ev-1/original.jpg');
      expect(result.display.storagePath).toBe('users/buyer-99/disputes/disp-7/ev-1/display.webp');
      expect(uploadedRefs).toEqual([
        'users/buyer-99/disputes/disp-7/ev-1/original.jpg',
        'users/buyer-99/disputes/disp-7/ev-1/display.webp',
      ]);
    });

    it('does NOT call deleteObject on dispute failure, preserving rules immutability', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'buyer-99' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const deleteObjectFn = vi.fn();
      let callCount = 0;
      const uploadBytesResumableFn = vi.fn(() => {
        callCount++;
        const { uploadTask, triggerSuccess, triggerError } = createMockUploadTask();
        if (callCount === 1) {
          // original succeeds
          setTimeout(() => triggerSuccess(), 0);
        } else {
          // display fails
          setTimeout(() => triggerError('storage/retry-limit-exceeded', 'Network timeout'), 0);
        }
        return uploadTask;
      });

      await expect(
        uploadDisputeEvidenceImage(image, 'disp-7', 'ev-1', undefined, {
          authInstance: mockAuth,
          storageInstance: mockStorage,
          refFn,
          uploadBytesResumableFn,
          deleteObjectFn,
        })
      ).rejects.toThrowError(/Network timeout/);

      // Crucial: Client delete MUST NOT be called for dispute paths
      expect(deleteObjectFn).not.toHaveBeenCalled();
    });
  });

  describe('AbortSignal Race Conditions', () => {
    it('Window 1: rejects immediately if signal was already aborted before calling upload', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const uploadBytesResumableFn = vi.fn();
      const controller = new AbortController();
      controller.abort(); // pre-aborted

      await expect(
        uploadDiagnosticImage(image, 'diag-abort-1', { signal: controller.signal }, {
          authInstance: mockAuth,
          uploadBytesResumableFn,
        })
      ).rejects.toThrowError(StorageUploadAbortedError);

      expect(uploadBytesResumableFn).not.toHaveBeenCalled();
    });

    it('Window 2: cancels task when signal aborts immediately during task initialization', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const controller = new AbortController();
      let wasCancelled = false;

      const uploadBytesResumableFn = vi.fn(() => {
        // Abort right when task is created
        controller.abort();
        const { uploadTask } = createMockUploadTask({
          cancelBehavior: () => {
            wasCancelled = true;
          },
        });
        return uploadTask;
      });

      await expect(
        uploadListingStagingImage(image, 'draft-1', 'img-1', { signal: controller.signal }, {
          authInstance: mockAuth,
          uploadBytesResumableFn,
        })
      ).rejects.toThrowError(StorageUploadAbortedError);

      expect(wasCancelled).toBe(true);
    });

    it('Window 3: cancels running task when signal aborts mid-stream', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const controller = new AbortController();
      let uploadTaskRef: UploadTask;

      const uploadBytesResumableFn = vi.fn(() => {
        const { uploadTask } = createMockUploadTask({ emitProgress: true });
        uploadTaskRef = uploadTask;
        return uploadTask;
      });

      const onProgress = vi.fn(() => {
        // Trigger abort upon first chunk progress
        controller.abort();
      });

      await expect(
        uploadListingStagingImage(image, 'draft-1', 'img-1', { signal: controller.signal, onProgress }, {
          authInstance: mockAuth,
          uploadBytesResumableFn,
        })
      ).rejects.toThrowError(StorageUploadAbortedError);

      expect(uploadTaskRef!.cancel).toHaveBeenCalled();
    });

    it('Window 4: rolls back committed diagnostic objects when signal aborts at completion settlement', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const deletedPaths: string[] = [];
      const deleteObjectFn = vi.fn(async (fileRef: StorageReference) => {
        deletedPaths.push(fileRef.fullPath);
      });

      const controller = new AbortController();
      let uploadStep = 0;

      const uploadBytesResumableFn = vi.fn(() => {
        uploadStep++;
        const { uploadTask, triggerSuccess } = createMockUploadTask();

        if (uploadStep === 1) {
          // First file (original) completes normally
          setTimeout(() => triggerSuccess(), 0);
        } else if (uploadStep === 2) {
          // Second file (diagnostic) commits, but signal is aborted at completion settlement (Window 4)
          setTimeout(() => {
            Object.defineProperty(controller.signal, 'aborted', { value: true, configurable: true });
            triggerSuccess();
          }, 0);
        }
        return uploadTask;
      });

      await expect(
        uploadDiagnosticImage(image, 'diag-race-1', { signal: controller.signal }, {
          authInstance: mockAuth,
          storageInstance: mockStorage,
          refFn,
          uploadBytesResumableFn,
          deleteObjectFn,
        })
      ).rejects.toThrowError(StorageUploadAbortedError);

      // Crucial: Rollback MUST include both the first normal file AND the second file that committed in Window 4
      expect(deletedPaths).toEqual([
        'users/farmer-1/diagnostics/diag-race-1/original.jpg',
        'users/farmer-1/diagnostics/diag-race-1/diagnostic.webp',
      ]);
      expect(deleteObjectFn).toHaveBeenCalledTimes(2);
    });

    it('Window 4: rolls back committed listing staging objects when signal aborts at completion settlement', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const deletedPaths: string[] = [];
      const deleteObjectFn = vi.fn(async (fileRef: StorageReference) => {
        deletedPaths.push(fileRef.fullPath);
      });

      const controller = new AbortController();

      const uploadBytesResumableFn = vi.fn(() => {
        const { uploadTask, triggerSuccess } = createMockUploadTask();
        // Display derivative completes, but signal is aborted at completion settlement (Window 4)
        setTimeout(() => {
          Object.defineProperty(controller.signal, 'aborted', { value: true, configurable: true });
          triggerSuccess();
        }, 0);
        return uploadTask;
      });

      await expect(
        uploadListingStagingImage(image, 'draft-race-1', 'img-001', { signal: controller.signal }, {
          authInstance: mockAuth,
          storageInstance: mockStorage,
          refFn,
          uploadBytesResumableFn,
          deleteObjectFn,
        })
      ).rejects.toThrowError(StorageUploadAbortedError);

      // Committed display file must be registered and rolled back
      expect(deletedPaths).toEqual([
        'users/farmer-1/staging/listings/draft-race-1/img-001/display.webp',
      ]);
      expect(deleteObjectFn).toHaveBeenCalledTimes(1);
    });

    it('Window 4: rejects with StorageUploadAbortedError without calling deleteObject for dispute evidence', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'buyer-99' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const deleteObjectFn = vi.fn();
      const controller = new AbortController();

      const uploadBytesResumableFn = vi.fn(() => {
        const { uploadTask, triggerSuccess } = createMockUploadTask();
        // Original completes, but signal is aborted at completion settlement (Window 4)
        setTimeout(() => {
          Object.defineProperty(controller.signal, 'aborted', { value: true, configurable: true });
          triggerSuccess();
        }, 0);
        return uploadTask;
      });

      await expect(
        uploadDisputeEvidenceImage(image, 'disp-race-1', 'ev-001', { signal: controller.signal }, {
          authInstance: mockAuth,
          storageInstance: mockStorage,
          refFn,
          uploadBytesResumableFn,
          deleteObjectFn,
        })
      ).rejects.toThrowError(StorageUploadAbortedError);

      // Prohibited: Client delete MUST NEVER be called on dispute evidence paths
      expect(deleteObjectFn).not.toHaveBeenCalled();
    });

    it('deduplication: normal successful uploads do not duplicate registered paths on rollback', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const deletedPaths: string[] = [];
      const deleteObjectFn = vi.fn(async (fileRef: StorageReference) => {
        deletedPaths.push(fileRef.fullPath);
      });

      let callCount = 0;
      const uploadBytesResumableFn = vi.fn(() => {
        callCount++;
        const { uploadTask, triggerSuccess, triggerError } = createMockUploadTask();
        if (callCount <= 2) {
          // original and diagnostic succeed
          setTimeout(() => triggerSuccess(), 0);
        } else {
          // display derivative fails
          setTimeout(() => triggerError('storage/unknown', 'Network error'), 0);
        }
        return uploadTask;
      });

      await expect(
        uploadDiagnosticImage(image, 'diag-dedup-1', undefined, {
          authInstance: mockAuth,
          storageInstance: mockStorage,
          refFn,
          uploadBytesResumableFn,
          deleteObjectFn,
        })
      ).rejects.toThrow();

      // Exactly two unique files rolled back, no duplicate path entries
      expect(deletedPaths).toEqual([
        'users/farmer-1/diagnostics/diag-dedup-1/original.jpg',
        'users/farmer-1/diagnostics/diag-dedup-1/diagnostic.webp',
      ]);
      expect(deleteObjectFn).toHaveBeenCalledTimes(2);
    });
  });

  describe('Error Classification & Security Boundaries', () => {
    it('wraps storage/unauthorized as StoragePermissionError preserving code and targetPath', async () => {
      const image = createProcessedImageFixture();
      const mockAuth = {
        authStateReady: vi.fn().mockResolvedValue(undefined),
        currentUser: { uid: 'farmer-1' } as User,
      } as unknown as Auth;

      const mockStorage = {} as FirebaseStorage;
      const refFn = vi.fn((_storage: FirebaseStorage, path?: string) => ({
        fullPath: path || '',
        name: path?.split('/').pop() || '',
      } as StorageReference));

      const uploadBytesResumableFn = vi.fn(() => {
        const { uploadTask, triggerError } = createMockUploadTask();
        setTimeout(() => triggerError('storage/unauthorized', 'Unauthorized write'), 0);
        return uploadTask;
      });

      try {
        await uploadDiagnosticImage(image, 'diag-perm-1', undefined, {
          authInstance: mockAuth,
          storageInstance: mockStorage,
          refFn,
          uploadBytesResumableFn,
        });
        expect.unreachable('Should have thrown StoragePermissionError');
      } catch (err: unknown) {
        expect(err).toBeInstanceOf(StoragePermissionError);
        const permErr = err as StoragePermissionError;
        expect(permErr.code).toBe('storage/unauthorized');
        expect(permErr.targetPath).toBe('users/farmer-1/diagnostics/diag-perm-1/original.jpg');
        expect(permErr.message).toContain('Storage access denied');
      }
    });
  });
});

