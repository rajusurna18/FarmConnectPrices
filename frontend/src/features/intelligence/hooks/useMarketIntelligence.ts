import { useQuery } from '@tanstack/react-query';
import { intelligenceService } from '../services/intelligenceService';
import type { IntelligenceFilterState } from '../types';

export function useMarketComparison(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: ['market-intelligence', 'compare', filters],
    queryFn: () => intelligenceService.getMarketComparison(filters),
    staleTime: 1000 * 60 * 5,
  });
}

export function useMarketIntelligenceSummary(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: ['market-intelligence', 'summary', filters],
    queryFn: () => intelligenceService.getSummary(filters),
    staleTime: 1000 * 60 * 5,
  });
}

export function usePriceTrends(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: ['market-intelligence', 'trends', filters],
    queryFn: () => intelligenceService.getTrends(filters),
    staleTime: 1000 * 60 * 5,
  });
}
