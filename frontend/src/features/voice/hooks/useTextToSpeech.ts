import { useState, useRef, useEffect, useCallback } from 'react';
import type { VoiceLanguage, UseTextToSpeechOptions, UseTextToSpeechReturn } from '../types';

export function useTextToSpeech(options: UseTextToSpeechOptions = {}): UseTextToSpeechReturn {
  const {
    language: defaultLanguage = 'en-IN',
    pitch = 1.0,
    rate = 1.0,
    volume = 1.0,
    onEnd,
    onError,
  } = options;

  const [isSpeaking, setIsSpeaking] = useState<boolean>(false);

  const isSupported =
    typeof window !== 'undefined' &&
    'speechSynthesis' in window &&
    typeof SpeechSynthesisUtterance !== 'undefined';

  const activeUtteranceRef = useRef<SpeechSynthesisUtterance | null>(null);

  const cancel = useCallback(() => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    setIsSpeaking(false);
    activeUtteranceRef.current = null;
  }, []);

  const pause = useCallback(() => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.pause();
    }
  }, []);

  const resume = useCallback(() => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.resume();
    }
  }, []);

  const speak = useCallback(
    (text: string, langOverride?: VoiceLanguage) => {
      if (!isSupported) {
        const errorMsg = 'Text-to-speech narration is not supported in this browser.';
        onError?.(errorMsg);
        return;
      }

      if (!text || !text.trim()) {
        return;
      }

      // Prevent overlapping narration: cancel any ongoing speech
      cancel();

      try {
        const targetLang = langOverride || defaultLanguage;
        const utterance = new SpeechSynthesisUtterance(text.trim());
        utterance.lang = targetLang;
        utterance.pitch = pitch;
        utterance.rate = rate;
        utterance.volume = volume;

        // Graceful voice selection: attempt to match exact or language prefix, fallback to browser default
        const availableVoices = window.speechSynthesis.getVoices?.() || [];
        if (availableVoices.length > 0) {
          const exactVoice = availableVoices.find((v) => v.lang === targetLang);
          const langPrefix = targetLang.split('-')[0].toLowerCase();
          const prefixVoice = availableVoices.find((v) => v.lang.toLowerCase().startsWith(langPrefix));
          if (exactVoice) {
            utterance.voice = exactVoice;
          } else if (prefixVoice) {
            utterance.voice = prefixVoice;
          }
        }

        utterance.onstart = () => {
          setIsSpeaking(true);
        };

        utterance.onend = () => {
          setIsSpeaking(false);
          activeUtteranceRef.current = null;
          onEnd?.();
        };

        utterance.onerror = (event: SpeechSynthesisErrorEvent) => {
          setIsSpeaking(false);
          activeUtteranceRef.current = null;
          // Suppress errors caused by intentional cancellation
          if (event.error !== 'interrupted' && event.error !== 'canceled') {
            const errorMsg = `Text-to-speech error: ${event.error}`;
            onError?.(errorMsg);
          }
        };

        activeUtteranceRef.current = utterance;
        window.speechSynthesis.speak(utterance);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Failed to execute speech synthesis.';
        setIsSpeaking(false);
        activeUtteranceRef.current = null;
        onError?.(message);
      }
    },
    [isSupported, defaultLanguage, pitch, rate, volume, cancel, onEnd, onError]
  );

  // Teardown speech synthesis on unmount
  useEffect(() => {
    return () => {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    };
  }, []);

  return {
    isSpeaking,
    isSupported,
    speak,
    cancel,
    pause,
    resume,
  };
}

