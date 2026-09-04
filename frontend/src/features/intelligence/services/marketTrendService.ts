import { apiClient } from '../../../services/api';
import type { MarketTrendData, IntelligenceFilterState } from '../types';

export const marketTrendService = {
  getMarketTrends: async (filters: IntelligenceFilterState): Promise<MarketTrendData> => {
    const params: Record<string, string | boolean> = {};
    if (filters.cropId) params.cropId = filters.cropId;
    if (filters.marketId) params.marketId = filters.marketId;
    if (filters.period) params.period = filters.period;
    if (filters.startDate) params.startDate = filters.startDate;
    if (filters.endDate) params.endDate = filters.endDate;
    if (filters.fromDate) params.fromDate = filters.fromDate;
    if (filters.toDate) params.toDate = filters.toDate;
    if (filters.unit) params.unit = filters.unit;
    if (filters.includeAiExplanation !== undefined) {
      params.includeAiExplanation = filters.includeAiExplanation;
    }

    const res = await apiClient.get<MarketTrendData>('/api/v1/market-intelligence/trends', { params });
    return res.data;
  }
};
