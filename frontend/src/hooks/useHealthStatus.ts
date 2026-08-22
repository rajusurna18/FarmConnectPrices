import { useQuery } from '@tanstack/react-query';
import { fetchHealthStatus } from '../services/healthService';

export const useHealthStatus = () => {
  return useQuery({
    queryKey: ['healthStatus'],
    queryFn: fetchHealthStatus,
    retry: 1,
    staleTime: 10000,
  });
};
