import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';

export interface LocationCascadeResult {
  states: string[];
  districts: string[];
  areas: string[];
  isStatesLoading: boolean;
  isDistrictsLoading: boolean;
  isAreasLoading: boolean;
}

export function useLocationCascade(selectedState?: string, selectedDistrict?: string) {
  const { data: states = [], isLoading: isStatesLoading, isError: isStatesError, refetch: refetchStates } = useQuery<string[]>({
    queryKey: ['locations', 'states'],
    queryFn: async () => {
      const response = await apiClient.get<string[]>('/api/v1/locations/states');
      return response.data;
    },
    staleTime: 1000 * 60 * 60,
    retry: 1
  });

  const { data: districts = [], isLoading: isDistrictsLoading, isError: isDistrictsError, refetch: refetchDistricts } = useQuery<string[]>({
    queryKey: ['locations', 'districts', selectedState],
    queryFn: async () => {
      if (!selectedState) return [];
      const response = await apiClient.get<string[]>('/api/v1/locations/districts', {
        params: { state: selectedState }
      });
      return response.data;
    },
    enabled: Boolean(selectedState),
    staleTime: 1000 * 60 * 60,
    retry: 1
  });

  const { data: areas = [], isLoading: isAreasLoading, isError: isAreasError, refetch: refetchAreas } = useQuery<string[]>({
    queryKey: ['locations', 'areas', selectedState, selectedDistrict],
    queryFn: async () => {
      if (!selectedState || !selectedDistrict) return [];
      const response = await apiClient.get<string[]>('/api/v1/locations/areas', {
        params: { state: selectedState, district: selectedDistrict }
      });
      return response.data;
    },
    enabled: Boolean(selectedState && selectedDistrict),
    staleTime: 1000 * 60 * 60,
    retry: 1
  });

  return {
    states,
    districts,
    areas,
    isStatesLoading,
    isDistrictsLoading,
    isAreasLoading,
    isStatesError,
    isDistrictsError,
    isAreasError,
    refetchStates,
    refetchDistricts,
    refetchAreas
  };
}

