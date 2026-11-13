import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { env } from '../config/env';
import { getCurrentIdToken, logoutUser } from './firebase/auth';
import { auth } from '../config/firebase';

export interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
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
let isLoggingOut = false;

/**
 * Reset module-level locks for testing purposes.
 */
export function _resetAuthStateForTesting(): void {
  refreshPromise = null;
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
 */
export async function executeSingleFlightTokenRefresh(): Promise<string> {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = (async () => {
    // 1. Ensure Firebase Auth initialization API is available
    if (!auth || typeof auth.authStateReady !== 'function') {
      throw new Error('Firebase Auth initialization failure: authStateReady API is unavailable.');
    }

    // 2. Wait for initial Firebase Auth restoration.
    // If authStateReady() rejects, do NOT catch and swallow; propagate error without logging out.
    await auth.authStateReady();

    // 3. Terminal condition: confirmed absence of user ONLY after successful auth initialization
    if (!auth.currentUser) {
      await handleTerminalLogout();
      throw new Error('Authentication session ended: no active user.');
    }

    // 4. Request forced token refresh
    try {
      const freshToken = await getCurrentIdToken(true);
      if (!freshToken) {
        // Null token alone when user exists is non-terminal: do NOT log out
        throw new Error('Failed to acquire refreshed token: token is null.');
      }
      return freshToken;
    } catch (refreshErr: unknown) {
      const code = (refreshErr as { code?: string })?.code;
      if (isTerminalAuthError(code)) {
        await handleTerminalLogout();
      }
      // For non-terminal errors (network, rate-limit, null token, etc.), rethrow without logout
      throw refreshErr;
    }
  })();

  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

apiClient.interceptors.request.use(
  async (config) => {
    // If request has already been retried with a fresh token, preserve it
    const retriable = config as RetriableRequestConfig;
    if (retriable._retry && config.headers?.Authorization) {
      return config;
    }

    try {
      const token = await getCurrentIdToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {
      console.warn('[apiClient] Failed to attach Firebase ID token:', error);
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

      try {
        const freshToken = await executeSingleFlightTokenRefresh();
        if (freshToken) {
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers.Authorization = `Bearer ${freshToken}`;
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
