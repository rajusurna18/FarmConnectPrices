import { useQuery } from '@tanstack/react-query';
import { marketService } from '../services/marketService';
import type { MarketFilterState } from '../types';

export const useMarkets = (filters?: MarketFilterState) => {
  return useQuery({
    queryKey: ['markets', filters],
    queryFn: () => marketService.getMarkets(filters),
    staleTime: 1000 * 60 * 30,
    retry: 1,
  });
};

export const useMarket = (marketId?: string) => {
  return useQuery({
    queryKey: ['market', marketId],
    queryFn: () => marketService.getMarketById(marketId!),
    enabled: Boolean(marketId && marketId.trim().length > 0),
    staleTime: 1000 * 60 * 30,
    retry: 1,
  });
};

export const useMarketCrops = (marketId?: string) => {
  return useQuery({
    queryKey: ['marketCrops', marketId],
    queryFn: () => marketService.getMarketCrops(marketId!),
    enabled: Boolean(marketId && marketId.trim().length > 0),
    staleTime: 1000 * 60 * 30,
    retry: 1,
  });
};

