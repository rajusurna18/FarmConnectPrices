import { apiClient } from '../../../services/api';
import type { Market, MarketSummary, MarketCrop, MarketFilterState } from '../types';

export const marketService = {
  getMarkets: async (filters?: MarketFilterState): Promise<MarketSummary[]> => {
    const params = new URLSearchParams();
    if (filters?.state) params.append('state', filters.state);
    if (filters?.district) params.append('district', filters.district);
    if (filters?.type) params.append('type', filters.type);
    if (filters?.cropId) params.append('cropId', filters.cropId);

    const queryString = params.toString();
    const url = queryString ? `/markets?${queryString}` : '/markets';
    const response = await apiClient.get<MarketSummary[]>(url);
    return response.data;
  },

  getMarketById: async (marketId: string): Promise<Market> => {
    const response = await apiClient.get<Market>(`/markets/${marketId}`);
    return response.data;
  },

  getMarketCrops: async (marketId: string): Promise<MarketCrop[]> => {
    const response = await apiClient.get<MarketCrop[]>(`/markets/${marketId}/crops`);
    return response.data;
  },
};
