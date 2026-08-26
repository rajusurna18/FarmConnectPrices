import { apiClient } from '../../../services/api';
import type { MarketComparisonData, MarketIntelligenceSummaryData, PriceTrendData, IntelligenceFilterState } from '../types';

export const intelligenceService = {
  getMarketComparison: async (filters: IntelligenceFilterState): Promise<MarketComparisonData> => {
    const params: Record<string, string> = {};
    if (filters.cropId) params.cropId = filters.cropId;
    if (filters.state) params.state = filters.state;
    if (filters.district) params.district = filters.district;
    if (filters.unit) params.unit = filters.unit;
    if (filters.fromDate) params.fromDate = filters.fromDate;
    if (filters.toDate) params.toDate = filters.toDate;

    const res = await apiClient.get<MarketComparisonData>('/api/v1/market-intelligence/compare', { params });
    return res.data;
  },

  getSummary: async (filters: IntelligenceFilterState): Promise<MarketIntelligenceSummaryData> => {
    const params: Record<string, string> = {};
    if (filters.cropId) params.cropId = filters.cropId;
    if (filters.state) params.state = filters.state;
    if (filters.district) params.district = filters.district;
    if (filters.unit) params.unit = filters.unit;
    if (filters.fromDate) params.fromDate = filters.fromDate;
    if (filters.toDate) params.toDate = filters.toDate;

    const res = await apiClient.get<MarketIntelligenceSummaryData>('/api/v1/market-intelligence/summary', { params });
    return res.data;
  },

  getTrends: async (filters: IntelligenceFilterState): Promise<PriceTrendData> => {
    const params: Record<string, string> = {};
    if (filters.cropId) params.cropId = filters.cropId;
    if (filters.unit) params.unit = filters.unit;
    if (filters.fromDate) params.fromDate = filters.fromDate;
    if (filters.toDate) params.toDate = filters.toDate;

    const res = await apiClient.get<PriceTrendData>('/api/v1/market-intelligence/trends', { params });
    return res.data;
  }
};
