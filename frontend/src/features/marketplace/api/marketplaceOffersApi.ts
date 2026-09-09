import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import type {
  Offer,
  OfferRound,
  CreateOfferRequest,
  CounterOfferRequest,
  OfferPageResponse,
} from '../../../types/offer';

export const marketplaceOffersApi = {
  // Create offer against an active listing
  createOffer: async (payload: CreateOfferRequest): Promise<Offer> => {
    const response = await apiClient.post<Offer>('/api/v1/marketplace/offers', payload);
    return response.data;
  },

  // Get offers initiated by authenticated buyer/customer
  getMyOffers: async (status?: string, page: number = 0, size: number = 20): Promise<OfferPageResponse> => {
    const response = await apiClient.get<OfferPageResponse>('/api/v1/marketplace/offers/mine', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Get offers received by farmer for owned listings
  getReceivedOffers: async (status?: string, page: number = 0, size: number = 20): Promise<OfferPageResponse> => {
    const response = await apiClient.get<OfferPageResponse>('/api/v1/marketplace/offers/received', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Get single offer detail
  getOffer: async (offerId: string): Promise<Offer> => {
    const response = await apiClient.get<Offer>(`/api/v1/marketplace/offers/${offerId}`);
    return response.data;
  },

  // Counter offer
  counterOffer: async ({
    offerId,
    payload,
  }: {
    offerId: string;
    payload: CounterOfferRequest;
  }): Promise<Offer> => {
    const response = await apiClient.post<Offer>(`/api/v1/marketplace/offers/${offerId}/counter`, payload);
    return response.data;
  },

  // Accept offer
  acceptOffer: async (offerId: string): Promise<Offer> => {
    const response = await apiClient.post<Offer>(`/api/v1/marketplace/offers/${offerId}/accept`);
    return response.data;
  },

  // Reject offer
  rejectOffer: async (offerId: string): Promise<Offer> => {
    const response = await apiClient.post<Offer>(`/api/v1/marketplace/offers/${offerId}/reject`);
    return response.data;
  },

  // Cancel offer (buyer only)
  cancelOffer: async (offerId: string): Promise<Offer> => {
    const response = await apiClient.post<Offer>(`/api/v1/marketplace/offers/${offerId}/cancel`);
    return response.data;
  },

  // Get negotiation round history
  getOfferHistory: async (offerId: string): Promise<OfferRound[]> => {
    const response = await apiClient.get<OfferRound[]>(`/api/v1/marketplace/offers/${offerId}/history`);
    return response.data;
  },
};

// TanStack Query Hooks with Caching

export const useMyOffersQuery = (status?: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: ['myOffers', status, page, size],
    queryFn: () => marketplaceOffersApi.getMyOffers(status, page, size),
  });
};

export const useReceivedOffersQuery = (status?: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: ['receivedOffers', status, page, size],
    queryFn: () => marketplaceOffersApi.getReceivedOffers(status, page, size),
  });
};

export const useOfferDetailQuery = (offerId: string) => {
  return useQuery({
    queryKey: ['offerDetail', offerId],
    queryFn: () => marketplaceOffersApi.getOffer(offerId),
    enabled: Boolean(offerId),
  });
};

export const useOfferHistoryQuery = (offerId: string) => {
  return useQuery({
    queryKey: ['offerHistory', offerId],
    queryFn: () => marketplaceOffersApi.getOfferHistory(offerId),
    enabled: Boolean(offerId),
  });
};

export const useCreateOfferMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceOffersApi.createOffer,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['myOffers'] });
      queryClient.invalidateQueries({ queryKey: ['receivedOffers'] });
      queryClient.invalidateQueries({ queryKey: ['marketplaceListing', data.listingId] });
    },
  });
};

export const useCounterOfferMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceOffersApi.counterOffer,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['myOffers'] });
      queryClient.invalidateQueries({ queryKey: ['receivedOffers'] });
      queryClient.invalidateQueries({ queryKey: ['offerDetail', data.offerId] });
      queryClient.invalidateQueries({ queryKey: ['offerHistory', data.offerId] });
    },
  });
};

export const useAcceptOfferMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceOffersApi.acceptOffer,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['myOffers'] });
      queryClient.invalidateQueries({ queryKey: ['receivedOffers'] });
      queryClient.invalidateQueries({ queryKey: ['offerDetail', data.offerId] });
      queryClient.invalidateQueries({ queryKey: ['offerHistory', data.offerId] });
    },
  });
};

export const useRejectOfferMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceOffersApi.rejectOffer,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['myOffers'] });
      queryClient.invalidateQueries({ queryKey: ['receivedOffers'] });
      queryClient.invalidateQueries({ queryKey: ['offerDetail', data.offerId] });
      queryClient.invalidateQueries({ queryKey: ['offerHistory', data.offerId] });
    },
  });
};

export const useCancelOfferMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceOffersApi.cancelOffer,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['myOffers'] });
      queryClient.invalidateQueries({ queryKey: ['receivedOffers'] });
      queryClient.invalidateQueries({ queryKey: ['offerDetail', data.offerId] });
      queryClient.invalidateQueries({ queryKey: ['offerHistory', data.offerId] });
    },
  });
};
