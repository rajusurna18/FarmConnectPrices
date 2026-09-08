import { apiClient } from './api';

export interface SellingCostsInput {
  transportationCost?: number;
  otherSellingCosts?: number;
}

export interface SmartSellingDecisionRequest {
  cropId: string;
  quantity: number;
  quantityUnit: string;
  candidateMarketIds: string[];
  farmId?: string;
  economicRecordId?: string;
  customSellingCosts?: SellingCostsInput;
  priceBasis?: string;
  forecastHorizon?: string; // '1_DAY' | '3_DAYS' | '7_DAYS' | '14_DAYS'
}

export interface MarketSummary {
  id: string;
  name: string;
  code: string;
  type: string;
  state: string;
  district: string;
  mandal?: string;
  status: string;
}

export interface MarketCard {
  rank: number;
  market: MarketSummary;
  selectedPrice: number;
  priceUnit: string;
  priceDate: string;
  stalePrice: boolean;
  costSourceClassification: string;
  grossRevenue: number;
  totalSellingCost: number;
  estimatedNetRealization: number;
  netRealizationPerUnit: number;
  totalProductionCost?: number;
  estimatedNetProfit?: number;
  roi?: number;
  profitabilityStatus: string;
  trendDirection: string;
  trendDataQuality: string;
  decisionBasis: string;
}

export interface TradeOffItem {
  marketIdA: string;
  marketNameA: string;
  marketIdB: string;
  marketNameB: string;
  tradeOffType: string;
  title: string;
  description: string;
  calculatedConsequence: number;
}

export interface ForecastScenarioItem {
  marketId: string;
  marketName: string;
  horizon: string;
  direction: string;
  confidence: string;
  isScenario: boolean;
  currentVerifiedPrice: number;
  expectedPrice?: number;
  bearPrice?: number;
  bullPrice?: number;
  expectedNetRealization?: number;
  bearNetRealization?: number;
  bullNetRealization?: number;
}

export interface AiDecisionResponse {
  summary: string;
  recommendation: string;
  verifiedFacts: string[];
  reasoning: string[];
  risks: string[];
  nextSteps: string[];
  limitations: string[];
}

export interface SmartSellingDecisionResponse {
  status: string;
  message: string;
  crop: {
    id: string;
    name: string;
  };
  quantity: number;
  quantityUnit: string;
  priceBasis: string;
  recommendedMarketId: string;
  recommendationType: string;
  decisionBasis: string;
  overallDecisionConfidence: string;
  forecastConfidence: string;
  trendDataQuality: string;
  economicsCompleteness: string;
  primaryRecommendation: MarketCard;
  rankedMarkets: MarketCard[];
  unavailableMarkets: unknown[];

  tradeOffs: TradeOffItem[];
  forecastScenarios: ForecastScenarioItem[];
  aiExplanation?: AiDecisionResponse;
  risks: string[];
  nextSteps: string[];
  limitations: string[];
}

export const evaluateSmartSelling = async (
  request: SmartSellingDecisionRequest
): Promise<SmartSellingDecisionResponse> => {
  const response = await apiClient.post<SmartSellingDecisionResponse>(
    '/api/v1/smart-selling/evaluate',
    request
  );
  return response.data;
};
