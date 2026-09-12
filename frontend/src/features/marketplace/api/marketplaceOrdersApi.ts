import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import type {
  Order,
  OrderPageResponse,
  CancelOrderRequest,
} from '../../../types/order';

export const marketplaceOrdersApi = {
  // Create order from accepted offer
  createOrderFromOffer: async (offerId: string): Promise<Order> => {
    const response = await apiClient.post<Order>(`/api/v1/marketplace/orders/from-offer/${offerId}`);
    return response.data;
  },

  // Get orders created by buyer/customer
  getMyOrders: async (status?: string, page: number = 0, size: number = 20): Promise<OrderPageResponse> => {
    const response = await apiClient.get<OrderPageResponse>('/api/v1/marketplace/orders/mine', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Get orders received by farmer for owned listings
  getReceivedOrders: async (status?: string, page: number = 0, size: number = 20): Promise<OrderPageResponse> => {
    const response = await apiClient.get<OrderPageResponse>('/api/v1/marketplace/orders/received', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Get single order detail
  getOrder: async (orderId: string): Promise<Order> => {
    const response = await apiClient.get<Order>(`/api/v1/marketplace/orders/${orderId}`);
    return response.data;
  },

  // Confirm order (Farmer)
  confirmOrder: async (orderId: string): Promise<Order> => {
    const response = await apiClient.post<Order>(`/api/v1/marketplace/orders/${orderId}/confirm`);
    return response.data;
  },

  // Process order (Farmer)
  processOrder: async (orderId: string): Promise<Order> => {
    const response = await apiClient.post<Order>(`/api/v1/marketplace/orders/${orderId}/process`);
    return response.data;
  },

  // Complete order (Farmer)
  completeOrder: async (orderId: string): Promise<Order> => {
    const response = await apiClient.post<Order>(`/api/v1/marketplace/orders/${orderId}/complete`);
    return response.data;
  },

  // Cancel order (Participant)
  cancelOrder: async ({
    orderId,
    reason,
  }: {
    orderId: string;
    reason?: string;
  }): Promise<Order> => {
    const response = await apiClient.post<Order>(`/api/v1/marketplace/orders/${orderId}/cancel`, {
      reason,
    } as CancelOrderRequest);
    return response.data;
  },
};

export function useCreateOrderFromOfferMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offerId: string) => marketplaceOrdersApi.createOrderFromOffer(offerId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-received-orders'] });
      queryClient.invalidateQueries({ queryKey: ['offer-detail', data.offerId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-listing', data.listingId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-listings'] });
    },
  });
}

export function useMyOrdersQuery(status?: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['marketplace-orders', status, page, size],
    queryFn: () => marketplaceOrdersApi.getMyOrders(status, page, size),
  });
}

export function useReceivedOrdersQuery(status?: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['marketplace-received-orders', status, page, size],
    queryFn: () => marketplaceOrdersApi.getReceivedOrders(status, page, size),
  });
}

export function useOrderDetailQuery(orderId: string) {
  return useQuery({
    queryKey: ['order-detail', orderId],
    queryFn: () => marketplaceOrdersApi.getOrder(orderId),
    enabled: Boolean(orderId),
  });
}

export function useConfirmOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderId: string) => marketplaceOrdersApi.confirmOrder(orderId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-received-orders'] });
    },
  });
}

export function useProcessOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderId: string) => marketplaceOrdersApi.processOrder(orderId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-received-orders'] });
    },
  });
}

export function useCompleteOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderId: string) => marketplaceOrdersApi.completeOrder(orderId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-received-orders'] });
    },
  });
}

export function useCancelOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, reason }: { orderId: string; reason?: string }) =>
      marketplaceOrdersApi.cancelOrder({ orderId, reason }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-received-orders'] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-listing', data.listingId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-listings'] });
    },
  });
}
