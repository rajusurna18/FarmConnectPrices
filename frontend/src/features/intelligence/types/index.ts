export interface MarketComparisonItem {
  marketId: string;
  marketName: string;
  state: string;
  district: string;
  mandal?: string;
  minPrice: number;
  maxPrice: number;
  modalPrice: number;
  currency: string;
  unit: string;
  sourceUnit?: string;
  conversionApplied?: boolean;
  conversionFactor?: number;
  priceDate: string;
  sourceName: string;
  qualityStatus: string;
}

export interface MarketComparisonData {
  crop: {
    id: string;
    name: string;
    category?: string;
  };
  date: string;
  currency: string;
  unit: string;
  markets: MarketComparisonItem[];
  highestMarket?: MarketComparisonItem | null;
  lowestMarket?: MarketComparisonItem | null;
  priceDifference: number;
  percentageDifference?: number | null;
}

export interface MarketIntelligenceSummaryData {
  observationCount: number;
  latestModalPrice: number;
  minObservedModalPrice: number;
  maxObservedModalPrice: number;
  averageModalPrice: number;
  firstObservationDate: string;
  latestObservationDate: string;
  absoluteChange: number;
  percentageChange?: number | null;
  trendDirection: 'INCREASING' | 'DECREASING' | 'STABLE' | 'INSUFFICIENT_DATA';
  currency: string;
  unit: string;
}

export interface PriceTrendPoint {
  priceDate: string;
  modalPrice: number;
  minPrice: number;
  maxPrice: number;
}

export interface PriceTrendData {
  cropId: string;
  cropName: string;
  marketId?: string;
  marketName?: string;
  currency: string;
  unit: string;
  points: PriceTrendPoint[];
  trendDirection: 'INCREASING' | 'DECREASING' | 'STABLE' | 'INSUFFICIENT_DATA';
  absoluteChange: number;
  percentageChange?: number | null;
}

export interface IntelligenceFilterState {
  cropId?: string;
  state?: string;
  district?: string;
  mandal?: string;
  unit?: string;
  fromDate?: string;
  toDate?: string;
}
