import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';

export interface CropMasterItem {
  id: string;
  name: string;
  category?: string;
  scientificName?: string;
  status?: string;
}

export function useCrops() {
  return useQuery<CropMasterItem[]>({
    queryKey: ['crops'],
    queryFn: async () => {
      const response = await apiClient.get<CropMasterItem[]>('/api/v1/crops');
      return response.data;
    },
    staleTime: 1000 * 60 * 30,
    retry: 1,
  });
}

