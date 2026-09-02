import { apiClient } from '../../../services/api';
import type { AiDecisionRequest, AiDecisionResponse } from '../types';

export const aiDecisionService = {
  processDecision: async (payload: AiDecisionRequest): Promise<AiDecisionResponse> => {
    const response = await apiClient.post<AiDecisionResponse>('/api/v1/ai/decisions', payload);
    return response.data;
  },
};
