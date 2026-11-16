import { AxiosError } from 'axios';
import { apiClient } from '../../../services/api';
import type {
  SessionInitRequest,
  SessionInitResponse,
  RouterTurnRequest,
  RouterTurnResponse,
  ChatError,
} from '../types';

/**
 * Initializes a new conversational session on the backend.
 * Calls POST /api/v1/router/session with the authenticated Axios client.
 * Binds the request to the expected owner UID through internal Axios configuration metadata
 * and accepts an optional AbortSignal for cancellation.
 * Returns 201 Created with server-generated UUIDv4 sessionId.
 */
export async function initSession(
  metadata?: string | null,
  ownerUid?: string,
  signal?: AbortSignal
): Promise<SessionInitResponse> {
  const payload: SessionInitRequest = metadata ? { metadata } : {};
  const response = await apiClient.post<SessionInitResponse>('/api/v1/router/session', payload, {
    expectedUid: ownerUid,
    signal,
  });
  return response.data;
}

/**
 * Dispatches an agricultural query turn to the router and orchestrator.
 * Calls POST /api/v1/router/turn with the authenticated Axios client.
 * Binds the request to the expected owner UID through internal Axios configuration metadata
 * and accepts an optional AbortSignal for cancellation.
 * Returns 200 OK with the orchestrated turn response.
 */
export async function sendTurn(
  request: RouterTurnRequest,
  ownerUid?: string,
  signal?: AbortSignal
): Promise<RouterTurnResponse> {
  const response = await apiClient.post<RouterTurnResponse>('/api/v1/router/turn', request, {
    expectedUid: ownerUid,
    signal,
  });
  return response.data;
}

/**
 * Parses and categorizes an Axios error from the router API into a typed ChatError.
 */
export function parseRouterError(error: unknown): ChatError {
  if (error instanceof AxiosError) {
    const status = error.response?.status || 0;
    const responseData = error.response?.data as { error?: string } | undefined;
    const serverMessage = responseData?.error || error.message;

    switch (status) {
      case 400:
        return {
          status,
          message: serverMessage || 'Invalid query parameter or turn index.',
          isRetryable: false,
          action: 'edit',
        };
      case 401:
        return {
          status,
          message: 'Your session has expired. Please refresh your credentials.',
          isRetryable: false,
        };
      case 403:
        return {
          status,
          message: serverMessage || 'Access restricted for your account role.',
          isRetryable: false,
        };
      case 404:
        return {
          status,
          message: 'Session expired or not found. A new session is required.',
          isRetryable: false,
          action: 'reinit',
        };
      case 409:
        return {
          status,
          message: serverMessage || 'Session synchronization conflict.',
          isRetryable: false,
          action: 'reinit',
        };
      case 410:
        return {
          status,
          message: 'Turn result has expired (410 GONE). The previous turn committed, but its cached result is no longer available.',
          isRetryable: false,
          action: 'reinit',
        };
      case 423:
        return {
          status,
          message: 'Assistant is busy processing your turn. Please wait a moment...',
          isRetryable: true,
          action: 'retry',
        };
      case 429:
        return {
          status,
          message: 'Rate limit reached. Please wait 5 seconds before sending another query.',
          isRetryable: false,
          action: 'cooldown',
        };
      case 500:
      case 503:
        return {
          status,
          message: serverMessage || 'Agricultural intelligence service temporarily unavailable. Please retry.',
          isRetryable: true,
          action: 'retry',
        };
      default:
        return {
          status,
          message: error.message || 'Network connection error. Please check your connection and retry.',
          isRetryable: true,
          action: 'retry',
        };
    }
  }

  return {
    status: 0,
    message: error instanceof Error ? error.message : 'An unexpected error occurred.',
    isRetryable: true,
    action: 'retry',
  };
}
