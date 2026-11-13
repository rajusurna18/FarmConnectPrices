import React from 'react';
import { Mic, MicOff, AlertCircle } from 'lucide-react';
import { useSpeechToText } from '../hooks/useSpeechToText';
import type { VoiceLanguage } from '../types';

export interface VoiceInputButtonProps {
  onTranscript: (transcript: string) => void;
  language?: VoiceLanguage;
  disabled?: boolean;
  className?: string;
  label?: string;
  showLanguageBadge?: boolean;
}

export const VoiceInputButton: React.FC<VoiceInputButtonProps> = ({
  onTranscript,
  language = 'en-IN',
  disabled = false,
  className = '',
  label,
  showLanguageBadge = true,
}) => {
  const {
    isListening,
    error,
    isSupported,
    startListening,
    stopListening,
    resetTranscript,
  } = useSpeechToText({
    language,
    onResult: (text) => {
      onTranscript(text);
    },
  });

  const handleToggle = () => {
    if (isListening) {
      stopListening();
    } else {
      resetTranscript();
      startListening(language);
    }
  };

  const getLangCodeShort = (lang: VoiceLanguage): string => {
    switch (lang) {
      case 'hi-IN': return 'HI';
      case 'te-IN': return 'TE';
      case 'ta-IN': return 'TA';
      default: return 'EN';
    }
  };

  if (!isSupported) {
    return (
      <div className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-xl text-slate-500 text-xs select-none">
        <MicOff className="w-3.5 h-3.5 text-slate-500" />
        <span>Voice unsupported</span>
      </div>
    );
  }

  return (
    <div className="inline-flex flex-col items-start gap-1">
      <button
        type="button"
        onClick={handleToggle}
        disabled={disabled}
        aria-label={isListening ? 'Stop voice recording' : (label || 'Start voice recording')}
        aria-pressed={isListening}
        className={`relative inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-200 select-none focus:outline-none focus:ring-2 focus:ring-emerald-500/50 disabled:opacity-50 disabled:cursor-not-allowed ${
          isListening
            ? 'bg-rose-500/20 text-rose-300 border border-rose-500/60 shadow-lg shadow-rose-950/40 animate-pulse'
            : 'bg-slate-900 text-slate-200 border border-slate-700 hover:border-emerald-500/50 hover:text-emerald-300 hover:bg-slate-800'
        } ${className}`}
      >
        {isListening ? (
          <>
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-rose-500"></span>
            </span>
            <Mic className="w-3.5 h-3.5 text-rose-400 animate-bounce" />
            <span>Listening...</span>
          </>
        ) : (
          <>
            <Mic className="w-3.5 h-3.5 text-emerald-400" />
            <span>{label || 'Voice Input'}</span>
          </>
        )}

        {showLanguageBadge && (
          <span className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-bold uppercase ${
            isListening ? 'bg-rose-950 text-rose-300' : 'bg-slate-800 text-slate-400'
          }`}>
            {getLangCodeShort(language)}
          </span>
        )}
      </button>

      {/* Inline Non-Intrusive Error Notice */}
      {error && (
        <div
          role="alert"
          className="flex items-center gap-1.5 px-2.5 py-1 bg-rose-950/80 border border-rose-500/30 rounded-lg text-rose-300 text-[11px] max-w-xs animate-fade-in"
        >
          <AlertCircle className="w-3 h-3 shrink-0 text-rose-400" />
          <span className="truncate">{error}</span>
        </div>
      )}
    </div>
  );
};

