import { apiClient } from './api';
import type { HealthResponse } from '../types/health';

export const fetchHealthStatus = async (): Promise<HealthResponse> => {
  const response = await apiClient.get<HealthResponse>('/api/v1/health');
  return response.data;
};
