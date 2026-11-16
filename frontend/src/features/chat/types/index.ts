import type { VoiceLanguage } from '../../voice/types';

/**
 * Confidence band for router intent classification.
 */
export type ConfidenceBand = 'HIGH' | 'MEDIUM' | 'LOW';

/**
 * The 17 approved agricultural primary intents matching the backend AgriIntent enum.
 */
export type AgriIntent =
  | 'CROP_SUITABILITY'
  | 'CROP_SELECTION'
  | 'SOIL_MANAGEMENT'
  | 'FERTILIZER_NUTRIENT'
  | 'IRRIGATION_MANAGEMENT'
  | 'PEST_MANAGEMENT'
  | 'PLANT_DISEASE_INQUIRY'
  | 'CROP_CULTIVATION_PRACTICES'
  | 'CROP_ROTATION'
  | 'HARVEST_POST_HARVEST'
  | 'WEATHER_INQUIRY'
  | 'MARKET_PRICE_INQUIRY'
  | 'MARKET_TREND_INQUIRY'
  | 'FARM_ECONOMICS_INQUIRY'
  | 'MARKETPLACE_DISCOVERY'
  | 'GENERAL_AGRI_KNOWLEDGE'
  | 'UNSUPPORTED_OR_UNCLEAR';

/**
 * Entities extracted from the user's natural language agricultural query.
 */
export interface ExtractedEntitiesDto {
  crops: string[];
  locations: string[];
  mandis: string[];
  quantities: string[];
  attributes: Record<string, unknown>;
}

/**
 * Clarification prompt details when the intent is ambiguous or missing required entities.
 */
export interface ClarificationDetails {
  required: boolean;
  prompt: string | null;
  candidateIntents: AgriIntent[];
  missingEntities: string[];
}

/**
 * Downstream orchestration payload synthesized from grounded domain services.
 */
export interface OrchestrationPayload {
  synthesizedText: string;
  fallback: boolean;
  warnings: string[];
  domainData: Record<string, unknown>;
}

/**
 * Complete turn response returned by the backend router controller.
 */
export interface RouterTurnResponse {
  sessionId: string;
  turnIndex: number;
  intent: AgriIntent;
  confidence: number;
  confidenceBand: ConfidenceBand;
  entities: ExtractedEntitiesDto;
  clarification: ClarificationDetails;
  authorized: boolean;
  cached: boolean;
  orchestration: OrchestrationPayload | null;
}

/**
 * Request payload for POST /api/v1/router/session
 */
export interface SessionInitRequest {
  metadata?: string | null;
}

/**
 * Response payload from POST /api/v1/router/session (201 Created)
 */
export interface SessionInitResponse {
  sessionId: string;
  status: string;
  ttlSeconds: number;
}

/**
 * Request payload for POST /api/v1/router/turn
 * Note: targetOwnerUid and fingerprint are omitted.
 */
export interface RouterTurnRequest {
  sessionId: string;
  turnIndex: number;
  query: string;
}

/**
 * Sender of a message in the conversational UI.
 */
export type ChatMessageSender = 'user' | 'assistant' | 'system';

/**
 * Individual chat message in the conversation stream.
 */
export interface ChatMessage {
  id: string;
  sender: ChatMessageSender;
  text: string;
  timestamp: number;
  turnIndex?: number;
  status: 'sending' | 'committed' | 'error';
  turnResponse?: RouterTurnResponse | null;
  error?: string | null;
  isRetryable?: boolean;
}

/**
 * Client-side structured chat error.
 */
export interface ChatError {
  status: number;
  message: string;
  isRetryable: boolean;
  action?: 'retry' | 'reinit' | 'cooldown' | 'edit';
}

/**
 * State of the conversational chat session managed in React memory.
 */
export interface ChatSessionState {
  sessionId: string | null;
  status: 'uninitialized' | 'initializing' | 'ready' | 'locked' | 'error';
  nextTurnIndex: number; // starts at 1
  messages: ChatMessage[];
  isProcessing: boolean;
  pendingRequest: RouterTurnRequest | null;
  error: ChatError | null;
  language: VoiceLanguage;
}
