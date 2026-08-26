import { apiClient } from '../../../services/api';
import type { MarketPriceSummary, MarketPriceDetail, MarketPriceFilterState } from '../types';

export const marketPriceService = {
  getMarketPrices: async (filters: MarketPriceFilterState = {}): Promise<MarketPriceSummary[]> => {
    const params: Record<string, string> = {};
    if (filters.marketId) params.marketId = filters.marketId;
    if (filters.cropId) params.cropId = filters.cropId;
    if (filters.priceDate) params.priceDate = filters.priceDate;
    if (filters.fromDate) params.fromDate = filters.fromDate;
    if (filters.toDate) params.toDate = filters.toDate;
    if (filters.qualityStatus) params.qualityStatus = filters.qualityStatus;
    if (filters.unit) params.unit = filters.unit;
    if (filters.state) params.state = filters.state;
    if (filters.district) params.district = filters.district;

    const res = await apiClient.get<MarketPriceSummary[]>('/api/v1/market-prices', { params });
    return res.data;
  },

  getLatestMarketPrice: async (marketId: string, cropId: string): Promise<MarketPriceDetail> => {
    const res = await apiClient.get<MarketPriceDetail>('/api/v1/market-prices/latest', {
      params: { marketId, cropId },
    });
    return res.data;
  },

  getMarketPriceHistory: async (
    marketId: string,
    cropId: string,
    fromDate?: string,
    toDate?: string
  ): Promise<MarketPriceDetail[]> => {
    const params: Record<string, string> = { marketId, cropId };
    if (fromDate) params.fromDate = fromDate;
    if (toDate) params.toDate = toDate;

    const res = await apiClient.get<MarketPriceDetail[]>('/api/v1/market-prices/history', { params });
    return res.data;
  },

  getMarketPriceById: async (priceId: string): Promise<MarketPriceDetail> => {
    const res = await apiClient.get<MarketPriceDetail>(`/api/v1/market-prices/${priceId}`);
    return res.data;
  },
};
