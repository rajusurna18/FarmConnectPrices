/**
 * FarmLink AI — Computer Vision & Firebase Storage Upload Service
 * Chapter 25, Part 5, Stage 3.2B
 *
 * Implements secure, resumable, abortable image uploads adhering to the
 * Stage 3.2A Storage Security Rules and domain access policies.
 */

import {
  ref as defaultRef,
  uploadBytesResumable as defaultUploadBytesResumable,
  deleteObject as defaultDeleteObject,
} from 'firebase/storage';
import type { FirebaseStorage, StorageReference, UploadTaskSnapshot } from 'firebase/storage';
import type { Auth } from 'firebase/auth';
import { storage as defaultStorage, auth as defaultAuth } from '../../../config/firebase';
import type { ProcessedImage } from '../types';

// ============================================================================
// ERROR HIERARCHY
// ============================================================================

export class StorageServiceError extends Error {
  readonly code?: string;

  constructor(message: string, code?: string) {
    super(message);
    this.name = 'StorageServiceError';
    this.code = code;
  }
}

export class StorageAuthenticationError extends StorageServiceError {
  constructor(message: string, code: string = 'auth/unauthenticated') {
    super(message, code);
    this.name = 'StorageAuthenticationError';
  }
}

export class StorageValidationError extends StorageServiceError {
  constructor(message: string, code: string = 'storage/invalid-argument') {
    super(message, code);
    this.name = 'StorageValidationError';
  }
}

export class StorageUploadAbortedError extends StorageServiceError {
  constructor(message: string = 'Image upload was canceled by caller.', code: string = 'storage/canceled') {
    super(message, code);
    this.name = 'StorageUploadAbortedError';
  }
}

export class StoragePermissionError extends StorageServiceError {
  readonly targetPath: string;

  constructor(
    message: string,
    targetPath: string,
    code: string = 'storage/unauthorized'
  ) {
    super(message, code);
    this.name = 'StoragePermissionError';
    this.targetPath = targetPath;
  }
}

// ============================================================================
// DOMAIN TYPES & INTERFACES
// ============================================================================

export interface UploadProgress {
  bytesTransferred: number;
  totalBytes: number;
  percent: number; // 0 to 100
  fileName: string;
}

export interface UploadedFileReference {
  storagePath: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  /**
   * Informational client-side timestamp (ISO 8601 string).
   * Not an authoritative server timestamp; must not be used for legal proof or chronology.
   */
  clientUploadedAt: string;
}

export interface DiagnosticUploadResult {
  diagnosticId: string;
  original: UploadedFileReference;
  diagnostic: UploadedFileReference;
  display: UploadedFileReference;
}

export interface ListingUploadResult {
  draftId: string;
  imageId: string;
  display: UploadedFileReference;
  original?: UploadedFileReference;
}

export interface DisputeUploadResult {
  disputeId: string;
  evidenceId: string;
  original: UploadedFileReference;
  display: UploadedFileReference;
}

export interface UploadOptions {
  onProgress?: (progress: UploadProgress) => void;
  signal?: AbortSignal;
}

export interface ListingUploadOptions extends UploadOptions {
  /**
   * Whether to upload the uncompressed original image alongside the display derivative.
   * Defaults to false to optimize bandwidth and storage for marketplace listings.
   */
  includeOriginal?: boolean;
}

export interface StorageUploadServiceDependencies {
  storageInstance?: FirebaseStorage;
  authInstance?: Auth;
  uploadBytesResumableFn?: typeof defaultUploadBytesResumable;
  deleteObjectFn?: typeof defaultDeleteObject;
  refFn?: typeof defaultRef;
}

// ============================================================================
// VALIDATION & HELPER UTILITIES
// ============================================================================

const ENTITY_ID_REGEX = /^[a-zA-Z0-9_-]{1,64}$/;

/**
 * Validates entity identifiers (diagnosticId, draftId, imageId, disputeId, evidenceId)
 * to prevent directory traversal or malformed Storage paths.
 */
export function validateEntityId(id: string, fieldName: string): void {
  const trimmed = id ? id.trim() : '';
  if (!trimmed || !ENTITY_ID_REGEX.test(trimmed)) {
    throw new StorageValidationError(
      `Invalid ${fieldName}: "${id}". Identifiers must be 1-64 alphanumeric characters, underscores, or hyphens.`
    );
  }
}

/**
 * Resolves the approved Storage filename based on the image role and MIME type.
 * Enforces strict 1:1 consistency matching the storage.rules allowlist.
 */
export function resolveStorageFileName(
  role: 'original' | 'diagnostic' | 'display',
  mimeType: string
): string {
  const normalizedMime = mimeType ? mimeType.toLowerCase().trim() : '';

  switch (normalizedMime) {
    case 'image/jpeg':
      return `${role}.jpg`;
    case 'image/png':
      if (role !== 'original') {
        throw new StorageValidationError(
          `Derivatives cannot be PNG format; only original files may be PNG (received ${role} with ${mimeType}).`
        );
      }
      return `${role}.png`;
    case 'image/webp':
      return `${role}.webp`;
    default:
      throw new StorageValidationError(
        `Unsupported MIME type for storage upload: "${mimeType}". Allowed formats: image/jpeg, image/png, image/webp.`
      );
  }
}

/**
 * Awaits Firebase Auth initialization and returns the authoritative authenticated UID.
 * Never accepts a caller-supplied UID to prevent path spoofing or cross-user writes.
 */
export async function getAuthoritativeAuthenticatedUid(authInstance: Auth = defaultAuth): Promise<string> {
  if (typeof authInstance.authStateReady === 'function') {
    try {
      await authInstance.authStateReady();
    } catch (err: unknown) {
      throw new StorageAuthenticationError(
        `Firebase Auth initialization failure: ${(err as Error)?.message || 'authStateReady failed to resolve.'}`,
        'auth/initialization-failed'
      );
    }
  }

  const user = authInstance.currentUser;
  if (!user || !user.uid) {
    throw new StorageAuthenticationError(
      'Upload failed: User must be authenticated to upload files to Firebase Storage.',
      'auth/unauthenticated'
    );
  }

  return user.uid;
}

// ============================================================================
// CORE ATOMIC UPLOAD TASK RUNNER
// ============================================================================

/**
 * Executes a single resumable upload task with full AbortSignal lifecycle support
 * across all race windows: pre-start, post-task-creation, mid-stream, and near-completion.
 */
async function executeUploadTask(
  storageRef: StorageReference,
  blobOrFile: Blob | File,
  contentType: string,
  onProgress?: (progress: UploadProgress) => void,
  signal?: AbortSignal,
  onStored?: (storagePath: string) => void,
  uploadBytesResumableFn: typeof defaultUploadBytesResumable = defaultUploadBytesResumable
): Promise<UploadedFileReference> {
  // Window 1: Check if signal was aborted prior to initiating the task
  if (signal?.aborted) {
    throw new StorageUploadAbortedError('Image upload was canceled prior to start.');
  }

  const uploadTask = uploadBytesResumableFn(storageRef, blobOrFile, { contentType });

  return new Promise<UploadedFileReference>((resolve, reject) => {
    let isSettled = false;

    const cleanup = () => {
      isSettled = true;
      if (signal) {
        signal.removeEventListener('abort', onAbort);
      }
    };

    const onAbort = () => {
      if (!isSettled) {
        cleanup();
        try {
          uploadTask.cancel();
        } catch (cancelErr) {
          console.warn('[Storage] Warning during task cancellation:', cancelErr);
        }
        reject(new StorageUploadAbortedError('Image upload was canceled by caller.'));
      }
    };

    // Attach abort listener
    if (signal) {
      signal.addEventListener('abort', onAbort, { once: true });
      // Window 2: Check immediately after listener registration to eliminate registration microtask race
      if (signal.aborted) {
        onAbort();
        return;
      }
    }

    uploadTask.on(
      'state_changed',
      (snapshot: UploadTaskSnapshot) => {
        // Window 3: Check during progress callback
        if (signal?.aborted) {
          onAbort();
          return;
        }
        if (onProgress && snapshot.totalBytes > 0) {
          onProgress({
            bytesTransferred: snapshot.bytesTransferred,
            totalBytes: snapshot.totalBytes,
            percent: Math.round((snapshot.bytesTransferred / snapshot.totalBytes) * 100),
            fileName: storageRef.name,
          });
        }
      },
      (error: { code?: string; message?: string }) => {
        if (isSettled) return;
        cleanup();

        if (error.code === 'storage/canceled' || signal?.aborted) {
          reject(new StorageUploadAbortedError('Image upload was canceled.'));
        } else if (error.code === 'storage/unauthorized') {
          reject(
            new StoragePermissionError(
              `Storage access denied for "${storageRef.fullPath}". Verify that you are authenticated as the path owner, ` +
              `the file satisfies size and MIME rules, and that an existing file is not being overwritten.`,
              storageRef.fullPath,
              error.code
            )
          );
        } else {
          reject(new StorageServiceError(`Upload failed: ${error.message || 'Unknown storage error'}`, error.code));
        }
      },
      () => {
        if (isSettled) return;
        cleanup();

        // Register successfully committed object with rollback tracker BEFORE Window 4 check
        onStored?.(storageRef.fullPath);

        // Window 4: Completion race check — NEVER resolve as success if signal has aborted
        if (signal?.aborted) {
          reject(new StorageUploadAbortedError('Image upload was canceled prior to completion settlement.'));
          return;
        }

        resolve({
          storagePath: storageRef.fullPath,
          fileName: storageRef.name,
          mimeType: contentType,
          sizeBytes: blobOrFile.size,
          clientUploadedAt: new Date().toISOString(),
        });
      }
    );
  });
}

// ============================================================================
// DOMAIN UPLOAD SERVICES
// ============================================================================

/**
 * Safe rollback helper limited strictly to paths uploaded during the current invocation.
 * Never deletes dispute evidence paths or files from previous upload attempts.
 */
async function rollbackCurrentInvocationFiles(
  paths: string[],
  storageInstance: FirebaseStorage,
  deleteObjectFn: typeof defaultDeleteObject = defaultDeleteObject,
  refFn: typeof defaultRef = defaultRef
): Promise<void> {
  for (const path of paths) {
    try {
      const fileRef = refFn(storageInstance, path);
      await deleteObjectFn(fileRef);
    } catch (cleanupErr) {
      console.warn(`[Storage] Rollback failed to delete partial upload at "${path}":`, cleanupErr);
    }
  }
}

/**
 * Uploads a ProcessedImage to private diagnostic storage.
 * Path: users/{userId}/diagnostics/{diagnosticId}/{fileName}
 * Required objects: original image (raw File), diagnostic derivative (Blob), display derivative (Blob).
 */
export async function uploadDiagnosticImage(
  processedImage: ProcessedImage,
  diagnosticId: string,
  options?: UploadOptions,
  deps?: StorageUploadServiceDependencies
): Promise<DiagnosticUploadResult> {
  validateEntityId(diagnosticId, 'diagnosticId');

  if (!processedImage.diagnosticSource) {
    throw new StorageValidationError(
      'Diagnostic upload requires a diagnosticSource derivative, but none was provided on the ProcessedImage.'
    );
  }

  const authInstance = deps?.authInstance || defaultAuth;
  const storageInstance = deps?.storageInstance || defaultStorage;
  const uploadBytesResumableFn = deps?.uploadBytesResumableFn || defaultUploadBytesResumable;
  const deleteObjectFn = deps?.deleteObjectFn || defaultDeleteObject;
  const refFn = deps?.refFn || defaultRef;

  const userId = await getAuthoritativeAuthenticatedUid(authInstance);
  const basePath = `users/${userId}/diagnostics/${diagnosticId}`;
  const completedPathsInThisInvocation: string[] = [];
  const registerCommittedPath = (path: string) => {
    if (!completedPathsInThisInvocation.includes(path)) {
      completedPathsInThisInvocation.push(path);
    }
  };

  try {
    // 1. Upload original uncompressed File directly (preserves raw integrity)
    const originalFile = processedImage.originalFile;
    const originalFileName = resolveStorageFileName('original', originalFile.type);
    const originalRef = refFn(storageInstance, `${basePath}/${originalFileName}`);

    const uploadedOriginal = await executeUploadTask(
      originalRef,
      originalFile,
      originalFile.type,
      options?.onProgress,
      options?.signal,
      registerCommittedPath,
      uploadBytesResumableFn
    );

    // 2. Upload diagnostic derivative
    const diagDerivative = processedImage.diagnosticSource;
    const diagFileName = resolveStorageFileName('diagnostic', diagDerivative.mimeType);
    const diagRef = refFn(storageInstance, `${basePath}/${diagFileName}`);

    const uploadedDiag = await executeUploadTask(
      diagRef,
      diagDerivative.blob,
      diagDerivative.mimeType,
      options?.onProgress,
      options?.signal,
      registerCommittedPath,
      uploadBytesResumableFn
    );

    // 3. Upload display derivative
    const displayDerivative = processedImage.displayDerivative;
    const displayFileName = resolveStorageFileName('display', displayDerivative.mimeType);
    const displayRef = refFn(storageInstance, `${basePath}/${displayFileName}`);

    const uploadedDisplay = await executeUploadTask(
      displayRef,
      displayDerivative.blob,
      displayDerivative.mimeType,
      options?.onProgress,
      options?.signal,
      registerCommittedPath,
      uploadBytesResumableFn
    );

    return {
      diagnosticId,
      original: uploadedOriginal,
      diagnostic: uploadedDiag,
      display: uploadedDisplay,
    };
  } catch (error) {
    // Diagnostic domain allows owner deletion of partial files from this invocation
    await rollbackCurrentInvocationFiles(completedPathsInThisInvocation, storageInstance, deleteObjectFn, refFn);
    throw error;
  }
}

/**
 * Uploads a ProcessedImage to private listing staging storage.
 * Path: users/{userId}/staging/listings/{draftId}/{imageId}/{fileName}
 * Objects: display derivative (required) + original image (optional, default false).
 */
export async function uploadListingStagingImage(
  processedImage: ProcessedImage,
  draftId: string,
  imageId: string,
  options?: ListingUploadOptions,
  deps?: StorageUploadServiceDependencies
): Promise<ListingUploadResult> {
  validateEntityId(draftId, 'draftId');
  validateEntityId(imageId, 'imageId');

  const authInstance = deps?.authInstance || defaultAuth;
  const storageInstance = deps?.storageInstance || defaultStorage;
  const uploadBytesResumableFn = deps?.uploadBytesResumableFn || defaultUploadBytesResumable;
  const deleteObjectFn = deps?.deleteObjectFn || defaultDeleteObject;
  const refFn = deps?.refFn || defaultRef;

  const userId = await getAuthoritativeAuthenticatedUid(authInstance);
  const basePath = `users/${userId}/staging/listings/${draftId}/${imageId}`;
  const completedPathsInThisInvocation: string[] = [];
  const registerCommittedPath = (path: string) => {
    if (!completedPathsInThisInvocation.includes(path)) {
      completedPathsInThisInvocation.push(path);
    }
  };

  try {
    // 1. Upload display derivative (required for listing display)
    const displayDerivative = processedImage.displayDerivative;
    const displayFileName = resolveStorageFileName('display', displayDerivative.mimeType);
    const displayRef = refFn(storageInstance, `${basePath}/${displayFileName}`);

    const uploadedDisplay = await executeUploadTask(
      displayRef,
      displayDerivative.blob,
      displayDerivative.mimeType,
      options?.onProgress,
      options?.signal,
      registerCommittedPath,
      uploadBytesResumableFn
    );

    // 2. Upload original file optionally if requested
    let uploadedOriginal: UploadedFileReference | undefined;
    if (options?.includeOriginal) {
      const originalFile = processedImage.originalFile;
      const originalFileName = resolveStorageFileName('original', originalFile.type);
      const originalRef = refFn(storageInstance, `${basePath}/${originalFileName}`);

      uploadedOriginal = await executeUploadTask(
        originalRef,
        originalFile,
        originalFile.type,
        options?.onProgress,
        options?.signal,
        registerCommittedPath,
        uploadBytesResumableFn
      );
    }

    return {
      draftId,
      imageId,
      display: uploadedDisplay,
      original: uploadedOriginal,
    };
  } catch (error) {
    // Listing staging domain allows owner deletion of partial files from this invocation
    await rollbackCurrentInvocationFiles(completedPathsInThisInvocation, storageInstance, deleteObjectFn, refFn);
    throw error;
  }
}

/**
 * Uploads a ProcessedImage to private dispute evidence storage.
 * Path: users/{userId}/disputes/{disputeId}/{evidenceId}/{fileName}
 * Required objects: original image (raw File) + display derivative (Blob).
 *
 * NOTE: Storage Rules enforce 'allow delete: if false' on dispute evidence paths.
 * Client-side deletion is strictly prohibited. If an upload fails, this function
 * propagates the root error without attempting deleteObject().
 */
export async function uploadDisputeEvidenceImage(
  processedImage: ProcessedImage,
  disputeId: string,
  evidenceId: string,
  options?: UploadOptions,
  deps?: StorageUploadServiceDependencies
): Promise<DisputeUploadResult> {
  validateEntityId(disputeId, 'disputeId');
  validateEntityId(evidenceId, 'evidenceId');

  const authInstance = deps?.authInstance || defaultAuth;
  const storageInstance = deps?.storageInstance || defaultStorage;
  const uploadBytesResumableFn = deps?.uploadBytesResumableFn || defaultUploadBytesResumable;
  const refFn = deps?.refFn || defaultRef;

  const userId = await getAuthoritativeAuthenticatedUid(authInstance);
  const basePath = `users/${userId}/disputes/${disputeId}/${evidenceId}`;

  // 1. Upload original uncompressed File directly
  const originalFile = processedImage.originalFile;
  const originalFileName = resolveStorageFileName('original', originalFile.type);
  const originalRef = refFn(storageInstance, `${basePath}/${originalFileName}`);

  const uploadedOriginal = await executeUploadTask(
    originalRef,
    originalFile,
    originalFile.type,
    options?.onProgress,
    options?.signal,
    undefined,
    uploadBytesResumableFn
  );

  // 2. Upload display derivative
  const displayDerivative = processedImage.displayDerivative;
  const displayFileName = resolveStorageFileName('display', displayDerivative.mimeType);
  const displayRef = refFn(storageInstance, `${basePath}/${displayFileName}`);

  const uploadedDisplay = await executeUploadTask(
    displayRef,
    displayDerivative.blob,
    displayDerivative.mimeType,
    options?.onProgress,
    options?.signal,
    undefined,
    uploadBytesResumableFn
  );

  return {
    disputeId,
    evidenceId,
    original: uploadedOriginal,
    display: uploadedDisplay,
  };
}

