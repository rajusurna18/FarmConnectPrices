/**
 * Module 24 — Order Issues, Disputes & Resolution Foundation
 * Dedicated Dispute API Service and TanStack Query Integration.
 */

import axios from 'axios';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import type {
  CreateDisputeRequest,
  AddDisputeResponseRequest,
  AddDisputeEvidenceRequest,
  DisputeResponse,
  DisputeDetailResponse,
  DisputePageResponse,
  DisputeResponseRecord,
  DisputeEvidence,
  DisputeQueryParams,
} from '../../../types/dispute';

/**
 * Deterministic query key factory for React Query caching and cache invalidation.
 */
export const disputeQueryKeys = {
  all: ['marketplace-disputes'] as const,
  mine: (params?: DisputeQueryParams) =>
    ['marketplace-disputes', 'mine', params?.status ?? 'ALL', params?.page ?? 0, params?.size ?? 20, params?.cursor ?? null] as const,
  related: (params?: DisputeQueryParams) =>
    ['marketplace-disputes', 'related', params?.status ?? 'ALL', params?.page ?? 0, params?.size ?? 20, params?.cursor ?? null] as const,
  detail: (disputeId: string) => ['marketplace-disputes', 'detail', disputeId] as const,
  byOrder: (orderId: string) => ['marketplace-disputes', 'order', orderId] as const,
};

/**
 * Dedicated Marketplace Dispute API service.
 * Mediates all dispute operations through backend REST endpoints with Firebase authentication.
 */
export const marketplaceDisputesApi = {
  /**
   * Create a new dispute for an eligible order (POST /api/v1/marketplace/disputes).
   */
  createDispute: async (request: CreateDisputeRequest): Promise<DisputeResponse> => {
    const response = await apiClient.post<DisputeResponse>('/api/v1/marketplace/disputes', request);
    return response.data;
  },

  /**
   * Get disputes created by the authenticated caller (GET /api/v1/marketplace/disputes/mine).
   * Supports cursor-based pagination; cursor string is passed strictly unchanged.
   */
  getMyDisputes: async (params?: DisputeQueryParams): Promise<DisputePageResponse> => {
    const response = await apiClient.get<DisputePageResponse>('/api/v1/marketplace/disputes/mine', {
      params: {
        status: params?.status,
        page: params?.page,
        size: params?.size,
        cursor: params?.cursor,
      },
    });
    return response.data;
  },

  /**
   * Get disputes concerning orders where the caller is the counterparty (GET /api/v1/marketplace/disputes/related).
   * Supports cursor-based pagination; cursor string is passed strictly unchanged.
   */
  getRelatedDisputes: async (params?: DisputeQueryParams): Promise<DisputePageResponse> => {
    const response = await apiClient.get<DisputePageResponse>('/api/v1/marketplace/disputes/related', {
      params: {
        status: params?.status,
        page: params?.page,
        size: params?.size,
        cursor: params?.cursor,
      },
    });
    return response.data;
  },

  /**
   * Get all disputes associated with a specific order (GET /api/v1/marketplace/disputes/order/{orderId}).
   */
  getDisputesByOrderId: async (orderId: string): Promise<DisputeResponse[]> => {
    const response = await apiClient.get<DisputeResponse[]>(`/api/v1/marketplace/disputes/order/${orderId}`);
    return response.data;
  },

  /**
   * Get single dispute full detail including responses, evidence, and history (GET /api/v1/marketplace/disputes/{disputeId}).
   */
  getDisputeById: async (disputeId: string): Promise<DisputeDetailResponse> => {
    const response = await apiClient.get<DisputeDetailResponse>(`/api/v1/marketplace/disputes/${disputeId}`);
    return response.data;
  },

  /**
   * Submit a participant response message to an active dispute (POST /api/v1/marketplace/disputes/{disputeId}/responses).
   */
  addResponse: async (disputeId: string, request: AddDisputeResponseRequest): Promise<DisputeResponseRecord> => {
    const response = await apiClient.post<DisputeResponseRecord>(
      `/api/v1/marketplace/disputes/${disputeId}/responses`,
      request
    );
    return response.data;
  },

  /**
   * Register supporting evidence metadata for an active dispute (POST /api/v1/marketplace/disputes/{disputeId}/evidence).
   * Note: Client provides metadata only; file upload is not performed or accepted.
   */
  addEvidence: async (disputeId: string, request: AddDisputeEvidenceRequest): Promise<DisputeEvidence> => {
    const response = await apiClient.post<DisputeEvidence>(
      `/api/v1/marketplace/disputes/${disputeId}/evidence`,
      request
    );
    return response.data;
  },

  /**
   * Voluntary dispute closure by the original creator (POST /api/v1/marketplace/disputes/{disputeId}/close).
   */
  closeDispute: async (disputeId: string): Promise<DisputeResponse> => {
    const response = await apiClient.post<DisputeResponse>(`/api/v1/marketplace/disputes/${disputeId}/close`);
    return response.data;
  },
};

/**
 * Normalizes error messages from dispute API and network errors.
 */
export function extractDisputeApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (typeof error.response?.data === 'string' && error.response.data.trim()) {
      return error.response.data.trim();
    }
    const dataObj = error.response?.data as { message?: string } | undefined;
    if (typeof dataObj?.message === 'string' && dataObj.message.trim()) {
      return dataObj.message.trim();
    }
    if (error.response?.status === 400) return 'Invalid dispute request or validation error.';
    if (error.response?.status === 401) return 'Authentication required. Please sign in to continue.';
    if (error.response?.status === 403) return 'You are not authorized to access or modify this dispute.';
    if (error.response?.status === 404) return 'The requested dispute or order was not found.';
    if (error.response?.status === 409) return 'Dispute conflict: an active dispute already exists or is already closed.';
    if (error.response?.status === 503) return 'Database quota exceeded. Please try again shortly.';
    if (error.message) return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred while communicating with dispute services.';
}

// ==========================================
// TanStack Query Hooks
// ==========================================

/**
 * Hook to query disputes created by the current authenticated user.
 */
export function useMyDisputesQuery(params?: DisputeQueryParams) {
  return useQuery({
    queryKey: disputeQueryKeys.mine(params),
    queryFn: () => marketplaceDisputesApi.getMyDisputes(params),
    staleTime: 30000,
  });
}

/**
 * Hook to query disputes where the current authenticated user is the counterparty.
 */
export function useRelatedDisputesQuery(params?: DisputeQueryParams) {
  return useQuery({
    queryKey: disputeQueryKeys.related(params),
    queryFn: () => marketplaceDisputesApi.getRelatedDisputes(params),
    staleTime: 30000,
  });
}

/**
 * Hook to query disputes associated with a specific order.
 */
export function useOrderDisputesQuery(orderId: string) {
  return useQuery({
    queryKey: disputeQueryKeys.byOrder(orderId),
    queryFn: () => marketplaceDisputesApi.getDisputesByOrderId(orderId),
    enabled: Boolean(orderId),
    staleTime: 30000,
  });
}

/**
 * Hook to query full details for a single dispute.
 */
export function useDisputeDetailQuery(disputeId: string) {
  return useQuery({
    queryKey: disputeQueryKeys.detail(disputeId),
    queryFn: () => marketplaceDisputesApi.getDisputeById(disputeId),
    enabled: Boolean(disputeId),
    staleTime: 15000,
  });
}

/**
 * Hook to create a dispute. Invalidates relevant list, order, and order detail caches.
 */
export function useCreateDisputeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateDisputeRequest) => marketplaceDisputesApi.createDispute(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.all });
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.byOrder(data.orderId) });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
    },
  });
}

/**
 * Hook to submit a participant response to a dispute.
 */
export function useAddDisputeResponseMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ disputeId, message }: { disputeId: string; message: string }) =>
      marketplaceDisputesApi.addResponse(disputeId, { message }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.detail(variables.disputeId) });
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.all });
    },
  });
}

/**
 * Hook to register evidence metadata for a dispute.
 */
export function useAddDisputeEvidenceMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ disputeId, request }: { disputeId: string; request: AddDisputeEvidenceRequest }) =>
      marketplaceDisputesApi.addEvidence(disputeId, request),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.detail(variables.disputeId) });
    },
  });
}

/**
 * Hook for voluntary dispute closure by creator.
 */
export function useCloseDisputeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (disputeId: string) => marketplaceDisputesApi.closeDispute(disputeId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.detail(data.disputeId) });
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.all });
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.byOrder(data.orderId) });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
      queryClient.invalidateQueries({ queryKey: ['order-detail', data.orderId] });
    },
  });
}

