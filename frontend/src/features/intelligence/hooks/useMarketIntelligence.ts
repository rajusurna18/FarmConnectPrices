import { useQuery } from '@tanstack/react-query';
import { intelligenceService } from '../services/intelligenceService';
import type { IntelligenceFilterState } from '../types';
import { defaultRetry, queryKeys } from '../../../utils/queryConfig';

export function useMarketComparison(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: queryKeys.intelligenceCompare(filters),
    queryFn: () => intelligenceService.getMarketComparison(filters),
    staleTime: 1000 * 60 * 5,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    retry: defaultRetry,
  });
}

export function useMarketIntelligenceSummary(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: queryKeys.intelligenceSummary(filters),
    queryFn: () => intelligenceService.getSummary(filters),
    staleTime: 1000 * 60 * 5,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    retry: defaultRetry,
  });
}

export function usePriceTrends(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: queryKeys.intelligenceTrends(filters),
    queryFn: () => intelligenceService.getTrends(filters),
    staleTime: 1000 * 60 * 5,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    retry: defaultRetry,
  });
}
