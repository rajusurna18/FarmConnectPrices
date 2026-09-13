import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import type {
  Payment,
  PaymentIntentResponse,
  PaymentPageResponse,
  VerifyPaymentRequest,
} from '../../../types/payment';

export const marketplacePaymentsApi = {
  // Create or retrieve active payment intent
  createPaymentIntent: async (orderId: string): Promise<PaymentIntentResponse> => {
    const response = await apiClient.post<PaymentIntentResponse>(
      '/api/v1/marketplace/payments/create-intent',
      { orderId }
    );
    return response.data;
  },

  // Verify payment attempt
  verifyPayment: async ({
    paymentId,
    request,
  }: {
    paymentId: string;
    request?: VerifyPaymentRequest;
  }): Promise<Payment> => {
    const response = await apiClient.post<Payment>(
      `/api/v1/marketplace/payments/${paymentId}/verify`,
      request
    );
    return response.data;
  },

  // Get buyer payment history
  getMyPayments: async (status?: string, page: number = 0, size: number = 20): Promise<PaymentPageResponse> => {
    const response = await apiClient.get<PaymentPageResponse>('/api/v1/marketplace/payments/mine', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Get received payments for farmer
  getReceivedPayments: async (status?: string, page: number = 0, size: number = 20): Promise<PaymentPageResponse> => {
    const response = await apiClient.get<PaymentPageResponse>('/api/v1/marketplace/payments/received', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Get single payment detail
  getPayment: async (paymentId: string): Promise<Payment> => {
    const response = await apiClient.get<Payment>(`/api/v1/marketplace/payments/${paymentId}`);
    return response.data;
  },
};

export function useCreatePaymentIntentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderId: string) => marketplacePaymentsApi.createPaymentIntent(orderId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['marketplace-payments'] });
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
    },
  });
}

export function useVerifyPaymentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ paymentId, request }: { paymentId: string; request?: VerifyPaymentRequest }) =>
      marketplacePaymentsApi.verifyPayment({ paymentId, request }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['payment-detail', data.paymentId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-payments'] });
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
    },
  });
}

export function useMyPaymentsQuery(status?: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['marketplace-payments', status, page, size],
    queryFn: () => marketplacePaymentsApi.getMyPayments(status, page, size),
  });
}

export function useReceivedPaymentsQuery(status?: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['marketplace-received-payments', status, page, size],
    queryFn: () => marketplacePaymentsApi.getReceivedPayments(status, page, size),
  });
}

export function usePaymentDetailQuery(paymentId: string) {
  return useQuery({
    queryKey: ['payment-detail', paymentId],
    queryFn: () => marketplacePaymentsApi.getPayment(paymentId),
    enabled: Boolean(paymentId),
  });
}
