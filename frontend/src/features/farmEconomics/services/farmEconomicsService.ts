import { apiClient } from '../../../services/api';
import type {
  FarmEconomicRecordRequest,
  FarmEconomicRecordResponse,
  FarmProfitabilityEvaluationRequest,
  FarmProfitabilityEvaluationResponse,
  FarmMarketComparisonRequest,
  FarmMarketComparisonResponse,
} from '../types';

export const farmEconomicsService = {
  getEconomicRecords: async (): Promise<FarmEconomicRecordResponse[]> => {
    const response = await apiClient.get<FarmEconomicRecordResponse[]>('/api/v1/farm-economics');
    return response.data;
  },

  getEconomicRecordById: async (id: string): Promise<FarmEconomicRecordResponse> => {
    const response = await apiClient.get<FarmEconomicRecordResponse>(`/api/v1/farm-economics/${id}`);
    return response.data;
  },

  createEconomicRecord: async (payload: FarmEconomicRecordRequest): Promise<FarmEconomicRecordResponse> => {
    const response = await apiClient.post<FarmEconomicRecordResponse>('/api/v1/farm-economics', payload);
    return response.data;
  },

  updateEconomicRecord: async (id: string, payload: FarmEconomicRecordRequest): Promise<FarmEconomicRecordResponse> => {
    const response = await apiClient.put<FarmEconomicRecordResponse>(`/api/v1/farm-economics/${id}`, payload);
    return response.data;
  },

  deleteEconomicRecord: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/farm-economics/${id}`);
  },

  evaluateProfitability: async (payload: FarmProfitabilityEvaluationRequest): Promise<FarmProfitabilityEvaluationResponse> => {
    const response = await apiClient.post<FarmProfitabilityEvaluationResponse>('/api/v1/farm-economics/evaluate', payload);
    return response.data;
  },

  compareMarkets: async (payload: FarmMarketComparisonRequest): Promise<FarmMarketComparisonResponse> => {
    const response = await apiClient.post<FarmMarketComparisonResponse>('/api/v1/farm-economics/compare-markets', payload);
    return response.data;
  },
};
