import { useQuery } from '@tanstack/react-query';
import { marketPriceService } from '../services/marketPriceService';
import type { MarketPriceFilterState, MarketPriceSummary, MarketPriceDetail } from '../types';
import { defaultRetry, queryKeys } from '../../../utils/queryConfig';

export function useMarketPrices(filters: MarketPriceFilterState = {}) {
  return useQuery<MarketPriceSummary[], Error>({
    queryKey: queryKeys.marketPrices(filters),
    queryFn: () => marketPriceService.getMarketPrices(filters),
    staleTime: 1000 * 60 * 5, // 5 minutes
    retry: defaultRetry,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}

export function useMarketPrice(priceId: string | undefined) {
  return useQuery<MarketPriceDetail, Error>({
    queryKey: queryKeys.marketPriceDetail(priceId),
    queryFn: () => {
      if (!priceId) throw new Error('Price ID is required');
      return marketPriceService.getMarketPriceById(priceId);
    },
    enabled: Boolean(priceId && priceId.trim().length > 0),
    staleTime: 1000 * 60 * 5,
    retry: defaultRetry,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}

export function useLatestMarketPrice(marketId: string | undefined, cropId: string | undefined) {
  return useQuery<MarketPriceDetail, Error>({
    queryKey: ['latestMarketPrice', marketId || '', cropId || ''],
    queryFn: () => {
      if (!marketId || !cropId) throw new Error('Market ID and Crop ID are required');
      return marketPriceService.getLatestMarketPrice(marketId, cropId);
    },
    enabled: Boolean(marketId && cropId && marketId.trim().length > 0 && cropId.trim().length > 0),
    staleTime: 1000 * 60 * 5,
    retry: defaultRetry,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}

export function useMarketPriceHistory(
  marketId: string | undefined,
  cropId: string | undefined,
  fromDate?: string,
  toDate?: string
) {
  return useQuery<MarketPriceDetail[], Error>({
    queryKey: ['marketPriceHistory', marketId || '', cropId || '', fromDate || '', toDate || ''],
    queryFn: () => {
      if (!marketId || !cropId) throw new Error('Market ID and Crop ID are required');
      return marketPriceService.getMarketPriceHistory(marketId, cropId, fromDate, toDate);
    },
    enabled: Boolean(marketId && cropId && marketId.trim().length > 0 && cropId.trim().length > 0),
    staleTime: 1000 * 60 * 5,
    retry: defaultRetry,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}
