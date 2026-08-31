import { apiClient } from '../../../services/api';
import type {
  MarketEvaluationRequest,
  MarketEvaluationResponse,
  MarketComparisonRequest,
  MarketProfitabilityComparisonResponse
} from '../types/decisionSupport';

export const evaluateMarket = async (
  request: MarketEvaluationRequest
): Promise<MarketEvaluationResponse> => {
  const response = await apiClient.post<MarketEvaluationResponse>(
    '/api/v1/decision-support/evaluate',
    request
  );
  return response.data;
};

export const compareMarkets = async (
  request: MarketComparisonRequest
): Promise<MarketProfitabilityComparisonResponse> => {
  const response = await apiClient.post<MarketProfitabilityComparisonResponse>(
    '/api/v1/decision-support/compare-markets',
    request
  );
  return response.data;
};
