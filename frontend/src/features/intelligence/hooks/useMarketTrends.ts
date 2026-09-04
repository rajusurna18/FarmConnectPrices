import { useQuery } from '@tanstack/react-query';
import { marketTrendService } from '../services/marketTrendService';
import type { IntelligenceFilterState } from '../types';
import { defaultRetry } from '../../../utils/queryConfig';

export function useMarketTrends(filters: IntelligenceFilterState) {
  return useQuery({
    queryKey: ['marketTrends', filters],
    queryFn: () => marketTrendService.getMarketTrends(filters),
    staleTime: 1000 * 60 * 5,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    retry: defaultRetry,
  });
}
