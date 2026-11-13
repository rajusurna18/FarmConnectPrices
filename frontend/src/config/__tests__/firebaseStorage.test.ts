import { describe, it, expect, vi, beforeEach } from 'vitest';
import { storage, app, initializeStorageEmulator, getStorageEmulatorRegistry } from '../firebase';
import type { FirebaseStorage } from 'firebase/storage';

describe('Firebase Storage Configuration & Emulator Registry (firebase.ts)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('exports singleton storage instance bound to the singleton app', () => {
    expect(storage).toBeDefined();
    expect(storage.app).toBe(app);
  });

  describe('Storage Emulator Tracking Registry', () => {
    it('returns a persistent WeakMap registry keyed by FirebaseStorage instances', () => {
      const registry = getStorageEmulatorRegistry();
      expect(registry).toBeInstanceOf(WeakMap);
    });

    it('validates host and port boundaries before connecting', () => {
      const mockStorage = {} as FirebaseStorage;

      expect(() => initializeStorageEmulator(mockStorage, '', 9199)).toThrowError(
        /Firebase Storage Emulator host cannot be blank/
      );
      expect(() => initializeStorageEmulator(mockStorage, '   ', 9199)).toThrowError(
        /Firebase Storage Emulator host cannot be blank/
      );
      expect(() => initializeStorageEmulator(mockStorage, 'localhost', 0)).toThrowError(
        /Invalid Firebase Storage Emulator port: 0/
      );
      expect(() => initializeStorageEmulator(mockStorage, 'localhost', -1)).toThrowError(
        /Invalid Firebase Storage Emulator port: -1/
      );
      expect(() => initializeStorageEmulator(mockStorage, 'localhost', 70000)).toThrowError(
        /Invalid Firebase Storage Emulator port: 70000/
      );
      expect(() => initializeStorageEmulator(mockStorage, 'localhost', NaN)).toThrowError(
        /Invalid Firebase Storage Emulator port: NaN/
      );
    });

    it('connects a new Storage instance and records it in the registry', () => {
      const mockStorage = { _id: 'storage-1' } as unknown as FirebaseStorage;
      const mockConnect = vi.fn();

      initializeStorageEmulator(mockStorage, 'localhost', 9199, mockConnect);

      expect(mockConnect).toHaveBeenCalledTimes(1);
      expect(mockConnect).toHaveBeenCalledWith(mockStorage, 'localhost', 9199);

      const registry = getStorageEmulatorRegistry();
      expect(registry.get(mockStorage)).toEqual({ host: 'localhost', port: 9199 });
    });

    it('safely skips duplicate initialization when the same instance is already registered for the same target', () => {
      const mockStorage = { _id: 'storage-2' } as unknown as FirebaseStorage;
      const mockConnect = vi.fn();

      // First call connects
      initializeStorageEmulator(mockStorage, 'localhost', 9199, mockConnect);
      expect(mockConnect).toHaveBeenCalledTimes(1);

      // Second call with same instance and same target skips duplicate connection
      initializeStorageEmulator(mockStorage, 'localhost', 9199, mockConnect);
      expect(mockConnect).toHaveBeenCalledTimes(1);
    });

    it('rejects a different target for an already-registered Storage instance', () => {
      const mockStorage = { _id: 'storage-3' } as unknown as FirebaseStorage;
      const mockConnect = vi.fn();

      initializeStorageEmulator(mockStorage, 'localhost', 9199, mockConnect);

      // Attempt to reconnect to a different port
      expect(() => initializeStorageEmulator(mockStorage, 'localhost', 9200, mockConnect)).toThrowError(
        /CRITICAL INITIALIZATION CONFLICT: FirebaseStorage instance is already connected to emulator at localhost:9199. Cannot reconnect the same instance to localhost:9200/
      );

      // Attempt to reconnect to a different host
      expect(() => initializeStorageEmulator(mockStorage, '127.0.0.1', 9199, mockConnect)).toThrowError(
        /CRITICAL INITIALIZATION CONFLICT: FirebaseStorage instance is already connected to emulator at localhost:9199. Cannot reconnect the same instance to 127.0.0.1:9199/
      );
    });

    it('does not skip initialization for a new Storage instance merely because another instance was connected', () => {
      const mockStorageA = { _id: 'storage-a' } as unknown as FirebaseStorage;
      const mockStorageB = { _id: 'storage-b' } as unknown as FirebaseStorage;
      const mockConnect = vi.fn();

      initializeStorageEmulator(mockStorageA, 'localhost', 9199, mockConnect);
      expect(mockConnect).toHaveBeenCalledWith(mockStorageA, 'localhost', 9199);

      initializeStorageEmulator(mockStorageB, 'localhost', 9199, mockConnect);
      expect(mockConnect).toHaveBeenCalledWith(mockStorageB, 'localhost', 9199);

      expect(mockConnect).toHaveBeenCalledTimes(2);
    });

    it('propagates genuine unverified initialization errors from connectStorageEmulator', () => {
      const mockStorage = { _id: 'storage-err' } as unknown as FirebaseStorage;
      const mockConnect = vi.fn(() => {
        throw new Error('EADDRINUSE: Port 9199 is already in use by another process');
      });

      expect(() => initializeStorageEmulator(mockStorage, 'localhost', 9199, mockConnect)).toThrowError(
        /EADDRINUSE: Port 9199 is already in use by another process/
      );

      // Mismatched / failed instance is NOT added to registry
      const registry = getStorageEmulatorRegistry();
      expect(registry.has(mockStorage)).toBe(false);
    });
  });
});
