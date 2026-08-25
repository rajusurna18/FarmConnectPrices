import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { farmApi } from '../services/farmApi';
import type { CreateFarmInput, CreateFarmCropInput } from '../types/farmTypes';

export const useFarms = () => {
  return useQuery({
    queryKey: ['farms'],
    queryFn: farmApi.getFarms,
    staleTime: 1000 * 60 * 5,
  });
};

export const useFarm = (farmId?: string) => {
  return useQuery({
    queryKey: ['farms', farmId],
    queryFn: () => farmApi.getFarmById(farmId!),
    enabled: !!farmId,
  });
};

export const useCreateFarm = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateFarmInput) => farmApi.createFarm(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['farms'] });
    },
  });
};

export const useUpdateFarm = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ farmId, input }: { farmId: string; input: CreateFarmInput }) =>
      farmApi.updateFarm(farmId, input),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['farms'] });
      queryClient.invalidateQueries({ queryKey: ['farms', variables.farmId] });
    },
  });
};

export const useDeleteFarm = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (farmId: string) => farmApi.deleteFarm(farmId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['farms'] });
    },
  });
};

export const useFarmCrops = (farmId?: string) => {
  return useQuery({
    queryKey: ['farmCrops', farmId],
    queryFn: () => farmApi.getFarmCrops(farmId!),
    enabled: !!farmId,
  });
};

export const useAddFarmCrop = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ farmId, input }: { farmId: string; input: CreateFarmCropInput }) =>
      farmApi.addFarmCrop(farmId, input),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['farmCrops', variables.farmId] });
    },
  });
};

export const useUpdateFarmCrop = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ farmId, farmCropId, input }: { farmId: string; farmCropId: string; input: Partial<CreateFarmCropInput> }) =>
      farmApi.updateFarmCrop(farmId, farmCropId, input),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['farmCrops', variables.farmId] });
    },
  });
};

export const useDeleteFarmCrop = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ farmId, farmCropId }: { farmId: string; farmCropId: string }) =>
      farmApi.deleteFarmCrop(farmId, farmCropId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['farmCrops', variables.farmId] });
    },
  });
};

export const useCrops = () => {
  return useQuery({
    queryKey: ['cropsMaster'],
    queryFn: farmApi.getCrops,
    staleTime: 1000 * 60 * 30,
  });
};

export const useLocations = () => {
  return useQuery({
    queryKey: ['locationsMaster'],
    queryFn: farmApi.getLocations,
    staleTime: 1000 * 60 * 30,
  });
};
