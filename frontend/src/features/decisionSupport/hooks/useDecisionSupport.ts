import { useMutation } from '@tanstack/react-query';
import { evaluateMarket, compareMarkets } from '../services/decisionSupportApi';
import type {
  MarketEvaluationRequest,
  MarketEvaluationResponse,
  MarketComparisonRequest,
  MarketProfitabilityComparisonResponse
} from '../types/decisionSupport';

export const useEvaluateMarket = () => {
  return useMutation<MarketEvaluationResponse, Error, MarketEvaluationRequest>({
    mutationFn: evaluateMarket,
  });
};

export const useCompareMarkets = () => {
  return useMutation<MarketProfitabilityComparisonResponse, Error, MarketComparisonRequest>({
    mutationFn: compareMarkets,
  });
};
