import { useQuery } from '@tanstack/react-query';
import { marketService } from '../services/marketService';
import type { MarketFilterState } from '../types';
import { defaultRetry, queryKeys } from '../../../utils/queryConfig';

export const useMarkets = (filters?: MarketFilterState) => {
  return useQuery({
    queryKey: queryKeys.markets(filters?.state, filters?.district, filters?.mandal, filters?.type),
    queryFn: () => marketService.getMarkets(filters),
    staleTime: 1000 * 60 * 30,
    retry: defaultRetry,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
};

export const useMarket = (marketId?: string) => {
  return useQuery({
    queryKey: queryKeys.market(marketId),
    queryFn: () => marketService.getMarketById(marketId!),
    enabled: Boolean(marketId && marketId.trim().length > 0),
    staleTime: 1000 * 60 * 30,
    retry: defaultRetry,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
};

export const useMarketCrops = (marketId?: string) => {
  return useQuery({
    queryKey: queryKeys.marketCrops(marketId),
    queryFn: () => marketService.getMarketCrops(marketId!),
    enabled: Boolean(marketId && marketId.trim().length > 0),
    staleTime: 1000 * 60 * 30,
    retry: defaultRetry,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
};
