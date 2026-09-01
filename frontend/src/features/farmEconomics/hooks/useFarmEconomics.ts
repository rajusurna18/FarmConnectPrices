import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { farmEconomicsService } from '../services/farmEconomicsService';
import type {
  FarmEconomicRecordRequest,
  FarmProfitabilityEvaluationRequest,
  FarmMarketComparisonRequest,
} from '../types';

export const FARM_ECONOMICS_KEYS = {
  all: ['farmEconomics'] as const,
  lists: () => [...FARM_ECONOMICS_KEYS.all, 'list'] as const,
  detail: (id: string) => [...FARM_ECONOMICS_KEYS.all, 'detail', id] as const,
  evaluation: (id?: string) => [...FARM_ECONOMICS_KEYS.all, 'evaluation', id ?? 'custom'] as const,
  comparison: (id?: string) => [...FARM_ECONOMICS_KEYS.all, 'comparison', id ?? 'custom'] as const,
};

export function useFarmEconomics() {
  return useQuery({
    queryKey: FARM_ECONOMICS_KEYS.lists(),
    queryFn: farmEconomicsService.getEconomicRecords,
  });
}

export function useFarmEconomic(id: string) {
  return useQuery({
    queryKey: FARM_ECONOMICS_KEYS.detail(id),
    queryFn: () => farmEconomicsService.getEconomicRecordById(id),
    enabled: Boolean(id),
  });
}

export function useCreateFarmEconomic() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: FarmEconomicRecordRequest) => farmEconomicsService.createEconomicRecord(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FARM_ECONOMICS_KEYS.lists() });
    },
  });
}

export function useUpdateFarmEconomic() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: FarmEconomicRecordRequest }) =>
      farmEconomicsService.updateEconomicRecord(id, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: FARM_ECONOMICS_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: FARM_ECONOMICS_KEYS.detail(variables.id) });
    },
  });
}

export function useDeleteFarmEconomic() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => farmEconomicsService.deleteEconomicRecord(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FARM_ECONOMICS_KEYS.lists() });
    },
  });
}

export function useEvaluateFarmEconomics() {
  return useMutation({
    mutationFn: (payload: FarmProfitabilityEvaluationRequest) =>
      farmEconomicsService.evaluateProfitability(payload),
  });
}

export function useCompareFarmEconomics() {
  return useMutation({
    mutationFn: (payload: FarmMarketComparisonRequest) =>
      farmEconomicsService.compareMarkets(payload),
  });
}
