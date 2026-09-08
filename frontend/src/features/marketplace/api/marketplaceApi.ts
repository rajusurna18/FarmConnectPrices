import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import type {
  ProductListing,
  CreateListingRequest,
  UpdateListingRequest,
  ListingPageResponse,
  ListingFilterParams,
} from '../../../types/marketplace';

export const marketplaceApi = {
  // Public browse active produce listings
  browseListings: async (params?: ListingFilterParams): Promise<ListingPageResponse> => {
    const response = await apiClient.get<ListingPageResponse>('/api/v1/marketplace/listings', {
      params,
    });
    return response.data;
  },

  // Get single listing detail
  getListing: async (listingId: string): Promise<ProductListing> => {
    const response = await apiClient.get<ProductListing>(`/api/v1/marketplace/listings/${listingId}`);
    return response.data;
  },

  // Get authenticated farmer's own listings
  getMyListings: async (status?: string, page: number = 0, size: number = 20): Promise<ListingPageResponse> => {
    const response = await apiClient.get<ListingPageResponse>('/api/v1/marketplace/listings/mine', {
      params: { status, page, size },
    });
    return response.data;
  },

  // Create listing
  createListing: async (payload: CreateListingRequest): Promise<ProductListing> => {
    const response = await apiClient.post<ProductListing>('/api/v1/marketplace/listings', payload);
    return response.data;
  },

  // Update listing
  updateListing: async ({
    listingId,
    payload,
  }: {
    listingId: string;
    payload: UpdateListingRequest;
  }): Promise<ProductListing> => {
    const response = await apiClient.put<ProductListing>(`/api/v1/marketplace/listings/${listingId}`, payload);
    return response.data;
  },

  // Update status
  updateListingStatus: async ({
    listingId,
    status,
  }: {
    listingId: string;
    status: string;
  }): Promise<ProductListing> => {
    const response = await apiClient.patch<ProductListing>(
      `/api/v1/marketplace/listings/${listingId}/status`,
      { status }
    );
    return response.data;
  },

  // Delete listing
  deleteListing: async (listingId: string): Promise<void> => {
    await apiClient.delete(`/api/v1/marketplace/listings/${listingId}`);
  },
};

// TanStack Query Hooks with Caching

export const useMarketplaceBrowseQuery = (params?: ListingFilterParams) => {
  return useQuery({
    queryKey: ['marketplaceListings', params],
    queryFn: () => marketplaceApi.browseListings(params),
    staleTime: 60 * 1000,
  });
};

export const useListingDetailQuery = (listingId: string) => {
  return useQuery({
    queryKey: ['marketplaceListing', listingId],
    queryFn: () => marketplaceApi.getListing(listingId),
    enabled: Boolean(listingId),
  });
};

export const useMyListingsQuery = (status?: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: ['myMarketplaceListings', status, page, size],
    queryFn: () => marketplaceApi.getMyListings(status, page, size),
  });
};

export const useCreateListingMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceApi.createListing,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['marketplaceListings'] });
      queryClient.invalidateQueries({ queryKey: ['myMarketplaceListings'] });
    },
  });
};

export const useUpdateListingMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceApi.updateListing,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['marketplaceListings'] });
      queryClient.invalidateQueries({ queryKey: ['myMarketplaceListings'] });
      queryClient.invalidateQueries({ queryKey: ['marketplaceListing', data.listingId] });
    },
  });
};

export const useUpdateListingStatusMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceApi.updateListingStatus,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['marketplaceListings'] });
      queryClient.invalidateQueries({ queryKey: ['myMarketplaceListings'] });
      queryClient.invalidateQueries({ queryKey: ['marketplaceListing', data.listingId] });
    },
  });
};

export const useDeleteListingMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceApi.deleteListing,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['marketplaceListings'] });
      queryClient.invalidateQueries({ queryKey: ['myMarketplaceListings'] });
    },
  });
};
