export type ProductionCostCategory =
  | 'SEEDS'
  | 'FERTILIZER'
  | 'PESTICIDES'
  | 'LABOR'
  | 'IRRIGATION'
  | 'MACHINERY'
  | 'LAND'
  | 'OTHER';

export interface ProductionCostItem {
  id?: string;
  category: ProductionCostCategory;
  description?: string;
  amount: number;
  currency?: string;
}

export interface SellingCosts {
  transportationCost: number;
  otherSellingCosts: number;
}

export interface FarmEconomicRecordRequest {
  farmId: string;
  cropId: string;
  season: 'KHARIF' | 'RABI' | 'ZAID';
  cultivatedArea: number;
  cultivatedAreaUnit: 'ACRE' | 'HECTARE';
  expectedYield: number;
  yieldUnit: 'KG' | 'QUINTAL' | 'TON';
  productionCosts: ProductionCostItem[];
  sellingCosts: SellingCosts;
}

export interface FarmEconomicRecordResponse {
  id: string;
  ownerUid: string;
  farmId: string;
  cropId: string;
  farmName?: string;
  cropName?: string;
  season: string;
  cultivatedArea: number;
  cultivatedAreaUnit: string;
  expectedYield: number;
  yieldUnit: string;
  productionCosts: ProductionCostItem[];
  sellingCosts: SellingCosts;
  totalProductionCost: number;
  totalSellingCost: number;
  totalCost: number;
  productionCostPerUnit: number;
  totalCostPerUnit: number;
  breakEvenSellingPrice: number;
  createdAt: string;
  updatedAt: string;
}

export interface FarmProfitabilityEvaluationRequest {
  economicRecordId?: string;
  farmId?: string;
  cropId?: string;
  season?: string;
  cultivatedArea?: number;
  cultivatedAreaUnit?: string;
  expectedYield?: number;
  yieldUnit?: string;
  productionCosts?: ProductionCostItem[];
  marketId?: string;
  priceBasis?: 'MIN' | 'MODAL' | 'MAX';
  priceMode?: 'EXACT_DATE' | 'LATEST_AVAILABLE';
  date?: string;
  transportationCost?: number;
  otherSellingCosts?: number;
}

export interface FarmProfitabilityEvaluationResponse {
  status: string;
  message: string;
  crop?: {
    id: string;
    name: string;
    code: string;
    category: string;
  };
  farm?: {
    id: string;
    name: string;
    landArea: number;
    landAreaUnit: string;
  };
  market?: {
    id: string;
    name: string;
    code: string;
    state: string;
    district: string;
  };
  selectedPrice?: number;
  priceBasis?: string;
  priceUnit?: string;
  expectedYield?: number;
  yieldUnit?: string;
  grossRevenue?: number;
  totalProductionCost?: number;
  totalSellingCost?: number;
  totalCost?: number;
  estimatedNetRealization?: number;
  estimatedProfit?: number;
  profitPerUnit?: number;
  productionCostPerUnit?: number;
  totalCostPerUnit?: number;
  breakEvenSellingPrice?: number;
  roi?: number | null;
  profitabilityStatus?: 'PROFITABLE' | 'BREAK_EVEN' | 'LOSS' | 'INSUFFICIENT_DATA';
  currency?: string;
  priceDate?: string;
  observedAt?: string;
  source?: {
    type?: string;
    name?: string;
  };
  qualityStatus?: string;
  stalePrice?: boolean;
  staleMessage?: string;
  costCompleteness?: 'COMPLETE' | 'PARTIAL';
}

export interface FarmMarketComparisonRequest {
  economicRecordId?: string;
  farmId?: string;
  cropId?: string;
  season?: string;
  expectedYield?: number;
  yieldUnit?: string;
  productionCosts?: ProductionCostItem[];
  priceBasis?: 'MIN' | 'MODAL' | 'MAX';
  priceMode?: 'EXACT_DATE' | 'LATEST_AVAILABLE';
  date?: string;
  markets: {
    marketId: string;
    transportationCost: number;
    otherSellingCosts: number;
  }[];
}

export interface FarmMarketComparisonResponse {
  crop?: {
    id: string;
    name: string;
  };
  expectedYield?: number;
  yieldUnit?: string;
  priceBasis?: string;
  priceMode?: string;
  date?: string;
  evaluations: FarmProfitabilityEvaluationResponse[];
  topMarket?: FarmProfitabilityEvaluationResponse;
  rankingSummary: string;
}
