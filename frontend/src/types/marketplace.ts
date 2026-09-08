export type ProductListingStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'SOLD_OUT' | 'EXPIRED';

export type ProductListingQualityGrade = 'PREMIUM' | 'GRADE_A' | 'GRADE_B' | 'STANDARD' | 'UNSPECIFIED';

export interface LocationDto {
  state?: string;
  district?: string;
  mandal?: string;
  village?: string;
  pincode?: string;
}

export interface MarketPriceReference {
  verifiedModalPrice?: number;
  verifiedMinPrice?: number;
  verifiedMaxPrice?: number;
  priceUnit?: string;
  marketName?: string;
  date?: string;
  note?: string;
}

export interface ProductListing {
  listingId: string;
  ownerUid: string;
  cropId: string;
  cropName: string;
  quantity: number;
  availableQuantity: number;
  unit: string;
  askingPrice: number;
  priceUnit: string;
  location?: LocationDto;
  description?: string;
  qualityGrade?: ProductListingQualityGrade;
  harvestDate?: string;
  availableFrom?: string;
  status: ProductListingStatus;
  createdAt: string;
  updatedAt: string;
  referenceMarketPrice?: MarketPriceReference;
}

export interface CreateListingRequest {
  cropId: string;
  quantity: number;
  availableQuantity?: number;
  unit: string;
  askingPrice: number;
  priceUnit: string;
  location?: LocationDto;
  description?: string;
  qualityGrade?: ProductListingQualityGrade;
  harvestDate?: string;
  availableFrom?: string;
  status?: ProductListingStatus;
}

export interface UpdateListingRequest {
  quantity?: number;
  availableQuantity?: number;
  unit?: string;
  askingPrice?: number;
  priceUnit?: string;
  location?: LocationDto;
  description?: string;
  qualityGrade?: ProductListingQualityGrade;
  harvestDate?: string;
  availableFrom?: string;
}

export interface UpdateListingStatusRequest {
  status: ProductListingStatus;
}

export interface ListingPageResponse {
  items: ProductListing[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface ListingFilterParams {
  cropId?: string;
  state?: string;
  district?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: 'newest' | 'price_asc' | 'price_desc' | 'quantity_desc';
  page?: number;
  size?: number;
}
