import React from 'react';
import { Volume2, VolumeX } from 'lucide-react';
import { useTextToSpeech } from '../hooks/useTextToSpeech';
import type { VoiceLanguage } from '../types';

export interface AudioNarrationButtonProps {
  text: string;
  language?: VoiceLanguage;
  disabled?: boolean;
  className?: string;
  label?: string;
  size?: 'sm' | 'md';
}

export const AudioNarrationButton: React.FC<AudioNarrationButtonProps> = ({
  text,
  language = 'en-IN',
  disabled = false,
  className = '',
  label,
  size = 'md',
}) => {
  const { isSpeaking, isSupported, speak, cancel } = useTextToSpeech({
    language,
  });

  if (!isSupported) {
    return null;
  }

  const handleToggle = () => {
    if (isSpeaking) {
      cancel();
    } else {
      speak(text, language);
    }
  };

  const isSmall = size === 'sm';

  return (
    <button
      type="button"
      onClick={handleToggle}
      disabled={disabled || !text || !text.trim()}
      aria-label={isSpeaking ? 'Stop audio narration' : (label || 'Listen to narration')}
      aria-pressed={isSpeaking}
      className={`inline-flex items-center gap-1.5 rounded-xl font-medium transition-all duration-200 select-none focus:outline-none focus:ring-2 focus:ring-emerald-500/50 disabled:opacity-40 disabled:cursor-not-allowed ${
        isSmall ? 'px-2.5 py-1 text-xs' : 'px-3.5 py-2 text-xs font-semibold'
      } ${
        isSpeaking
          ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/60 shadow-lg shadow-emerald-950/40 animate-pulse'
          : 'bg-slate-900 text-slate-300 border border-slate-700 hover:border-emerald-500/50 hover:text-emerald-300 hover:bg-slate-800'
      } ${className}`}
    >
      {isSpeaking ? (
        <>
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
          </span>
          <VolumeX className="w-3.5 h-3.5 text-emerald-400" />
          <span>{label ? `Stop ${label}` : 'Stop Narration'}</span>
        </>
      ) : (
        <>
          <Volume2 className="w-3.5 h-3.5 text-emerald-400" />
          <span>{label || 'Listen'}</span>
        </>
      )}
    </button>
  );
};

