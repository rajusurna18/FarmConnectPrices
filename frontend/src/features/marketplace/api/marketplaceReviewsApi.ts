import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import type {
  CreateReviewRequest,
  ReviewResponse,
  ReviewEligibilityResponse,
  RatingSummaryResponse,
  ReviewPageResponse,
} from '../../../types/review';

export const marketplaceReviewsApi = {
  // Submit review
  createReview: async (request: CreateReviewRequest): Promise<ReviewResponse> => {
    const response = await apiClient.post<ReviewResponse>('/api/v1/marketplace/reviews', request);
    return response.data;
  },

  // Check eligibility for order review
  getReviewEligibility: async (orderId: string): Promise<ReviewEligibilityResponse> => {
    const response = await apiClient.get<ReviewEligibilityResponse>(
      `/api/v1/marketplace/reviews/eligibility/${orderId}`
    );
    return response.data;
  },

  // Get my submitted reviews
  getMyReviews: async (page: number = 0, size: number = 20): Promise<ReviewPageResponse> => {
    const response = await apiClient.get<ReviewPageResponse>('/api/v1/marketplace/reviews/mine', {
      params: { page, size },
    });
    return response.data;
  },

  // Get reviews received by user
  getUserReviews: async (userId: string, page: number = 0, size: number = 20): Promise<ReviewPageResponse> => {
    const response = await apiClient.get<ReviewPageResponse>(`/api/v1/marketplace/users/${userId}/reviews`, {
      params: { page, size },
    });
    return response.data;
  },

  // Get order reviews
  getOrderReviews: async (orderId: string): Promise<ReviewResponse[]> => {
    const response = await apiClient.get<ReviewResponse[]>(`/api/v1/marketplace/orders/${orderId}/reviews`);
    return response.data;
  },

  // Get user rating summary
  getUserRatingSummary: async (userId: string): Promise<RatingSummaryResponse> => {
    const response = await apiClient.get<RatingSummaryResponse>(`/api/v1/marketplace/users/${userId}/rating-summary`);
    return response.data;
  },
};

// React Query Hooks

export const useReviewEligibilityQuery = (orderId: string) => {
  return useQuery({
    queryKey: ['review-eligibility', orderId],
    queryFn: () => marketplaceReviewsApi.getReviewEligibility(orderId),
    enabled: Boolean(orderId),
    staleTime: 30000,
  });
};

export const useMyReviewsQuery = (page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: ['my-reviews', page, size],
    queryFn: () => marketplaceReviewsApi.getMyReviews(page, size),
    staleTime: 60000,
  });
};

export const useUserReviewsQuery = (userId: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: ['user-reviews', userId, page, size],
    queryFn: () => marketplaceReviewsApi.getUserReviews(userId, page, size),
    enabled: Boolean(userId),
    staleTime: 60000,
  });
};

export const useOrderReviewsQuery = (orderId: string) => {
  return useQuery({
    queryKey: ['order-reviews', orderId],
    queryFn: () => marketplaceReviewsApi.getOrderReviews(orderId),
    enabled: Boolean(orderId),
    staleTime: 30000,
  });
};

export const useUserRatingSummaryQuery = (userId: string) => {
  return useQuery({
    queryKey: ['rating-summary', userId],
    queryFn: () => marketplaceReviewsApi.getUserRatingSummary(userId),
    enabled: Boolean(userId),
    staleTime: 300000, // 5 minutes
  });
};

export const useCreateReviewMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateReviewRequest) => marketplaceReviewsApi.createReview(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['review-eligibility', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['order-reviews', data.orderId] });
      queryClient.invalidateQueries({ queryKey: ['my-reviews'] });
      queryClient.invalidateQueries({ queryKey: ['user-reviews', data.revieweeUid] });
      queryClient.invalidateQueries({ queryKey: ['rating-summary', data.revieweeUid] });
    },
  });
};
