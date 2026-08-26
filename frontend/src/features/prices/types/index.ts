export type PriceQualityStatus = 'VERIFIED' | 'UNVERIFIED' | 'REJECTED';
export type PriceStatus = 'ACTIVE' | 'INACTIVE';
export type PriceSourceType = 'GOVERNMENT' | 'MARKET' | 'VERIFIED_PARTNER' | 'MANUAL' | 'IMPORTED_DATA' | 'OTHER';

export interface MarketPriceSource {
  type: PriceSourceType;
  name: string;
  reference: string | null;
}

export interface MarketPriceSummary {
  id: string;
  marketId: string;
  marketName: string;
  cropId: string;
  cropName: string;
  priceDate: string;
  minPrice: number;
  maxPrice: number;
  modalPrice: number;
  currency: string;
  unit: string;
  sourceUnit?: string;
  conversionApplied?: boolean;
  conversionFactor?: number;
  sourceType: PriceSourceType;
  sourceName: string;
  qualityStatus: PriceQualityStatus;
  status: PriceStatus;
}

export interface MarketPriceDetail {
  id: string;
  market: {
    id: string;
    name: string;
    code: string;
    type: string;
    state: string;
    district: string;
    mandal: string;
    status: string;
  };
  crop: {
    id: string;
    name: string;
    category: string;
    scientificName: string;
    seasonality: string;
    status: string;
  };
  priceDate: string;
  observedAt: string;
  minPrice: number;
  maxPrice: number;
  modalPrice: number;
  currency: string;
  unit: string;
  sourceUnit?: string;
  conversionApplied?: boolean;
  conversionFactor?: number;
  source: MarketPriceSource;
  qualityStatus: PriceQualityStatus;
  status: PriceStatus;
  createdAt: string;
  updatedAt: string;
}

export interface MarketPriceFilterState {
  marketId?: string;
  cropId?: string;
  priceDate?: string;
  fromDate?: string;
  toDate?: string;
  qualityStatus?: PriceQualityStatus;
  unit?: string;
  state?: string;
  district?: string;
}

export const QUALITY_STATUS_LABELS: Record<PriceQualityStatus, string> = {
  VERIFIED: 'Verified Data',
  UNVERIFIED: 'Unverified Observation',
  REJECTED: 'Rejected Record',
};

export const SOURCE_TYPE_LABELS: Record<PriceSourceType, string> = {
  GOVERNMENT: 'Government Feed',
  MARKET: 'Direct Market Feed',
  VERIFIED_PARTNER: 'Verified Partner',
  MANUAL: 'Manual Field Report',
  IMPORTED_DATA: 'Imported Telemetry Data',
  OTHER: 'Other Source',
};
