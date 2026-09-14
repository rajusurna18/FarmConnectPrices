import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import type {
  Delivery,
  DeliveryPageResponse,
  CreateDeliveryRequest,
  AssignDeliveryPartnerRequest,
} from '../../../types/delivery';

export const marketplaceDeliveriesApi = {
  // Create delivery request
  createDelivery: async (request: CreateDeliveryRequest): Promise<Delivery> => {
    const response = await apiClient.post<Delivery>('/api/v1/marketplace/deliveries', request);
    return response.data;
  },

  // Get buyer deliveries
  getMyDeliveries: async (status?: string, page: number = 0, size: number = 20): Promise<DeliveryPageResponse> => {
    const response = await apiClient.get<DeliveryPageResponse>('/api/v1/marketplace/deliveries/mine', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Get received deliveries for farmer
  getReceivedDeliveries: async (status?: string, page: number = 0, size: number = 20): Promise<DeliveryPageResponse> => {
    const response = await apiClient.get<DeliveryPageResponse>('/api/v1/marketplace/deliveries/received', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Get single delivery detail
  getDelivery: async (deliveryId: string): Promise<Delivery> => {
    const response = await apiClient.get<Delivery>(`/api/v1/marketplace/deliveries/${deliveryId}`);
    return response.data;
  },

  // Get delivery by order ID
  getDeliveryByOrderId: async (orderId: string): Promise<Delivery> => {
    const response = await apiClient.get<Delivery>(`/api/v1/marketplace/orders/${orderId}/delivery`);
    return response.data;
  },

  // Assign delivery partner
  assignPartner: async ({ deliveryId, request }: { deliveryId: string; request: AssignDeliveryPartnerRequest }): Promise<Delivery> => {
    const response = await apiClient.post<Delivery>(`/api/v1/marketplace/deliveries/${deliveryId}/assign`, request);
    return response.data;
  },

  // Mark ready for pickup
  markReady: async (deliveryId: string): Promise<Delivery> => {
    const response = await apiClient.post<Delivery>(`/api/v1/marketplace/deliveries/${deliveryId}/ready`);
    return response.data;
  },

  // Mark picked up
  markPickedUp: async (deliveryId: string): Promise<Delivery> => {
    const response = await apiClient.post<Delivery>(`/api/v1/marketplace/deliveries/${deliveryId}/pickup`);
    return response.data;
  },

  // Mark in-transit
  markInTransit: async (deliveryId: string): Promise<Delivery> => {
    const response = await apiClient.post<Delivery>(`/api/v1/marketplace/deliveries/${deliveryId}/in-transit`);
    return response.data;
  },

  // Mark out-for-delivery
  markOutForDelivery: async (deliveryId: string): Promise<Delivery> => {
    const response = await apiClient.post<Delivery>(`/api/v1/marketplace/deliveries/${deliveryId}/out-for-delivery`);
    return response.data;
  },

  // Mark delivered
  markDelivered: async (deliveryId: string): Promise<Delivery> => {
    const response = await apiClient.post<Delivery>(`/api/v1/marketplace/deliveries/${deliveryId}/delivered`);
    return response.data;
  },

  // Cancel delivery
  cancelDelivery: async ({ deliveryId, reason }: { deliveryId: string; reason?: string }): Promise<Delivery> => {
    const response = await apiClient.post<Delivery>(`/api/v1/marketplace/deliveries/${deliveryId}/cancel`, { reason });
    return response.data;
  },
};

export function useCreateDeliveryMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateDeliveryRequest) => marketplaceDeliveriesApi.createDelivery(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['marketplace-deliveries'] });
      queryClient.invalidateQueries({ queryKey: ['order-delivery', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
    },
  });
}

export function useMyDeliveriesQuery(status?: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['marketplace-deliveries', status, page, size],
    queryFn: () => marketplaceDeliveriesApi.getMyDeliveries(status, page, size),
  });
}

export function useReceivedDeliveriesQuery(status?: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['marketplace-received-deliveries', status, page, size],
    queryFn: () => marketplaceDeliveriesApi.getReceivedDeliveries(status, page, size),
  });
}

export function useDeliveryDetailQuery(deliveryId: string) {
  return useQuery({
    queryKey: ['delivery-detail', deliveryId],
    queryFn: () => marketplaceDeliveriesApi.getDelivery(deliveryId),
    enabled: Boolean(deliveryId),
  });
}

export function useOrderDeliveryQuery(orderId: string) {
  return useQuery({
    queryKey: ['order-delivery', orderId],
    queryFn: () => marketplaceDeliveriesApi.getDeliveryByOrderId(orderId),
    enabled: Boolean(orderId),
    retry: false,
  });
}

export function useAssignPartnerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ deliveryId, request }: { deliveryId: string; request: AssignDeliveryPartnerRequest }) =>
      marketplaceDeliveriesApi.assignPartner({ deliveryId, request }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['delivery-detail', data.deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-deliveries'] });
      queryClient.invalidateQueries({ queryKey: ['order-delivery', data.orderId] });
    },
  });
}

export function useUpdateDeliveryStatusMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ deliveryId, action }: { deliveryId: string; action: 'ready' | 'pickup' | 'in-transit' | 'out-for-delivery' | 'delivered' }) => {
      switch (action) {
        case 'ready': return marketplaceDeliveriesApi.markReady(deliveryId);
        case 'pickup': return marketplaceDeliveriesApi.markPickedUp(deliveryId);
        case 'in-transit': return marketplaceDeliveriesApi.markInTransit(deliveryId);
        case 'out-for-delivery': return marketplaceDeliveriesApi.markOutForDelivery(deliveryId);
        case 'delivered': return marketplaceDeliveriesApi.markDelivered(deliveryId);
      }
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['delivery-detail', data.deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-deliveries'] });
      queryClient.invalidateQueries({ queryKey: ['order-delivery', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
    },
  });
}

export function useCancelDeliveryMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ deliveryId, reason }: { deliveryId: string; reason?: string }) =>
      marketplaceDeliveriesApi.cancelDelivery({ deliveryId, reason }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['delivery-detail', data.deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-deliveries'] });
      queryClient.invalidateQueries({ queryKey: ['order-delivery', data.orderId] });
    },
  });
}
