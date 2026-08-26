export interface MarketLocation {
  state?: string | null;
  district?: string | null;
  mandal?: string | null;
  village?: string | null;
  pincode?: string | null;
}

export type MarketType = 'MANDI' | 'RYTHU_BAZAAR' | 'WHOLESALE_MARKET' | 'LOCAL_MARKET' | 'OTHER';

export type MarketStatus = 'ACTIVE' | 'INACTIVE';

export const MARKET_TYPE_LABELS: Record<MarketType, string> = {
  MANDI: 'Mandi',
  RYTHU_BAZAAR: 'Rythu Bazaar',
  WHOLESALE_MARKET: 'Wholesale Market',
  LOCAL_MARKET: 'Local Market',
  OTHER: 'Other Market',
};

export interface MarketSummary {
  id: string;
  name: string;
  code: string;
  type: MarketType;
  state: string;
  district: string;
  mandal: string;
  status: MarketStatus;
  supportedCropCount: number;
}

export interface Market {
  id: string;
  name: string;
  code: string;
  type: MarketType;
  location: MarketLocation;
  latitude?: number | null;
  longitude?: number | null;
  status: MarketStatus;
  createdAt: string;
  updatedAt: string;
}

export interface MarketCrop {
  id: string;
  marketId: string;
  cropId: string;
  cropName: string;
  cropCategory: string;
  cropScientificName?: string;
  status: MarketStatus;
  createdAt: string;
  updatedAt: string;
}

export interface MarketFilterState {
  state?: string;
  district?: string;
  mandal?: string;
  type?: string;
  cropId?: string;
}
