export type AiDecisionType =
  | 'MARKET_SELECTION'
  | 'PROFITABILITY_EXPLANATION'
  | 'MARKET_COMPARISON_EXPLANATION'
  | 'SELLING_DECISION_SUPPORT';

export type AiConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW';

export interface AiDataFreshness {
  marketPriceDate?: string;
  status: 'CURRENT' | 'STALE' | 'MISSING';
  lastUpdatedIso?: string;
  stalePrice: boolean;
  staleMessage?: string;
}

export interface AiCalculatedMetrics {
  selectedPrice?: number;
  expectedYield?: number;
  yieldUnit?: string;
  grossRevenue?: number;
  totalProductionCost?: number;
  totalSellingCost?: number;
  totalCost?: number;
  estimatedNetRealization?: number;
  estimatedProfit?: number;
  profitPerUnit?: number;
  productionCostPerUnit?: number;
  totalCostPerUnit?: number;
  breakEvenSellingPrice?: number;
  roi?: number;
  profitabilityStatus?: string;
}

export interface AiDecisionRequest {
  decisionType: AiDecisionType;
  cropId?: string;
  farmId?: string;
  economicRecordId?: string;
  marketIds?: string[];
  quantity?: number;
  quantityUnit?: string;
  priceBasis?: 'MIN' | 'MODAL' | 'MAX';
  priceMode?: 'EXACT_DATE' | 'LATEST_AVAILABLE';
  date?: string;
  transportationCost?: number;
  otherSellingCosts?: number;
}

export interface AiDecisionResponse {
  decisionType: AiDecisionType;
  summary: string;
  recommendation?: string;
  confidence: AiConfidenceLevel;
  verifiedFacts: string[];
  calculatedMetrics?: AiCalculatedMetrics;
  reasoning: string[];
  risks: string[];
  nextSteps: string[];
  engine: string;
  aiGenerated: boolean;
  dataFreshness?: AiDataFreshness;
  limitations: string[];
}
