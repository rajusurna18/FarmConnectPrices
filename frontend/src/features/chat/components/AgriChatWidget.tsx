import React, { useState, useRef, useEffect } from 'react';
import {
  MessageSquare,
  RotateCcw,
  Bot,
  Sparkles,
  TrendingUp,
  Bug,
  Sprout,
  DollarSign,
  ChevronDown,
} from 'lucide-react';
import { useAuth } from '../../auth/hooks/useAuth';
import { useAgriChatbot } from '../hooks/useAgriChatbot';
import { ChatMessageBubble } from './ChatMessageBubble';
import { ChatInputArea } from './ChatInputArea';
import type { AgriIntent, ChatMessage } from '../types';

const SUGGESTED_PROMPTS = [
  {
    icon: TrendingUp,
    label: 'Tomato mandi prices',
    prompt: 'What are the current tomato modal prices in Kolar market?',
  },
  {
    icon: Bug,
    label: 'Chilli leaf curl remedies',
    prompt: 'What are safe organic remedies for leaf curl virus in chilli?',
  },
  {
    icon: Sprout,
    label: 'Fertilizer for wheat',
    prompt: 'What is the recommended fertilizer schedule for wheat in sandy loam soil?',
  },
  {
    icon: DollarSign,
    label: 'My farm economics',
    prompt: 'Show me my current farm economics and profitability overview.',
  },
];

export const AgriChatWidget: React.FC = () => {
  const { isAuthenticated, currentUser } = useAuth();
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [stagedQuery, setStagedQuery] = useState<string>('');
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  const {
    sessionState,
    sendMessage,
    retryLastTurn,
    resetSession,
    setLanguage,
    cooldownSeconds,
  } = useAgriChatbot();

  // Scroll to bottom whenever messages change or processing starts
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [sessionState.messages, sessionState.isProcessing, isOpen]);

  // Auth Guard: Never render chatbot interface for unauthenticated callers
  if (!isAuthenticated || !currentUser) {
    return null;
  }

  const handlePromptClick = (prompt: string) => {
    sendMessage(prompt);
  };

  /**
   * Refines clarification intent by associating it with the original user query
   * that produced this turn, preserving crops, mandis, and context, then staging
   * it into the input area for normal user submission.
   */
  const handleSelectCandidateIntent = (intent: AgriIntent, assistantMsg: ChatMessage) => {
    const originalUserMsg = sessionState.messages.find(
      (m) => m.sender === 'user' && m.turnIndex === assistantMsg.turnIndex
    );
    const originalQuery = originalUserMsg?.text?.trim() || '';
    const intentLabel = intent.toLowerCase().replace(/_/g, ' ');

    const contextualQuery = originalQuery
      ? `${originalQuery} (Focus: ${intentLabel})`
      : `Please provide guidance for ${intentLabel}.`;

    setStagedQuery(contextualQuery);
  };

  return (
    <>
      {/* Floating Action Button (FAB) */}
      {!isOpen && (
        <button
          type="button"
          onClick={() => setIsOpen(true)}
          aria-label="Open FarmLink AI Chat"
          aria-expanded={false}
          className="fixed bottom-5 right-5 z-40 flex items-center gap-2.5 px-4 py-3 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-medium rounded-full shadow-2xl shadow-emerald-950/50 border border-emerald-400/30 transition-all duration-300 hover:scale-105 active:scale-95 group focus:outline-none focus:ring-2 focus:ring-emerald-400/50"
        >
          <div className="relative">
            <span className="absolute -top-0.5 -right-0.5 flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-300 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-400"></span>
            </span>
            <MessageSquare className="w-5 h-5 transition-transform group-hover:scale-110" />
          </div>
          <span className="text-sm font-semibold tracking-wide">FarmLink AI</span>
        </button>
      )}

      {/* Expanded Chat Widget Dialog */}
      {isOpen && (
        <div
          role="dialog"
          aria-label="FarmLink AI Assistant"
          className="fixed bottom-4 right-4 sm:bottom-6 sm:right-6 z-50 flex flex-col w-[calc(100vw-2rem)] sm:w-[410px] h-[580px] max-h-[85vh] bg-slate-950 border border-slate-800 rounded-2xl shadow-2xl shadow-black/80 overflow-hidden transition-all duration-200 animate-in fade-in zoom-in-95"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-slate-900 border-b border-slate-800 text-white shrink-0">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-emerald-600 to-teal-500 flex items-center justify-center border border-emerald-400/30 shadow-sm">
                <Bot className="w-4 h-4 text-white" />
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <h3 className="font-semibold text-sm text-slate-100">FarmLink AI</h3>
                  <span className="px-1.5 py-0.2 rounded text-[10px] font-medium bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    Online
                  </span>
                </div>
                <p className="text-[11px] text-slate-400">
                  {sessionState.sessionId
                    ? `Session Active • Turn ${sessionState.nextTurnIndex - 1}`
                    : 'Agricultural Advisor'}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-1">
              {/* Reset Session Button */}
              <button
                type="button"
                onClick={resetSession}
                title="Reset session"
                aria-label="Reset chat session"
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors focus:outline-none focus:ring-1 focus:ring-emerald-500/50"
              >
                <RotateCcw className="w-4 h-4" />
              </button>

              {/* Minimize / Close Button */}
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                title="Minimize chat"
                aria-label="Minimize chat window"
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors focus:outline-none focus:ring-1 focus:ring-emerald-500/50"
              >
                <ChevronDown className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Chat Stream Body */}
          <div className="flex-1 p-4 overflow-y-auto space-y-3 bg-gradient-to-b from-slate-950 via-slate-950 to-slate-900/40">
            {sessionState.messages.length === 0 ? (
              <div className="h-full flex flex-col justify-center items-center text-center px-3 py-6 select-none">
                <div className="w-12 h-12 rounded-2xl bg-emerald-950/60 border border-emerald-500/40 flex items-center justify-center mb-3 shadow-lg shadow-emerald-950/30">
                  <Sparkles className="w-6 h-6 text-emerald-400" />
                </div>
                <h4 className="font-semibold text-base text-slate-100 mb-1">
                  How can I help your farm today?
                </h4>
                <p className="text-xs text-slate-400 max-w-xs mb-5 leading-relaxed">
                  Ask me about crop diseases, fertilizers, market prices, mandi trends, or farm economics.
                </p>

                {/* Quick Suggestion Chips */}
                <div className="w-full space-y-2 max-w-xs">
                  {SUGGESTED_PROMPTS.map((item, idx) => {
                    const Icon = item.icon;
                    return (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => handlePromptClick(item.prompt)}
                        className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl bg-slate-900/90 hover:bg-slate-800 border border-slate-800 hover:border-emerald-500/40 text-left transition-all text-xs text-slate-300 group"
                      >
                        <div className="w-6 h-6 rounded-lg bg-slate-800 group-hover:bg-emerald-950/60 border border-slate-700/60 group-hover:border-emerald-500/30 flex items-center justify-center shrink-0">
                          <Icon className="w-3.5 h-3.5 text-emerald-400" />
                        </div>
                        <span className="truncate group-hover:text-emerald-200 transition-colors">
                          {item.label}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : (
              sessionState.messages.map((msg) => (
                <ChatMessageBubble
                  key={msg.id}
                  message={msg}
                  language={sessionState.language}
                  onRetry={retryLastTurn}
                  onAskAgain={(query) => sendMessage(query)}
                  onSelectCandidateIntent={(intent) => handleSelectCandidateIntent(intent, msg)}
                />
              ))
            )}

            {/* Processing Indicator */}
            {sessionState.isProcessing && (
              <div className="flex justify-start mb-4">
                <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 rounded-2xl rounded-tl-none px-4 py-2.5 shadow-md">
                  <div className="w-5 h-5 rounded-full bg-emerald-600/30 flex items-center justify-center">
                    <Bot className="w-3 h-3 text-emerald-400 animate-pulse" />
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs text-slate-400">Consulting agricultural models</span>
                    <span className="flex gap-1 items-center ml-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-bounce [animation-delay:-0.3s]"></span>
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-bounce [animation-delay:-0.15s]"></span>
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-bounce"></span>
                    </span>
                  </div>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Footer Input Area */}
          <ChatInputArea
            onSendMessage={sendMessage}
            isProcessing={sessionState.isProcessing}
            cooldownSeconds={cooldownSeconds}
            language={sessionState.language}
            onLanguageChange={setLanguage}
            stagedQuery={stagedQuery}
            onStagedQueryConsumed={() => setStagedQuery('')}
          />
        </div>
      )}
    </>
  );
};
