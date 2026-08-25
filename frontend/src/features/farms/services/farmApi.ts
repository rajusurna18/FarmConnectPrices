import { apiClient } from '../../../services/api';
import type { Farm, CreateFarmInput, FarmCrop, CreateFarmCropInput, CropMaster, LocationMaster } from '../types/farmTypes';

export const farmApi = {
  // Farms
  getFarms: async (): Promise<Farm[]> => {
    const response = await apiClient.get<Farm[]>('/api/v1/farms');
    return response.data;
  },

  getFarmById: async (farmId: string): Promise<Farm> => {
    const response = await apiClient.get<Farm>(`/api/v1/farms/${farmId}`);
    return response.data;
  },

  createFarm: async (input: CreateFarmInput): Promise<Farm> => {
    const response = await apiClient.post<Farm>('/api/v1/farms', input);
    return response.data;
  },

  updateFarm: async (farmId: string, input: CreateFarmInput): Promise<Farm> => {
    const response = await apiClient.put<Farm>(`/api/v1/farms/${farmId}`, input);
    return response.data;
  },

  deleteFarm: async (farmId: string): Promise<void> => {
    await apiClient.delete(`/api/v1/farms/${farmId}`);
  },

  // Farm Crops
  getFarmCrops: async (farmId: string): Promise<FarmCrop[]> => {
    const response = await apiClient.get<FarmCrop[]>(`/api/v1/farms/${farmId}/crops`);
    return response.data;
  },

  addFarmCrop: async (farmId: string, input: CreateFarmCropInput): Promise<FarmCrop> => {
    const response = await apiClient.post<FarmCrop>(`/api/v1/farms/${farmId}/crops`, input);
    return response.data;
  },

  updateFarmCrop: async (farmId: string, farmCropId: string, input: Partial<CreateFarmCropInput>): Promise<FarmCrop> => {
    const response = await apiClient.put<FarmCrop>(`/api/v1/farms/${farmId}/crops/${farmCropId}`, input);
    return response.data;
  },

  deleteFarmCrop: async (farmId: string, farmCropId: string): Promise<void> => {
    await apiClient.delete(`/api/v1/farms/${farmId}/crops/${farmCropId}`);
  },

  // Reference Masters
  getCrops: async (): Promise<CropMaster[]> => {
    const response = await apiClient.get<CropMaster[]>('/api/v1/crops');
    return response.data;
  },

  getLocations: async (): Promise<LocationMaster[]> => {
    const response = await apiClient.get<LocationMaster[]>('/api/v1/locations');
    return response.data;
  },
};
