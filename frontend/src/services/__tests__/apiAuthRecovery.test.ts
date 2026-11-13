import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios, { AxiosHeaders, type AxiosResponse } from 'axios';
import {
  apiClient,
  isTerminalAuthError,
  handleTerminalLogout,
  executeSingleFlightTokenRefresh,
  _resetAuthStateForTesting,
} from '../api';
import * as authService from '../firebase/auth';
import { auth } from '../../config/firebase';

interface MockWindow {
  location: {
    pathname: string;
    assign: ReturnType<typeof vi.fn>;
  };
}

interface MockAuth {
  currentUser: { uid: string } | null;
  authStateReady?: ReturnType<typeof vi.fn>;
}

interface AuthErrorWithCode extends Error {
  code?: string;
  response?: AxiosResponse;
  originalAxiosError?: unknown;
}

vi.mock('../firebase/auth', () => ({
  getCurrentIdToken: vi.fn(),
  logoutUser: vi.fn(),
}));

vi.mock('../../config/firebase', () => ({
  auth: {
    currentUser: { uid: 'test-user-123' },
    authStateReady: vi.fn().mockResolvedValue(undefined),
  },
}));

describe('Safe Axios Authentication Recovery Suite', () => {
  let assignMock: ReturnType<typeof vi.fn>;
  const mockGlobal = globalThis as unknown as { window: MockWindow };
  const mockAuth = auth as unknown as MockAuth;

  beforeEach(() => {
    vi.clearAllMocks();
    _resetAuthStateForTesting();

    assignMock = vi.fn();
    mockGlobal.window = {
      location: {
        pathname: '/dashboard',
        assign: assignMock,
      },
    };

    // Default mock behavior
    mockAuth.currentUser = { uid: 'test-user-123' };
    mockAuth.authStateReady = vi.fn().mockResolvedValue(undefined);
    vi.mocked(authService.getCurrentIdToken).mockResolvedValue('initial-mock-token');
    vi.mocked(authService.logoutUser).mockResolvedValue(undefined);
  });

  describe('1. Terminal Auth Error Classification', () => {
    it('classifies auth/user-token-expired as a terminal error', () => {
      expect(isTerminalAuthError('auth/user-token-expired')).toBe(true);
    });

    it('classifies auth/user-disabled as a terminal error', () => {
      expect(isTerminalAuthError('auth/user-disabled')).toBe(true);
    });

    it('classifies auth/invalid-user-token as a terminal error', () => {
      expect(isTerminalAuthError('auth/invalid-user-token')).toBe(true);
    });

    it('classifies auth/network-request-failed as NON-terminal', () => {
      expect(isTerminalAuthError('auth/network-request-failed')).toBe(false);
    });

    it('classifies auth/too-many-requests as NON-terminal', () => {
      expect(isTerminalAuthError('auth/too-many-requests')).toBe(false);
    });

    it('returns false for undefined, empty, or unknown error codes', () => {
      expect(isTerminalAuthError(undefined)).toBe(false);
      expect(isTerminalAuthError('')).toBe(false);
      expect(isTerminalAuthError('auth/other-code')).toBe(false);
    });
  });

  describe('2. Session Cleanup & Safe Logout (handleTerminalLogout)', () => {
    it('calls logoutUser and redirects to /login from an authenticated page', async () => {
      mockGlobal.window.location.pathname = '/farms/123';
      await handleTerminalLogout();

      expect(authService.logoutUser).toHaveBeenCalledTimes(1);
      expect(assignMock).toHaveBeenCalledWith('/login');
    });

    it('does not trigger a redirect loop when already on /login', async () => {
      mockGlobal.window.location.pathname = '/login';
      await handleTerminalLogout();

      expect(authService.logoutUser).toHaveBeenCalledTimes(1);
      expect(assignMock).not.toHaveBeenCalled();
    });

    it('does not trigger a redirect loop when already on /register', async () => {
      mockGlobal.window.location.pathname = '/register';
      await handleTerminalLogout();

      expect(authService.logoutUser).toHaveBeenCalledTimes(1);
      expect(assignMock).not.toHaveBeenCalled();
    });

    it('prevents duplicate concurrent logout executions via mutex lock', async () => {
      mockGlobal.window.location.pathname = '/dashboard';

      // Call handleTerminalLogout concurrently
      await Promise.all([
        handleTerminalLogout(),
        handleTerminalLogout(),
        handleTerminalLogout(),
      ]);

      expect(authService.logoutUser).toHaveBeenCalledTimes(1);
    });
  });

  describe('3. Single-Flight Token Refresh Coordinator & Initialization Safety', () => {
    it('awaits auth.authStateReady before evaluating auth state', async () => {
      let authReadyResolved = false;
      mockAuth.authStateReady = vi.fn().mockImplementationOnce(async () => {
        authReadyResolved = true;
      });
      vi.mocked(authService.getCurrentIdToken).mockResolvedValueOnce('fresh-token-123');

      const token = await executeSingleFlightTokenRefresh();
      expect(authReadyResolved).toBe(true);
      expect(token).toBe('fresh-token-123');
    });

    it('does NOT logout or redirect if auth.authStateReady() rejects', async () => {
      mockAuth.currentUser = null; // Even if currentUser is null, rejection must not trigger logout
      mockAuth.authStateReady = vi.fn().mockRejectedValueOnce(new Error('IndexedDB storage failure'));

      await expect(executeSingleFlightTokenRefresh()).rejects.toThrow('IndexedDB storage failure');
      expect(authService.logoutUser).not.toHaveBeenCalled();
      expect(assignMock).not.toHaveBeenCalled();
    });

    it('does NOT logout if authStateReady API is unavailable, failing as a non-terminal initialization error', async () => {
      mockAuth.currentUser = null;
      mockAuth.authStateReady = undefined;

      await expect(executeSingleFlightTokenRefresh()).rejects.toThrow(
        'Firebase Auth initialization failure: authStateReady API is unavailable.'
      );
      expect(authService.logoutUser).not.toHaveBeenCalled();
      expect(assignMock).not.toHaveBeenCalled();
    });

    it('triggers terminal logout only when currentUser is null after successful initialization', async () => {
      mockAuth.currentUser = null;

      await expect(executeSingleFlightTokenRefresh()).rejects.toThrow(
        'Authentication session ended: no active user.'
      );
      expect(authService.logoutUser).toHaveBeenCalledTimes(1);
      expect(assignMock).toHaveBeenCalledWith('/login');
    });

    it('does NOT trigger logout when currentUser exists but refreshed token is null', async () => {
      mockAuth.currentUser = { uid: 'test-user-123' };
      vi.mocked(authService.getCurrentIdToken).mockResolvedValueOnce(null as unknown as string);

      await expect(executeSingleFlightTokenRefresh()).rejects.toThrow(
        'Failed to acquire refreshed token: token is null.'
      );
      expect(authService.logoutUser).not.toHaveBeenCalled();
      expect(assignMock).not.toHaveBeenCalled();
    });

    it('coalesces multiple concurrent refresh requests into exactly one getCurrentIdToken call', async () => {
      vi.mocked(authService.getCurrentIdToken).mockImplementation(
        () => new Promise((resolve) => setTimeout(() => resolve('coalesced-token-999'), 20))
      );

      const [res1, res2, res3] = await Promise.all([
        executeSingleFlightTokenRefresh(),
        executeSingleFlightTokenRefresh(),
        executeSingleFlightTokenRefresh(),
      ]);

      expect(res1).toBe('coalesced-token-999');
      expect(res2).toBe('coalesced-token-999');
      expect(res3).toBe('coalesced-token-999');
      expect(authService.getCurrentIdToken).toHaveBeenCalledTimes(1);
      expect(authService.getCurrentIdToken).toHaveBeenCalledWith(true);
    });

    it.each([
      'auth/user-token-expired',
      'auth/user-disabled',
      'auth/invalid-user-token',
    ])('triggers terminal logout when refresh throws terminal error (%s)', async (terminalCode) => {
      _resetAuthStateForTesting();
      const terminalError = new Error(`Terminal auth error: ${terminalCode}`) as AuthErrorWithCode;
      terminalError.code = terminalCode;
      vi.mocked(authService.getCurrentIdToken).mockRejectedValueOnce(terminalError);

      await expect(executeSingleFlightTokenRefresh()).rejects.toThrow(`Terminal auth error: ${terminalCode}`);
      expect(authService.logoutUser).toHaveBeenCalledTimes(1);
      expect(assignMock).toHaveBeenCalledWith('/login');
    });

    it('does NOT trigger logout when refresh throws a non-terminal network error', async () => {
      const networkError = new Error('Network error') as AuthErrorWithCode;
      networkError.code = 'auth/network-request-failed';
      vi.mocked(authService.getCurrentIdToken).mockRejectedValueOnce(networkError);

      await expect(executeSingleFlightTokenRefresh()).rejects.toThrow('Network error');
      expect(authService.logoutUser).not.toHaveBeenCalled();
      expect(assignMock).not.toHaveBeenCalled();
    });

    it('does NOT trigger logout when refresh throws a rate-limit error (auth/too-many-requests)', async () => {
      const rateLimitError = new Error('Rate limit exceeded') as AuthErrorWithCode;
      rateLimitError.code = 'auth/too-many-requests';
      vi.mocked(authService.getCurrentIdToken).mockRejectedValueOnce(rateLimitError);

      await expect(executeSingleFlightTokenRefresh()).rejects.toThrow('Rate limit exceeded');
      expect(authService.logoutUser).not.toHaveBeenCalled();
      expect(assignMock).not.toHaveBeenCalled();
    });
  });

  describe('4. Axios Interceptor Recovery & Retry Flow', () => {
    it('catches HTTP 401, fetches fresh token, and replays request with new Authorization header', async () => {
      let callCount = 0;
      let authorizationSeenOnReplay = '';

      apiClient.defaults.adapter = async (config) => {
        callCount++;
        if (callCount === 1) {
          const err = new axios.AxiosError(
            'Unauthorized',
            'ERR_BAD_RESPONSE',
            config,
            null,
            {
              data: { error: 'Token expired' },
              status: 401,
              statusText: 'Unauthorized',
              headers: new AxiosHeaders(),
              config,
            } as AxiosResponse
          );
          throw err;
        }

        authorizationSeenOnReplay = config.headers?.Authorization as string;
        return {
          data: { success: true },
          status: 200,
          statusText: 'OK',
          headers: new AxiosHeaders(),
          config,
        };
      };

      vi.mocked(authService.getCurrentIdToken).mockImplementation(async (forceRefresh) => {
        if (forceRefresh) {
          return 'brand-new-token';
        }
        return 'old-token';
      });

      const response = await apiClient.get('/api/v1/profile');

      expect(callCount).toBe(2);
      expect(authorizationSeenOnReplay).toBe('Bearer brand-new-token');
      expect(response.status).toBe(200);
      expect(response.data).toEqual({ success: true });
    });

    it('coalesces concurrent 401 responses into a single refresh and retries all requests', async () => {
      let profileAttempts = 0;
      let farmsAttempts = 0;

      apiClient.defaults.adapter = async (config) => {
        if (config.url?.includes('profile')) {
          profileAttempts++;
          if (profileAttempts === 1) {
            throw new axios.AxiosError('401', 'ERR_BAD_RESPONSE', config, null, {
              status: 401,
              data: {},
              headers: new AxiosHeaders(),
              config,
            } as AxiosResponse);
          }
          return { data: { profile: 'data' }, status: 200, statusText: 'OK', headers: new AxiosHeaders(), config };
        } else {
          farmsAttempts++;
          if (farmsAttempts === 1) {
            throw new axios.AxiosError('401', 'ERR_BAD_RESPONSE', config, null, {
              status: 401,
              data: {},
              headers: new AxiosHeaders(),
              config,
            } as AxiosResponse);
          }
          return { data: { farms: [] }, status: 200, statusText: 'OK', headers: new AxiosHeaders(), config };
        }
      };

      vi.mocked(authService.getCurrentIdToken).mockImplementation(
        async (forceRefresh) => {
          if (forceRefresh) {
            await new Promise((resolve) => setTimeout(resolve, 20));
            return 'single-refresh-token';
          }
          return 'initial-token';
        }
      );

      const [r1, r2] = await Promise.all([
        apiClient.get('/api/v1/profile'),
        apiClient.get('/api/v1/farms'),
      ]);

      expect(r1.status).toBe(200);
      expect(r2.status).toBe(200);
      expect(authService.getCurrentIdToken).toHaveBeenCalledWith(true);
      // Ensure forceRefresh=true was called exactly once across both concurrent 401s
      const forceRefreshCalls = vi.mocked(authService.getCurrentIdToken).mock.calls.filter((c) => c[0] === true);
      expect(forceRefreshCalls).toHaveLength(1);
    });

    it('does not retry a request more than once if backend returns 401 again (rejects to caller)', async () => {
      let callCount = 0;

      apiClient.defaults.adapter = async (config) => {
        callCount++;
        throw new axios.AxiosError('401 Unauthorized', 'ERR_BAD_RESPONSE', config, null, {
          status: 401,
          data: { error: 'Still unauthorized' },
          headers: new AxiosHeaders(),
          config,
        } as AxiosResponse);
      };

      vi.mocked(authService.getCurrentIdToken).mockResolvedValue('refreshed-token');

      await expect(apiClient.get('/api/v1/protected')).rejects.toThrow();

      // Original request (1) + Retry attempt (2) = exactly 2 calls, never 3
      expect(callCount).toBe(2);
      // No logout triggered for retried backend rejection
      expect(authService.logoutUser).not.toHaveBeenCalled();
    });

    it('does NOT attempt refresh or retry for HTTP 403 Forbidden', async () => {
      let callCount = 0;

      apiClient.defaults.adapter = async (config) => {
        callCount++;
        throw new axios.AxiosError('Forbidden', 'ERR_BAD_RESPONSE', config, null, {
          status: 403,
          data: { error: 'Insufficient role permissions' },
          headers: new AxiosHeaders(),
          config,
        } as AxiosResponse);
      };

      await expect(apiClient.get('/api/v1/farmer-only')).rejects.toThrow();

      expect(callCount).toBe(1);
      const forceRefreshCalls = vi.mocked(authService.getCurrentIdToken).mock.calls.filter((c) => c[0] === true);
      expect(forceRefreshCalls).toHaveLength(0);
      expect(authService.logoutUser).not.toHaveBeenCalled();
    });

    it('does NOT attempt refresh or retry for HTTP 503 Service Unavailable', async () => {
      let callCount = 0;

      apiClient.defaults.adapter = async (config) => {
        callCount++;
        throw new axios.AxiosError('Service Unavailable', 'ERR_BAD_RESPONSE', config, null, {
          status: 503,
          data: { error: 'Rate limit or circuit open' },
          headers: new AxiosHeaders(),
          config,
        } as AxiosResponse);
      };

      await expect(apiClient.get('/api/v1/heavy-query')).rejects.toThrow();

      expect(callCount).toBe(1);
      const forceRefreshCalls = vi.mocked(authService.getCurrentIdToken).mock.calls.filter((c) => c[0] === true);
      expect(forceRefreshCalls).toHaveLength(0);
      expect(authService.logoutUser).not.toHaveBeenCalled();
    });

    it('does NOT attempt refresh or retry for HTTP 500 Internal Server Error', async () => {
      let callCount = 0;

      apiClient.defaults.adapter = async (config) => {
        callCount++;
        throw new axios.AxiosError('Internal Server Error', 'ERR_BAD_RESPONSE', config, null, {
          status: 500,
          data: { error: 'Server exploded' },
          headers: new AxiosHeaders(),
          config,
        } as AxiosResponse);
      };

      await expect(apiClient.get('/api/v1/server-crash')).rejects.toThrow();

      expect(callCount).toBe(1);
      const forceRefreshCalls = vi.mocked(authService.getCurrentIdToken).mock.calls.filter((c) => c[0] === true);
      expect(forceRefreshCalls).toHaveLength(0);
      expect(authService.logoutUser).not.toHaveBeenCalled();
    });

    it('propagates meaningful refresh failure and attaches original response context to caller', async () => {
      apiClient.defaults.adapter = async (config) => {
        throw new axios.AxiosError('Unauthorized', 'ERR_BAD_RESPONSE', config, null, {
          status: 401,
          statusText: 'Unauthorized',
          data: { error: 'Invalid JWT signature' },
          headers: new AxiosHeaders(),
          config,
        } as AxiosResponse);
      };

      const networkErr = new Error('Connection lost to auth server') as AuthErrorWithCode;
      networkErr.code = 'auth/network-request-failed';
      vi.mocked(authService.getCurrentIdToken).mockImplementation((forceRefresh) => {
        if (forceRefresh) return Promise.reject(networkErr);
        return Promise.resolve('init-token');
      });

      let caughtError: AuthErrorWithCode | undefined;
      try {
        await apiClient.get('/api/v1/data');
      } catch (err: unknown) {
        caughtError = err as AuthErrorWithCode;
      }

      expect(caughtError).toBeDefined();
      expect(caughtError?.message).toBe('Connection lost to auth server');
      expect(caughtError?.code).toBe('auth/network-request-failed');
      expect(caughtError?.response?.status).toBe(401);
      expect(caughtError?.response?.data).toEqual({ error: 'Invalid JWT signature' });
      expect(authService.logoutUser).not.toHaveBeenCalled();
      expect(assignMock).not.toHaveBeenCalled();
    });
  });
});
