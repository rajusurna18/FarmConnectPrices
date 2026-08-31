export interface MarketEvaluationRequest {
  cropId: string;
  marketId: string;
  quantity: number;
  quantityUnit: string;
  priceBasis?: 'MODAL' | 'MIN' | 'MAX';
  priceMode?: 'LATEST_AVAILABLE' | 'EXACT_DATE';
  date?: string | null;
  transportationCost?: number;
  otherSellingCosts?: number;
}

export interface MarketPriceSourceDto {
  type: string;
  name: string;
  reference?: string | null;
}

export interface CropSummary {
  id: string;
  name: string;
  category?: string;
  code?: string;
}

export interface MarketSummary {
  id: string;
  name: string;
  code?: string;
  type?: string;
  state?: string;
  district?: string;
}

export interface MarketEvaluationResponse {
  status: 'SUCCESS' | 'NO_VERIFIED_PRICE' | 'UNIT_MISMATCH' | 'INVALID_CROP' | 'INVALID_MARKET' | 'ERROR';
  message: string;
  crop?: CropSummary;
  market?: MarketSummary;
  selectedPrice?: number;
  priceBasis?: string;
  priceUnit?: string;
  quantity?: number;
  quantityUnit?: string;
  grossRevenue?: number;
  transportationCost?: number;
  otherSellingCosts?: number;
  totalSellingCosts?: number;
  estimatedNetRealization?: number;
  netRealizationPerUnit?: number;
  sellingCostBreakEvenPrice?: number;
  currency?: string;
  priceDate?: string;
  observedAt?: string;
  source?: MarketPriceSourceDto;
  qualityStatus?: string;
  isStalePrice?: boolean;
  staleMessage?: string;
  isLoss?: boolean;
}

export interface MarketCostInput {
  marketId: string;
  transportationCost: number;
  otherSellingCosts: number;
}

export interface MarketComparisonRequest {
  cropId: string;
  quantity: number;
  quantityUnit: string;
  priceBasis?: 'MODAL' | 'MIN' | 'MAX';
  priceMode?: 'LATEST_AVAILABLE' | 'EXACT_DATE';
  date?: string | null;
  markets: MarketCostInput[];
}

export interface MarketProfitabilityComparisonResponse {
  crop: CropSummary;
  quantity: number;
  quantityUnit: string;
  priceBasis: string;
  priceMode: string;
  date?: string;
  evaluations: MarketEvaluationResponse[];
  topRealizationMarket?: MarketEvaluationResponse;
  rankingSummary: string;
  disclaimer: string;
}
