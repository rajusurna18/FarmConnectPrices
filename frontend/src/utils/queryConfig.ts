interface ApiError {
  response?: {
    status?: number;
  };
}

/**
 * Standard retry function for React Query across FarmConnectPrices.
 * Immediately returns false when status is 503 (FirestoreQuotaExhaustedException),
 * preventing duplicate/retry storms when database quota is active.
 */
export const defaultRetry = (failureCount: number, error: unknown) => {
  const apiError = error as ApiError;
  if (apiError?.response?.status === 503) return false;
  return failureCount < 1;
};

/**
 * Normalizes query keys using primitive strings to prevent reference-invalidation bugs.
 */
export const queryKeys = {
  states: () => ['locations', 'states'] as const,
  districts: (state?: string) => ['locations', 'districts', state || ''] as const,
  areas: (state?: string, district?: string) => ['locations', 'areas', state || '', district || ''] as const,
  crops: () => ['crops'] as const,
  markets: (state?: string, district?: string, mandal?: string, type?: string) =>
    ['markets', state || '', district || '', mandal || '', type || ''] as const,
  market: (id?: string) => ['market', id || ''] as const,
  marketCrops: (marketId?: string) => ['marketCrops', marketId || ''] as const,
  marketPrices: (filters?: {
    state?: string;
    district?: string;
    marketId?: string;
    cropId?: string;
    priceDate?: string;
    unit?: string;
    qualityStatus?: string;
  }) =>
    [
      'marketPrices',
      filters?.state || '',
      filters?.district || '',
      filters?.marketId || '',
      filters?.cropId || '',
      filters?.priceDate || '',
      filters?.unit || 'QUINTAL',
      filters?.qualityStatus || '',
    ] as const,
  marketPriceDetail: (id?: string) => ['marketPrice', id || ''] as const,
  intelligenceTrends: (filters?: { state?: string; district?: string; marketId?: string; cropId?: string; unit?: string }) =>
    ['market-intelligence', 'trends', filters?.state || '', filters?.district || '', filters?.marketId || '', filters?.cropId || '', filters?.unit || 'QUINTAL'] as const,
  intelligenceSummary: (filters?: { state?: string; district?: string; marketId?: string; cropId?: string; unit?: string }) =>
    ['market-intelligence', 'summary', filters?.state || '', filters?.district || '', filters?.marketId || '', filters?.cropId || '', filters?.unit || 'QUINTAL'] as const,
  intelligenceCompare: (filters?: { state?: string; district?: string; marketId?: string; cropId?: string; unit?: string }) =>
    ['market-intelligence', 'compare', filters?.state || '', filters?.district || '', filters?.marketId || '', filters?.cropId || '', filters?.unit || 'QUINTAL'] as const,
};
