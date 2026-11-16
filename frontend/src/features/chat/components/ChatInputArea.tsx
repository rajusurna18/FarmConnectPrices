import React, { useState, useRef, useEffect } from 'react';
import { Send, Globe, AlertCircle, Loader2 } from 'lucide-react';
import { VoiceInputButton } from '../../voice/components/VoiceInputButton';
import { abortActiveSpeechRecognition } from '../../voice/hooks/useSpeechToText';
import { SUPPORTED_VOICE_LANGUAGES } from '../../voice/types';
import type { VoiceLanguage } from '../../voice/types';

export interface ChatInputAreaProps {
  onSendMessage: (query: string) => Promise<void>;
  isProcessing: boolean;
  cooldownSeconds: number;
  language: VoiceLanguage;
  onLanguageChange: (lang: VoiceLanguage) => void;
  disabled?: boolean;
  placeholder?: string;
  stagedQuery?: string;
  onStagedQueryConsumed?: () => void;
}

export const ChatInputArea: React.FC<ChatInputAreaProps> = ({
  onSendMessage,
  isProcessing,
  cooldownSeconds,
  language,
  onLanguageChange,
  disabled = false,
  placeholder = 'Ask anything about crops, market prices, pests, soil...',
  stagedQuery,
  onStagedQueryConsumed,
}) => {
  const [inputText, setInputText] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  // Synchronize staged clarification or suggestion query into the input field
  useEffect(() => {
    if (stagedQuery) {
      setInputText(stagedQuery);
      onStagedQueryConsumed?.();
      if (textareaRef.current) {
        textareaRef.current.focus();
      }
    }
  }, [stagedQuery, onStagedQueryConsumed]);

  // Auto-resize textarea height
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 120)}px`;
    }
  }, [inputText]);

  const handleVoiceTranscript = (transcript: string) => {
    if (!transcript) return;
    setInputText((prev) => {
      const trimmed = prev.trim();
      return trimmed ? `${trimmed} ${transcript}` : transcript;
    });
  };

  const handleSend = async () => {
    const trimmed = inputText.trim();
    if (!trimmed || isProcessing || disabled || cooldownSeconds > 0) return;

    // 1. Immediately abort active microphone capture so dictation halts
    abortActiveSpeechRecognition();

    // 2. Cancel speech synthesis if speaking when user dispatches turn
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      try {
        window.speechSynthesis.cancel();
      } catch {
        // Ignore cancellation errors
      }
    }

    setInputText('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }

    await onSendMessage(trimmed);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const isSendDisabled = !inputText.trim() || isProcessing || disabled || cooldownSeconds > 0;

  return (
    <div className="flex flex-col gap-2 p-3 bg-slate-950/90 border-t border-slate-800">
      {/* Cooldown Warning Notice */}
      {cooldownSeconds > 0 && (
        <div
          role="alert"
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-amber-950/70 border border-amber-500/40 text-amber-300 text-xs animate-fade-in"
        >
          <AlertCircle className="w-3.5 h-3.5 text-amber-400 shrink-0" />
          <span>
            Rate limit active. Please wait{' '}
            <strong className="font-mono">{cooldownSeconds}s</strong> before sending another
            turn.
          </span>
        </div>
      )}

      {/* Input Row */}
      <div className="relative flex items-end gap-2 bg-slate-900 border border-slate-800 rounded-xl p-2 focus-within:border-emerald-500/60 focus-within:ring-1 focus-within:ring-emerald-500/50 transition-all">
        {/* Auto-growing Textarea */}
        <textarea
          ref={textareaRef}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={cooldownSeconds > 0 ? `Cooling down (${cooldownSeconds}s)...` : placeholder}
          disabled={disabled || cooldownSeconds > 0}
          rows={1}
          className="flex-1 bg-transparent border-0 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-0 resize-none min-h-[36px] max-h-[120px] py-1 px-1.5 leading-relaxed"
        />

        {/* Action Controls: Voice + Send */}
        <div className="flex items-center gap-1.5 shrink-0 self-end pb-0.5">
          {/* Integrated Voice Input */}
          <VoiceInputButton
            onTranscript={handleVoiceTranscript}
            language={language}
            disabled={disabled || cooldownSeconds > 0 || isProcessing}
            showLanguageBadge={false}
            className="p-1.5 px-2 text-xs rounded-lg"
          />

          {/* Send Button */}
          <button
            type="button"
            onClick={handleSend}
            disabled={isSendDisabled}
            aria-label="Send message"
            className="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 disabled:bg-slate-800 text-white disabled:text-slate-600 transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/50 disabled:cursor-not-allowed"
          >
            {isProcessing ? (
              <Loader2 className="w-4 h-4 animate-spin text-emerald-300" />
            ) : (
              <Send className="w-4 h-4" />
            )}
          </button>
        </div>
      </div>

      {/* Footer Controls: Language Selector and Helper Hints */}
      <div className="flex items-center justify-between text-[11px] text-slate-400 px-1">
        <div className="flex items-center gap-1.5">
          <Globe className="w-3 h-3 text-slate-500" />
          <select
            value={language}
            onChange={(e) => onLanguageChange(e.target.value as VoiceLanguage)}
            disabled={disabled || isProcessing}
            aria-label="Select voice and response language"
            className="bg-slate-900 border border-slate-800 text-slate-300 rounded px-1.5 py-0.5 text-[11px] focus:outline-none focus:border-emerald-500/50 cursor-pointer"
          >
            {SUPPORTED_VOICE_LANGUAGES.map((lang) => (
              <option key={lang.code} value={lang.code} className="bg-slate-900 text-slate-200">
                {lang.nativeLabel} ({lang.label.split(' ')[0]})
              </option>
            ))}
          </select>
        </div>

        <span className="hidden sm:inline text-slate-500 text-[10px]">
          Press <kbd className="px-1 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">Enter</kbd> to send, <kbd className="px-1 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">Shift+Enter</kbd> for newline
        </span>
      </div>
    </div>
  );
};
