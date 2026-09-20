import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import type { BuyerOpportunityPageResponse } from '../../../types/buyerOpportunity';

export const marketplaceBuyerOpportunitiesApi = {
  getBuyerOpportunities: async (
    listingId: string,
    page: number = 0,
    size: number = 10
  ): Promise<BuyerOpportunityPageResponse> => {
    const response = await apiClient.get<BuyerOpportunityPageResponse>(
      '/api/v1/marketplace/buyer-opportunities',
      {
        params: { listingId, page, size },
      }
    );
    return response.data;
  },
};

export function useBuyerOpportunitiesQuery(listingId: string, page: number = 0, size: number = 10) {
  return useQuery({
    queryKey: ['buyer-opportunities', listingId, page, size],
    queryFn: () => marketplaceBuyerOpportunitiesApi.getBuyerOpportunities(listingId, page, size),
    enabled: Boolean(listingId),
  });
}
