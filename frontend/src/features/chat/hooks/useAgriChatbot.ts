import { useState, useRef, useEffect, useCallback } from 'react';
import axios from 'axios';
import { useAuth } from '../../auth/hooks/useAuth';
import { auth } from '../../../config/firebase';
import type { VoiceLanguage } from '../../voice/types';
import { initSession, sendTurn, parseRouterError } from '../services/chatSessionService';
import type {
  ChatMessage,
  ChatSessionState,
  RouterTurnRequest,
  RouterTurnResponse,
  ChatError,
} from '../types';

export interface UseAgriChatbotReturn {
  sessionState: ChatSessionState;
  sendMessage: (query: string) => Promise<void>;
  retryLastTurn: () => Promise<void>;
  reinitAndResend: () => Promise<void>;
  resetSession: () => void;
  setLanguage: (lang: VoiceLanguage) => void;
  cooldownSeconds: number;
}

export function useAgriChatbot(): UseAgriChatbotReturn {
  const { currentUser } = useAuth();
  const currentUid = currentUser?.uid;

  const [sessionState, setSessionState] = useState<ChatSessionState>({
    sessionId: null,
    status: 'uninitialized',
    nextTurnIndex: 1, // First submitted turn is strictly 1
    messages: [],
    isProcessing: false,
    pendingRequest: null,
    error: null,
    language: 'en-IN',
  });

  const [cooldownSeconds, setCooldownSeconds] = useState<number>(0);

  // Active turn execution lock, retry counter, and references
  const isExecutingRef = useRef<boolean>(false);
  const lockRetryCountRef = useRef<number>(0);
  const activeSessionIdRef = useRef<string | null>(null);
  const nextTurnIndexRef = useRef<number>(1);
  const pendingRequestRef = useRef<RouterTurnRequest | null>(null);

  // Browser-compatible retry timer handle
  const retryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Request generation counter to safely reject stale async responses
  const requestGenerationRef = useRef<number>(0);

  // AbortController handle for active in-flight network requests
  const abortControllerRef = useRef<AbortController | null>(null);

  // Synchronous ref tracking active authenticated UID to prevent race conditions
  const currentUidRef = useRef<string | undefined>(currentUid);
  useEffect(() => {
    currentUidRef.current = currentUid;
  }, [currentUid]);

  // Keep state refs in sync for race-free async operations
  useEffect(() => {
    activeSessionIdRef.current = sessionState.sessionId;
    nextTurnIndexRef.current = sessionState.nextTurnIndex;
    pendingRequestRef.current = sessionState.pendingRequest;
  }, [sessionState.sessionId, sessionState.nextTurnIndex, sessionState.pendingRequest]);

  // Helper to safely clear pending retry timer
  const clearRetryTimer = useCallback(() => {
    if (retryTimerRef.current !== null) {
      clearTimeout(retryTimerRef.current);
      retryTimerRef.current = null;
    }
  }, []);

  // Helper to safely abort active in-flight network requests
  const abortActiveOperations = useCallback(() => {
    if (abortControllerRef.current !== null) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
  }, []);

  // Auth boundary: Wipe session state completely on logout or authenticated UID change
  useEffect(() => {
    clearRetryTimer();
    abortActiveOperations();
    requestGenerationRef.current += 1; // Invalidate all prior in-flight requests
    isExecutingRef.current = false;
    lockRetryCountRef.current = 0;
    activeSessionIdRef.current = null;
    nextTurnIndexRef.current = 1;
    pendingRequestRef.current = null;

    setSessionState((prev) => ({
      sessionId: null,
      status: 'uninitialized',
      nextTurnIndex: 1,
      messages: [],
      isProcessing: false,
      pendingRequest: null,
      error: null,
      language: prev.language,
    }));
  }, [currentUid, clearRetryTimer, abortActiveOperations]);

  // Hook teardown: cancel pending timers and abort in-flight requests on unmount
  useEffect(() => {
    return () => {
      clearRetryTimer();
      abortActiveOperations();
      requestGenerationRef.current += 1;
    };
  }, [clearRetryTimer, abortActiveOperations]);

  // Cooldown countdown timer for HTTP 429
  useEffect(() => {
    if (cooldownSeconds <= 0) return;
    const timer = setInterval(() => {
      setCooldownSeconds((prev) => (prev > 1 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldownSeconds]);

  const setLanguage = useCallback((lang: VoiceLanguage) => {
    setSessionState((prev) => ({ ...prev, language: lang }));
  }, []);

  const resetSession = useCallback(() => {
    clearRetryTimer();
    abortActiveOperations();
    requestGenerationRef.current += 1; // Invalidate any older in-flight requests or retries
    isExecutingRef.current = false;
    lockRetryCountRef.current = 0;
    activeSessionIdRef.current = null;
    nextTurnIndexRef.current = 1;
    pendingRequestRef.current = null;

    setSessionState((prev) => ({
      ...prev,
      sessionId: null,
      status: 'uninitialized',
      nextTurnIndex: 1,
      messages: [],
      isProcessing: false,
      pendingRequest: null,
      error: null,
    }));
  }, [clearRetryTimer, abortActiveOperations]);

  /**
   * Reusable authorization and freshness validator for all asynchronous operations.
   * Establishes that:
   * 1. The request owner UID is defined.
   * 2. The request owner UID matches auth.currentUser?.uid (the live Firebase Auth singleton).
   * 3. The request owner UID matches currentUidRef.current.
   * 4. The request generation remains strictly current.
   * 5. The active session matches the expected session (when specified).
   */
  const isRequestAuthorized = useCallback(
    (
      generation: number,
      requestOwnerUid: string | undefined,
      expectedSessionId?: string | null
    ): boolean => {
      // 1. Request owner UID must be defined
      if (!requestOwnerUid) {
        return false;
      }

      // 2. Request owner UID must match the live Firebase Auth singleton identity
      const liveFirebaseUid = auth.currentUser?.uid;
      if (!liveFirebaseUid || liveFirebaseUid !== requestOwnerUid) {
        return false;
      }

      // 3. Request owner UID must match currentUidRef
      if (currentUidRef.current !== requestOwnerUid) {
        return false;
      }

      // 4. Request generation must remain strictly current
      if (generation !== requestGenerationRef.current) {
        return false;
      }

      // 5. Active session must match expected session if specified
      if (
        expectedSessionId !== undefined &&
        expectedSessionId !== null &&
        activeSessionIdRef.current !== expectedSessionId
      ) {
        return false;
      }

      return true;
    },
    []
  );

  /**
   * Internal turn dispatch engine with idempotent retry, locked backoff,
   * generation tracking, live identity verification, and stale response protection.
   */
  const executeTurn = useCallback(
    async (
      turnReq: RouterTurnRequest,
      generation: number,
      requestOwnerUid: string | undefined,
      signal?: AbortSignal,
      isRetry: boolean = false
    ): Promise<void> => {
      // Pre-dispatch guard: check live auth identity, generation, and session validity
      if (
        !isRequestAuthorized(generation, requestOwnerUid, turnReq.sessionId) ||
        signal?.aborted
      ) {
        return;
      }

      isExecutingRef.current = true;
      setSessionState((prev) => ({
        ...prev,
        isProcessing: true,
        error: null,
        pendingRequest: turnReq,
      }));

      try {
        const response: RouterTurnResponse = await sendTurn(turnReq, requestOwnerUid, signal);

        // Post-response guard: reject stale in-flight response from older session or account switch
        if (
          !isRequestAuthorized(generation, requestOwnerUid, response.sessionId) ||
          signal?.aborted
        ) {
          isExecutingRef.current = false;
          return;
        }

        // Turn confirmed committed on server
        lockRetryCountRef.current = 0;
        isExecutingRef.current = false;

        const assistantMsg: ChatMessage = {
          id: `asst-${response.turnIndex}-${Date.now()}`,
          sender: 'assistant',
          text: response.orchestration?.synthesizedText || 'Response received.',
          timestamp: Date.now(),
          turnIndex: response.turnIndex,
          status: 'committed',
          turnResponse: response,
        };

        setSessionState((prev) => {
          // Double-check session boundary inside updater
          if (prev.sessionId !== response.sessionId) return prev;
          return {
            ...prev,
            status: 'ready',
            nextTurnIndex: response.turnIndex + 1, // Advance turn index only after confirmed HTTP 200
            messages: [...prev.messages, assistantMsg],
            isProcessing: false,
            pendingRequest: null,
            error: null,
          };
        });
      } catch (err: unknown) {
        // If aborted or cancelled (DOM AbortController or Axios CanceledError), exit cleanly without error state
        if (
          axios.isCancel(err) ||
          (err as { name?: string })?.name === 'CanceledError' ||
          (err as { name?: string })?.name === 'AbortError' ||
          signal?.aborted ||
          !isRequestAuthorized(generation, requestOwnerUid, turnReq.sessionId)
        ) {
          isExecutingRef.current = false;
          return;
        }

        const chatErr = parseRouterError(err);
        isExecutingRef.current = false;

        // 1. HTTP 423 (Locked): Bounded backoff retry preserving identical request tuple
        if (chatErr.status === 423 && lockRetryCountRef.current < 2) {
          lockRetryCountRef.current += 1;
          setSessionState((prev) => ({ ...prev, status: 'locked' }));

          clearRetryTimer();
          retryTimerRef.current = setTimeout(() => {
            retryTimerRef.current = null;
            // Guard conditions before executing retry
            if (isRequestAuthorized(generation, requestOwnerUid, turnReq.sessionId)) {
              const retrySignal = abortControllerRef.current?.signal;
              executeTurn(turnReq, generation, requestOwnerUid, retrySignal, true);
            }
          }, 2000);
          return;
        }

        lockRetryCountRef.current = 0;

        // 2. HTTP 429: Rate limit cooldown
        if (chatErr.status === 429) {
          setCooldownSeconds(5);
        }

        // 3. Construct descriptive assistant error/guidance message
        let errorHelpText = chatErr.message;
        if (chatErr.status === 410) {
          errorHelpText =
            'Turn result has expired (HTTP 410). The previous turn was committed, but its cached result is no longer available in the session history.';
        } else if (chatErr.status === 409) {
          errorHelpText =
            'Session synchronization conflict (HTTP 409). The turn sequence or request state is out of sync with the server.';
        }

        const errorMsg: ChatMessage = {
          id: `err-${turnReq.turnIndex}-${Date.now()}`,
          sender: 'system',
          text: errorHelpText,
          timestamp: Date.now(),
          turnIndex: turnReq.turnIndex,
          status: 'error',
          error: chatErr.message,
          isRetryable: chatErr.isRetryable,
        };

        setSessionState((prev) => ({
          ...prev,
          status: 'error',
          messages: [...prev.messages, errorMsg],
          isProcessing: false,
          error: chatErr,
        }));
      }
    },
    [clearRetryTimer, isRequestAuthorized]
  );

  /**
   * Submits a natural-language agricultural question.
   */
  const sendMessage = useCallback(
    async (rawQuery: string): Promise<void> => {
      const query = rawQuery?.trim();
      if (!query) return;

      // Prevent concurrent submissions or submissions during cooldown
      if (isExecutingRef.current || sessionState.isProcessing || cooldownSeconds > 0) {
        return;
      }

      // Clear any pending retry timer before starting a new turn
      clearRetryTimer();

      // Advance request generation for the new turn
      requestGenerationRef.current += 1;
      const currentGen = requestGenerationRef.current;
      const requestUid = currentUidRef.current;

      // Pre-submission identity check
      if (!isRequestAuthorized(currentGen, requestUid)) {
        return;
      }

      // Abort any prior in-flight request and create a fresh AbortController for this turn
      abortActiveOperations();
      abortControllerRef.current = new AbortController();
      const signal = abortControllerRef.current.signal;

      // Query length safeguard matching backend constraint (<= 1000 characters)
      if (query.length > 1000) {
        const validationErr: ChatMessage = {
          id: `val-err-${Date.now()}`,
          sender: 'system',
          text: 'Query exceeds the maximum allowed length of 1,000 characters. Please shorten your question.',
          timestamp: Date.now(),
          status: 'error',
          isRetryable: false,
        };
        setSessionState((prev) => ({
          ...prev,
          messages: [...prev.messages, validationErr],
        }));
        return;
      }

      let activeSessionId = activeSessionIdRef.current;
      let turnIndex = nextTurnIndexRef.current;

      // Lazy session initialization if uninitialized
      if (!activeSessionId) {
        if (!isRequestAuthorized(currentGen, requestUid)) {
          return;
        }

        setSessionState((prev) => ({ ...prev, status: 'initializing', isProcessing: true }));
        try {
          const initRes = await initSession(null, requestUid, signal);

          // Reject if generation or live Firebase user has changed while init was in-flight
          if (!isRequestAuthorized(currentGen, requestUid) || signal.aborted) {
            return;
          }

          activeSessionId = initRes.sessionId;
          activeSessionIdRef.current = activeSessionId;
          turnIndex = 1;
          nextTurnIndexRef.current = 1;
        } catch (initErr: unknown) {
          if (
            axios.isCancel(initErr) ||
            (initErr as { name?: string })?.name === 'CanceledError' ||
            (initErr as { name?: string })?.name === 'AbortError' ||
            signal.aborted ||
            !isRequestAuthorized(currentGen, requestUid)
          ) {
            return;
          }

          const chatErr = parseRouterError(initErr);
          const errorMsg: ChatMessage = {
            id: `init-err-${Date.now()}`,
            sender: 'system',
            text: `Failed to initialize session: ${chatErr.message}`,
            timestamp: Date.now(),
            status: 'error',
            isRetryable: true,
          };
          setSessionState((prev) => ({
            ...prev,
            status: 'error',
            messages: [...prev.messages, errorMsg],
            isProcessing: false,
            error: chatErr,
          }));
          return;
        }
      }

      if (!isRequestAuthorized(currentGen, requestUid, activeSessionId) || signal.aborted) {
        return;
      }

      const turnReq: RouterTurnRequest = {
        sessionId: activeSessionId,
        turnIndex,
        query,
      };

      const userMsg: ChatMessage = {
        id: `user-${turnIndex}-${Date.now()}`,
        sender: 'user',
        text: query,
        timestamp: Date.now(),
        turnIndex,
        status: 'committed',
      };

      setSessionState((prev) => ({
        ...prev,
        sessionId: activeSessionId,
        status: 'ready',
        messages: [...prev.messages, userMsg],
      }));

      await executeTurn(turnReq, currentGen, requestUid, signal);
    },
    [
      sessionState.isProcessing,
      cooldownSeconds,
      clearRetryTimer,
      abortActiveOperations,
      executeTurn,
      isRequestAuthorized,
    ]
  );

  /**
   * Retries the identical pending request tuple after network error or 423 lock timeout.
   */
  const retryLastTurn = useCallback(async (): Promise<void> => {
    const pending = pendingRequestRef.current;
    if (!pending || isExecutingRef.current) return;

    clearRetryTimer();
    abortActiveOperations();
    abortControllerRef.current = new AbortController();
    const signal = abortControllerRef.current.signal;

    const currentGen = requestGenerationRef.current;
    const requestUid = currentUidRef.current;

    if (!isRequestAuthorized(currentGen, requestUid, pending.sessionId)) {
      return;
    }

    await executeTurn(pending, currentGen, requestUid, signal, true);
  }, [clearRetryTimer, abortActiveOperations, executeTurn, isRequestAuthorized]);

  /**
   * Explicit user-directed recovery: initializes a clean session and dispatches the preserved query.
   */
  const reinitAndResend = useCallback(async (): Promise<void> => {
    const pending = pendingRequestRef.current;
    if (!pending || isExecutingRef.current) return;

    const queryToResend = pending.query;

    // Reset session state and bump generation to invalidate old requests
    resetSession();
    requestGenerationRef.current += 1;
    const currentGen = requestGenerationRef.current;
    const requestUid = currentUidRef.current;

    if (!isRequestAuthorized(currentGen, requestUid)) {
      return;
    }

    abortActiveOperations();
    abortControllerRef.current = new AbortController();
    const signal = abortControllerRef.current.signal;

    setSessionState((prev) => ({ ...prev, status: 'initializing', isProcessing: true }));

    try {
      const initRes = await initSession(null, requestUid, signal);

      if (!isRequestAuthorized(currentGen, requestUid) || signal.aborted) {
        return;
      }

      activeSessionIdRef.current = initRes.sessionId;
      nextTurnIndexRef.current = 1;

      const newTurnReq: RouterTurnRequest = {
        sessionId: initRes.sessionId,
        turnIndex: 1,
        query: queryToResend,
      };

      const userMsg: ChatMessage = {
        id: `user-1-${Date.now()}`,
        sender: 'user',
        text: queryToResend,
        timestamp: Date.now(),
        turnIndex: 1,
        status: 'committed',
      };

      setSessionState((prev) => ({
        ...prev,
        sessionId: initRes.sessionId,
        status: 'ready',
        nextTurnIndex: 1,
        messages: [...prev.messages, userMsg],
      }));

      await executeTurn(newTurnReq, currentGen, requestUid, signal);
    } catch (err: unknown) {
      if (
        axios.isCancel(err) ||
        (err as { name?: string })?.name === 'CanceledError' ||
        (err as { name?: string })?.name === 'AbortError' ||
        signal.aborted ||
        !isRequestAuthorized(currentGen, requestUid)
      ) {
        return;
      }

      const chatErr = parseRouterError(err);
      setSessionState((prev) => ({
        ...prev,
        status: 'error',
        isProcessing: false,
        error: chatErr,
      }));
    }
  }, [resetSession, abortActiveOperations, executeTurn, isRequestAuthorized]);

  return {
    sessionState,
    sendMessage,
    retryLastTurn,
    reinitAndResend,
    resetSession,
    setLanguage,
    cooldownSeconds,
  };
}
