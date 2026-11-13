import { useState, useRef, useEffect, useCallback } from 'react';
import type { VoiceLanguage, UseSpeechToTextOptions, UseSpeechToTextReturn } from '../types';

// Browser-safe interface declarations for Web Speech API
interface SpeechRecognitionEventLike {
  resultIndex: number;
  results: {
    length: number;
    [index: number]: {
      isFinal: boolean;
      length: number;
      [index: number]: {
        transcript: string;
        confidence: number;
      };
    };
  };
}

interface SpeechRecognitionErrorEventLike {
  error: string;
  message?: string;
}

interface SpeechRecognitionInstance {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  maxAlternatives?: number;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onstart: (() => void) | null;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEventLike) => void) | null;
  onend: (() => void) | null;
}

type SpeechRecognitionConstructor = new () => SpeechRecognitionInstance;

function getSpeechRecognitionConstructor(): SpeechRecognitionConstructor | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const win = window as unknown as {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  };
  return win.SpeechRecognition || win.webkitSpeechRecognition || null;
}

export function useSpeechToText(options: UseSpeechToTextOptions = {}): UseSpeechToTextReturn {
  const {
    language: initialLanguage = 'en-IN',
    continuous = false,
    interimResults = true,
    onResult,
    onError,
  } = options;

  const [language, setLanguage] = useState<VoiceLanguage>(initialLanguage);
  const [isListening, setIsListening] = useState<boolean>(false);
  const [transcript, setTranscript] = useState<string>('');
  const [interimTranscript, setInterimTranscript] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);
  const isExplicitStopRef = useRef<boolean>(false);

  // Check support statically/reactively
  const isSupported = typeof window !== 'undefined' && getSpeechRecognitionConstructor() !== null;

  // Cleanup handler
  const cleanupRecognition = useCallback(() => {
    if (recognitionRef.current) {
      try {
        recognitionRef.current.onstart = null;
        recognitionRef.current.onresult = null;
        recognitionRef.current.onerror = null;
        recognitionRef.current.onend = null;
        recognitionRef.current.abort();
      } catch {
        // Ignore abort errors during cleanup
      }
      recognitionRef.current = null;
    }
  }, []);

  const stopListening = useCallback(() => {
    isExplicitStopRef.current = true;
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop();
      } catch {
        // If already stopped or aborting
      }
    }
    setIsListening(false);
  }, []);

  const startListening = useCallback(
    (langOverride?: VoiceLanguage) => {
      if (!isSupported) {
        const unsupportedMsg = 'Speech recognition is not supported in this browser. Please use Chrome, Edge, or a supported browser.';
        setError(unsupportedMsg);
        onError?.(unsupportedMsg);
        return;
      }

      // Prevent duplicate active sessions
      if (isListening && recognitionRef.current) {
        return;
      }

      // Reset state prior to starting new session
      cleanupRecognition();
      setError(null);
      setInterimTranscript('');
      isExplicitStopRef.current = false;

      const Constructor = getSpeechRecognitionConstructor();
      if (!Constructor) {
        return;
      }

      try {
        const recognition = new Constructor();
        const activeLang = langOverride || language;
        recognition.lang = activeLang;
        recognition.continuous = continuous;
        recognition.interimResults = interimResults;

        recognition.onstart = () => {
          setIsListening(true);
        };

        recognition.onresult = (event: SpeechRecognitionEventLike) => {
          let accumulatedFinal = '';
          let currentInterim = '';

          for (let i = event.resultIndex; i < event.results.length; i++) {
            const resultItem = event.results[i];
            const text = resultItem[0]?.transcript || '';
            if (resultItem.isFinal) {
              accumulatedFinal += text;
            } else {
              currentInterim += text;
            }
          }

          if (accumulatedFinal) {
            setTranscript((prev) => {
              const updated = prev ? `${prev} ${accumulatedFinal.trim()}` : accumulatedFinal.trim();
              onResult?.(updated);
              return updated;
            });
          }

          setInterimTranscript(currentInterim);
        };

        recognition.onerror = (event: SpeechRecognitionErrorEventLike) => {
          let errorMsg = 'An unexpected speech recognition error occurred.';

          switch (event.error) {
            case 'not-allowed':
              errorMsg = 'Microphone permission was denied. Please allow microphone access in your browser settings to use voice input.';
              break;
            case 'no-speech':
              errorMsg = 'No speech was detected. Please check your microphone and try speaking again.';
              break;
            case 'network':
              errorMsg = 'Network error during speech recognition. An active internet connection is required by browser speech services.';
              break;
            case 'audio-capture':
              errorMsg = 'No microphone device was detected or your microphone is busy.';
              break;
            case 'aborted':
              // Suppress error message if user explicitly stopped
              if (isExplicitStopRef.current) {
                return;
              }
              errorMsg = 'Speech recognition was aborted.';
              break;
            case 'language-not-supported':
              errorMsg = `The selected language (${activeLang}) is not supported on your browser's speech recognition service.`;
              break;
            default:
              errorMsg = `Speech recognition error: ${event.error}`;
              break;
          }

          setError(errorMsg);
          onError?.(errorMsg);
          setIsListening(false);
        };

        recognition.onend = () => {
          setIsListening(false);
          recognitionRef.current = null;
        };

        recognitionRef.current = recognition;
        recognition.start();
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Failed to initialize speech recognition.';
        setError(message);
        onError?.(message);
        setIsListening(false);
      }
    },
    [isSupported, isListening, language, continuous, interimResults, onResult, onError, cleanupRecognition]
  );

  const resetTranscript = useCallback(() => {
    setTranscript('');
    setInterimTranscript('');
    setError(null);
  }, []);

  // Teardown recognition on unmount
  useEffect(() => {
    return () => {
      cleanupRecognition();
    };
  }, [cleanupRecognition]);

  return {
    isListening,
    transcript,
    interimTranscript,
    error,
    isSupported,
    language,
    setLanguage,
    startListening,
    stopListening,
    resetTranscript,
  };
}

