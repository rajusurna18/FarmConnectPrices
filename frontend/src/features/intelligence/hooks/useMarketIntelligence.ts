import { useQuery } from '@tanstack/react-query';
import { intelligenceService } from '../services/intelligenceService';
import type { IntelligenceFilterState } from '../types';

interface ApiError {
  response?: {
    status?: number;
  };
}

const defaultRetry = (failureCount: number, error: unknown) => {
  const apiError = error as ApiError;
  if (apiError?.response?.status === 503) return false;
  return failureCount < 1;
};

export function useMarketComparison(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: ['market-intelligence', 'compare', filters],
    queryFn: () => intelligenceService.getMarketComparison(filters),
    staleTime: 1000 * 60 * 5,
    refetchOnWindowFocus: false,
    retry: defaultRetry,
  });
}

export function useMarketIntelligenceSummary(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: ['market-intelligence', 'summary', filters],
    queryFn: () => intelligenceService.getSummary(filters),
    staleTime: 1000 * 60 * 5,
    refetchOnWindowFocus: false,
    retry: defaultRetry,
  });
}

export function usePriceTrends(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: ['market-intelligence', 'trends', filters],
    queryFn: () => intelligenceService.getTrends(filters),
    staleTime: 1000 * 60 * 5,
    refetchOnWindowFocus: false,
    retry: defaultRetry,
  });
}
