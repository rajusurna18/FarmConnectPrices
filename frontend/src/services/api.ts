import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { env } from '../config/env';
import { getCurrentIdToken, logoutUser } from './firebase/auth';
import { auth } from '../config/firebase';

declare module 'axios' {
  export interface AxiosRequestConfig {
    expectedUid?: string;
  }
  export interface InternalAxiosRequestConfig {
    expectedUid?: string;
  }
}

export interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  expectedUid?: string;
}

/**
 * Checks whether an error code from Firebase Auth indicates a permanent,
 * terminal authentication failure requiring session termination.
 */
export function isTerminalAuthError(errorCode?: string): boolean {
  if (!errorCode) return false;
  return (
    errorCode === 'auth/user-token-expired' ||
    errorCode === 'auth/user-disabled' ||
    errorCode === 'auth/invalid-user-token'
  );
}

// Module-level single-flight coordination state
let refreshPromise: Promise<string> | null = null;
let refreshPromiseUid: string | undefined = undefined;
let isLoggingOut = false;

/**
 * Reset module-level locks for testing purposes.
 */
export function _resetAuthStateForTesting(): void {
  refreshPromise = null;
  refreshPromiseUid = undefined;
  isLoggingOut = false;
}

/**
 * Safe terminal session cleanup.
 * Prevents duplicate logout calls and redirect loops.
 */
export async function handleTerminalLogout(): Promise<void> {
  if (isLoggingOut) return;
  isLoggingOut = true;
  try {
    await logoutUser();
  } catch (err) {
    console.warn('[apiClient] Error during session logout:', err);
  } finally {
    if (typeof window !== 'undefined' && window.location) {
      const currentPath = window.location.pathname || '';
      if (!currentPath.startsWith('/login') && !currentPath.startsWith('/register')) {
        window.location.assign('/login');
      }
    }
    setTimeout(() => {
      isLoggingOut = false;
    }, 1000);
  }
}

/**
 * Single-flight token refresh coordinator.
 * Ensures concurrent 401s coalesce into one getCurrentIdToken(true) call.
 * When expectedUid is supplied, ensures the refresh only resolves tokens
 * belonging to that expected UID and never returns another user's token.
 */
export async function executeSingleFlightTokenRefresh(expectedUid?: string): Promise<string> {
  // If an expected UID is provided, immediately verify active user matches
  if (expectedUid && (!auth.currentUser || auth.currentUser.uid !== expectedUid)) {
    throw new Error(`Authentication session mismatch: active user does not match expected UID (${expectedUid}).`);
  }

  // Coalesce only when refresh targets match exactly:
  // - Owner-bound request matches only if refreshPromiseUid === expectedUid
  // - Unbound request matches only if refreshPromiseUid === undefined
  if (refreshPromise && refreshPromiseUid === expectedUid) {
    return refreshPromise;
  }

  const targetUid = expectedUid;
  refreshPromiseUid = targetUid;

  const thisPromise = (async () => {
    // 1. Ensure Firebase Auth initialization API is available
    if (!auth || typeof auth.authStateReady !== 'function') {
      throw new Error('Firebase Auth initialization failure: authStateReady API is unavailable.');
    }

    // 2. Wait for initial Firebase Auth restoration.
    // If authStateReady() rejects, do NOT catch and swallow; propagate error without logging out.
    await auth.authStateReady();

    // 3. Terminal condition: confirmed absence of user ONLY after successful auth initialization
    if (!auth.currentUser) {
      if (!targetUid) {
        await handleTerminalLogout();
      }
      throw new Error('Authentication session ended: no active user.');
    }

    if (targetUid && auth.currentUser.uid !== targetUid) {
      throw new Error('Authentication session ended: active user mismatch during token refresh.');
    }

    // 4. Request forced token refresh tied to targetUid
    try {
      const freshToken = targetUid
        ? await getCurrentIdToken(true, targetUid)
        : await getCurrentIdToken(true);
      if (!freshToken) {
        // Null token alone when user exists is non-terminal: do NOT log out
        throw new Error('Failed to acquire refreshed token: token is null.');
      }

      // Post-acquisition verification
      if (targetUid && (!auth.currentUser || auth.currentUser.uid !== targetUid)) {
        throw new Error('Authentication session ended: user identity shifted during token refresh.');
      }

      return freshToken;
    } catch (refreshErr: unknown) {
      const code = (refreshErr as { code?: string })?.code;
      if (isTerminalAuthError(code) && !targetUid) {
        await handleTerminalLogout();
      }
      // For non-terminal errors (network, rate-limit, null token, etc.), rethrow without logout
      throw refreshErr;
    }
  })();

  refreshPromise = thisPromise;

  try {
    return await thisPromise;
  } finally {
    // Identity-safe cleanup: only clear if this exact promise is still the active singleton
    if (refreshPromise === thisPromise) {
      refreshPromise = null;
      refreshPromiseUid = undefined;
    }
  }
}

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

/**
 * Safely extracts an abort reason message from an AbortSignal/GenericAbortSignal without using `any`.
 */
function getAbortReasonMessage(signal?: unknown): string | undefined {
  if (!signal || typeof signal !== 'object') {
    return undefined;
  }

  if (!('reason' in signal)) {
    return undefined;
  }

  const reason = (signal as { reason: unknown }).reason;

  if (typeof reason === 'string') {
    return reason;
  }

  if (reason && typeof reason === 'object' && 'message' in reason) {
    const message = (reason as { message: unknown }).message;
    if (typeof message === 'string') {
      return message;
    }
  }

  return undefined;
}

apiClient.interceptors.request.use(
  async (config) => {
    // 1. Check if request signal was already aborted before dispatch
    if (config.signal?.aborted) {
      throw new axios.CanceledError(getAbortReasonMessage(config.signal) || 'Request was aborted before dispatch');
    }

    const expectedUid = config.expectedUid;

    // 2 & 3. Owner-bound request pre-validation against live Firebase Auth
    // Runs on EVERY request before any bypass or token logic, including retries
    if (expectedUid) {
      if (!auth.currentUser || auth.currentUser.uid !== expectedUid) {
        throw new axios.CanceledError(`Request cancelled: active user does not match expected owner UID (${expectedUid}).`);
      }
    }

    // 4. Only after signal abort check and owner validation may a retried request with a fresh token proceed
    const retriable = config as RetriableRequestConfig;
    if (retriable._retry && config.headers?.Authorization) {
      return config;
    }

    // 5. Otherwise acquire the identity-bound token
    try {
      let token: string | null = null;
      if (refreshPromise && refreshPromiseUid === expectedUid) {
        token = await refreshPromise;
      } else {
        token = expectedUid
          ? await getCurrentIdToken(false, expectedUid)
          : await getCurrentIdToken(false);
      }

      // 6. Post-token acquisition identity verification for owner-bound requests
      if (expectedUid) {
        if (!token || !auth.currentUser || auth.currentUser.uid !== expectedUid) {
          throw new axios.CanceledError('Request cancelled: authentication identity shifted during token acquisition.');
        }
      }

      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {
      if (axios.isCancel(error)) {
        throw error;
      }
      console.warn('[apiClient] Failed to attach Firebase ID token:', error);
      if (expectedUid) {
        throw new axios.CanceledError(`Request cancelled: failed to acquire token for expected owner UID (${expectedUid}).`);
      }
    }

    // 7. Final check for cancellation before network dispatch
    if (config.signal?.aborted) {
      throw new axios.CanceledError(getAbortReasonMessage(config.signal) || 'Request was aborted before dispatch');
    }

    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined;

    // Only attempt recovery on HTTP 401 when config exists and has not been retried yet
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;

      const expectedUid = originalRequest.expectedUid;

      // For owner-bound requests: strictly verify identity before attempting refresh or replay
      if (expectedUid) {
        if (!auth.currentUser || auth.currentUser.uid !== expectedUid) {
          return Promise.reject(
            new axios.CanceledError(
              `HTTP 401 retry aborted: current user (${auth.currentUser?.uid}) does not match original request owner (${expectedUid}).`
            )
          );
        }
      }

      // Abort if request signal was aborted
      if (originalRequest.signal?.aborted) {
        return Promise.reject(
          new axios.CanceledError(getAbortReasonMessage(originalRequest.signal) || 'Request aborted prior to 401 replay')
        );
      }

      try {
        const freshToken = await executeSingleFlightTokenRefresh(expectedUid);

        // Re-verify identity after token refresh
        if (expectedUid) {
          if (!auth.currentUser || auth.currentUser.uid !== expectedUid) {
            return Promise.reject(
              new axios.CanceledError(
                'HTTP 401 retry aborted: authenticated user changed during token refresh.'
              )
            );
          }
        }

        if (freshToken) {
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers.Authorization = `Bearer ${freshToken}`;

          // Preserve expectedUid on retried request config
          if (expectedUid) {
            originalRequest.expectedUid = expectedUid;
          }

          return apiClient(originalRequest);
        }
      } catch (refreshError: unknown) {
        // Token refresh failed (terminal or non-terminal).
        // Propagate the meaningful refresh failure to the calling code without duplicate retries,
        // preserving the original Axios response context.
        if (refreshError instanceof Error) {
          const enhancedError = Object.assign(refreshError, {
            response: error.response,
            originalAxiosError: error,
          });
          return Promise.reject(enhancedError);
        }
        return Promise.reject(refreshError ?? error);
      }
    }

    // For:
    // - 401 on an already-retried request (do NOT logout, do NOT retry)
    // - 403 Forbidden (role permission error, do NOT logout)
    // - 503 Service Unavailable (quota circuit breaker, do NOT retry)
    // - 5xx server errors
    // - Network errors / timeouts
    // Propagate error directly to caller
    return Promise.reject(error);
  }
);
