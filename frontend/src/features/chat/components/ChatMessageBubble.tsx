import React from 'react';
import {
  Bot,
  User,
  AlertTriangle,
  RotateCcw,
  Sparkles,
  HelpCircle,
  Clock,
  RefreshCw,
  Info,
} from 'lucide-react';
import { AudioNarrationButton } from '../../voice/components/AudioNarrationButton';
import type { VoiceLanguage } from '../../voice/types';
import { SafeMarkdownRenderer } from './SafeMarkdownRenderer';
import type {
  ChatMessage,
  AgriIntent,
  ConfidenceBand,
} from '../types';

export interface ChatMessageBubbleProps {
  message: ChatMessage;
  language?: VoiceLanguage;
  onRetry?: () => void;
  onAskAgain?: (query: string) => void;
  onSelectCandidateIntent?: (intent: AgriIntent) => void;
}

const INTENT_LABELS: Record<AgriIntent, string> = {
  CROP_SUITABILITY: 'Crop Suitability',
  CROP_SELECTION: 'Crop Selection',
  SOIL_MANAGEMENT: 'Soil Management',
  FERTILIZER_NUTRIENT: 'Fertilizers & Nutrients',
  IRRIGATION_MANAGEMENT: 'Irrigation Management',
  PEST_MANAGEMENT: 'Pest Management',
  PLANT_DISEASE_INQUIRY: 'Plant Disease',
  CROP_CULTIVATION_PRACTICES: 'Cultivation Practices',
  CROP_ROTATION: 'Crop Rotation',
  HARVEST_POST_HARVEST: 'Harvest & Post-Harvest',
  WEATHER_INQUIRY: 'Weather Inquiry',
  MARKET_PRICE_INQUIRY: 'Market Price',
  MARKET_TREND_INQUIRY: 'Market Trend',
  FARM_ECONOMICS_INQUIRY: 'Farm Economics',
  MARKETPLACE_DISCOVERY: 'Marketplace',
  GENERAL_AGRI_KNOWLEDGE: 'Agri Knowledge',
  UNSUPPORTED_OR_UNCLEAR: 'Advisory Mode',
};

function formatKey(key: string): string {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/_/g, ' ')
    .trim()
    .replace(/^\w/, (c) => c.toUpperCase());
}

export const ChatMessageBubble: React.FC<ChatMessageBubbleProps> = ({
  message,
  language = 'en-IN',
  onRetry,
  onAskAgain,
  onSelectCandidateIntent,
}) => {
  const isUser = message.sender === 'user';
  const turnResp = message.turnResponse;
  const orchestration = turnResp?.orchestration;
  const clarification = turnResp?.clarification;
  const isFallback = orchestration?.fallback;

  // Confidence badge metadata
  const getConfidenceBadge = (band?: ConfidenceBand) => {
    switch (band) {
      case 'HIGH':
        return {
          bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
          label: 'High Confidence',
        };
      case 'MEDIUM':
        return {
          bg: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
          label: 'Medium Confidence',
        };
      case 'LOW':
        return {
          bg: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
          label: 'Low Confidence',
        };
      default:
        return null;
    }
  };

  const confidenceBadge = turnResp ? getConfidenceBadge(turnResp.confidenceBand) : null;
  const intentLabel = turnResp?.intent ? INTENT_LABELS[turnResp.intent] || turnResp.intent : null;

  // Format timestamp
  const timeString = new Date(message.timestamp).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
  });

  if (isUser) {
    return (
      <div className="flex justify-end mb-4 group select-text">
        <div className="flex flex-col items-end max-w-[85%]">
          <div className="flex items-start gap-2">
            <div className="bg-emerald-600 text-white rounded-2xl rounded-tr-none px-4 py-2.5 shadow-md shadow-emerald-950/20 text-sm leading-relaxed break-words">
              {message.text}
            </div>
            <div className="w-7 h-7 rounded-full bg-emerald-700/80 flex items-center justify-center shrink-0 mt-0.5 border border-emerald-500/40">
              <User className="w-3.5 h-3.5 text-emerald-100" />
            </div>
          </div>

          <div className="flex items-center gap-2 mt-1 text-[10px] text-slate-500 pr-9">
            {message.turnIndex && <span>Turn #{message.turnIndex}</span>}
            <span>{timeString}</span>
            {message.status === 'sending' && (
              <span className="flex items-center gap-1 text-emerald-400">
                <RefreshCw className="w-2.5 h-2.5 animate-spin" />
                <span>Sending</span>
              </span>
            )}
            {message.status === 'error' && (
              <span className="flex items-center gap-1 text-rose-400 font-medium">
                <AlertTriangle className="w-2.5 h-2.5" />
                <span>Failed</span>
              </span>
            )}
          </div>
        </div>
      </div>
    );
  }

  // Assistant Message
  const textToNarrate = orchestration?.synthesizedText || message.text;

  return (
    <div className="flex justify-start mb-4 group select-text">
      <div className="flex flex-col items-start max-w-[92%] sm:max-w-[85%]">
        <div className="flex items-start gap-2 w-full">
          {/* Avatar */}
          <div className="w-7 h-7 rounded-full bg-gradient-to-br from-emerald-600 to-teal-700 flex items-center justify-center shrink-0 mt-0.5 shadow-md shadow-emerald-950/30 border border-emerald-400/30">
            <Bot className="w-3.5 h-3.5 text-white" />
          </div>

          {/* Main Bubble Card */}
          <div className="flex-1 bg-slate-900 border border-slate-800 rounded-2xl rounded-tl-none p-3.5 sm:p-4 text-slate-100 shadow-xl shadow-slate-950/40">
            {/* Header: Intent, Confidence, Fallback badges */}
            {(intentLabel || confidenceBadge || isFallback) && (
              <div className="flex flex-wrap items-center gap-1.5 mb-2.5 pb-2 border-b border-slate-800/80">
                {intentLabel && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium bg-emerald-950/60 text-emerald-300 border border-emerald-500/30">
                    <Sparkles className="w-3 h-3 text-emerald-400" />
                    <span>{intentLabel}</span>
                  </span>
                )}

                {confidenceBadge && (
                  <span
                    className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold border ${confidenceBadge.bg}`}
                  >
                    {confidenceBadge.label}
                  </span>
                )}

                {isFallback && (
                  <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-amber-950/60 text-amber-300 border border-amber-500/30">
                    <Info className="w-2.5 h-2.5" />
                    <span>General Advisory</span>
                  </span>
                )}
              </div>
            )}

            {/* Synthesized Response Body */}
            <div className="text-sm leading-relaxed text-slate-200">
              <SafeMarkdownRenderer content={textToNarrate} />
            </div>

            {/* Domain Data Preview Chips */}
            {orchestration?.domainData && Object.keys(orchestration.domainData).length > 0 && (
              <div className="mt-3 pt-2.5 border-t border-slate-800/80">
                <span className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold mb-1.5 block">
                  Grounding Data Facts
                </span>
                <div className="flex flex-wrap gap-1.5">
                  {Object.entries(orchestration.domainData).map(([k, val]) => {
                    if (val === null || val === undefined || typeof val === 'object') return null;
                    return (
                      <div
                        key={k}
                        className="inline-flex items-center gap-1 px-2 py-1 rounded bg-slate-800/80 border border-slate-700/60 text-[11px]"
                      >
                        <span className="text-slate-400">{formatKey(k)}:</span>
                        <span className="text-emerald-300 font-medium font-mono">{String(val)}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Warnings Alert Box */}
            {orchestration?.warnings && orchestration.warnings.length > 0 && (
              <div className="mt-3 p-2.5 rounded-lg bg-amber-950/40 border border-amber-500/40 text-amber-300 text-xs flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <div className="font-semibold text-amber-200">Advisory Notice:</div>
                  <ul className="list-disc list-inside space-y-0.5 text-amber-300/90 text-[11px]">
                    {orchestration.warnings.map((warn, i) => (
                      <li key={i}>{warn}</li>
                    ))}
                  </ul>
                </div>
              </div>
            )}

            {/* Clarification Prompt & Candidate Intent Chips */}
            {clarification?.required && (
              <div className="mt-3 p-2.5 rounded-lg bg-emerald-950/30 border border-emerald-500/30 text-xs">
                <div className="flex items-center gap-1.5 text-emerald-300 font-semibold mb-2">
                  <HelpCircle className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Clarification Needed</span>
                </div>
                {clarification.prompt && (
                  <p className="text-slate-300 mb-2 leading-relaxed">{clarification.prompt}</p>
                )}
                {clarification.candidateIntents && clarification.candidateIntents.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {clarification.candidateIntents.map((candIntent) => (
                      <button
                        key={candIntent}
                        type="button"
                        onClick={() => onSelectCandidateIntent?.(candIntent)}
                        className="px-2.5 py-1 rounded-md bg-slate-800 hover:bg-emerald-900/60 border border-slate-700 hover:border-emerald-500/50 text-[11px] font-medium text-emerald-300 transition-colors"
                      >
                        {INTENT_LABELS[candIntent] || candIntent}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Error Banner with Remediation Actions */}
            {message.error && (
              <div className="mt-3 p-2.5 rounded-lg bg-rose-950/50 border border-rose-500/40 text-rose-300 text-xs space-y-2">
                <div className="flex items-center gap-1.5 font-semibold text-rose-200">
                  <AlertTriangle className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                  <span>{message.error}</span>
                </div>

                <div className="flex items-center gap-2 pt-1">
                  {message.isRetryable && onRetry && (
                    <button
                      type="button"
                      onClick={onRetry}
                      className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-rose-900/60 hover:bg-rose-900 border border-rose-500/50 text-[11px] font-semibold text-rose-200 transition-colors"
                    >
                      <RotateCcw className="w-3 h-3" />
                      <span>Retry</span>
                    </button>
                  )}

                  {onAskAgain && (
                    <button
                      type="button"
                      onClick={() => onAskAgain(message.text)}
                      className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 border border-slate-600 text-[11px] font-semibold text-slate-200 transition-colors"
                    >
                      <RefreshCw className="w-3 h-3" />
                      <span>Ask Again as New Turn</span>
                    </button>
                  )}
                </div>
              </div>
            )}

            {/* Audio Narration and Timestamp Row */}
            <div className="flex items-center justify-between gap-2 mt-3 pt-2 border-t border-slate-800/80">
              <AudioNarrationButton
                text={textToNarrate}
                language={language}
                size="sm"
                label="Listen"
              />

              <div className="flex items-center gap-1 text-[10px] text-slate-500 font-mono">
                <Clock className="w-2.5 h-2.5" />
                <span>{timeString}</span>
                {message.turnIndex && <span>• Turn #{message.turnIndex}</span>}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
