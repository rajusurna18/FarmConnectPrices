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
  comparisonScope?: string;
  observationSummary?: string;
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

export interface MarketTrendData {
  cropId?: string;
  cropName?: string;
  marketId?: string;
  marketName?: string;
  currency: string;
  unit: string;
  period: string;
  startDate?: string;
  endDate?: string;

  observationCount: number;
  earliestPrice?: number | null;
  latestPrice?: number | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  avgPrice?: number | null;
  priceRange?: number | null;
  absoluteChange?: number | null;
  percentageChange?: number | null;

  trendDirection: 'RISING' | 'FALLING' | 'STABLE' | 'INSUFFICIENT_DATA';
  volatility: 'LOW' | 'MEDIUM' | 'HIGH' | 'INSUFFICIENT_DATA';
  volatilityCvPercent?: number | null;
  dataQuality: 'GOOD' | 'LIMITED' | 'INSUFFICIENT';
  freshnessStatus: 'FRESH' | 'STALE' | 'UNAVAILABLE';
  latestObservationDate?: string;

  currentVsAverageStatement?: string;
  currentVsAveragePctDiff?: number | null;

  points: PriceTrendPoint[];
  aiExplanation?: {
    summary?: string;
    recommendation?: string;
    confidence?: 'HIGH' | 'MEDIUM' | 'LOW';
    verifiedFacts?: string[];
    reasoning?: string[];
    risks?: string[];
    nextSteps?: string[];
    engine?: string;
    aiGenerated?: boolean;
    dataFreshness?: {
      status?: string;
      staleMessage?: string;
      marketPriceDate?: string;
    };
  } | null;
}

export interface IntelligenceFilterState {
  cropId?: string;
  marketId?: string;
  period?: '7D' | '30D' | '90D' | '6M' | '1Y' | 'CUSTOM';
  startDate?: string;
  endDate?: string;
  state?: string;
  district?: string;
  mandal?: string;
  unit?: string;
  fromDate?: string;
  toDate?: string;
  includeAiExplanation?: boolean;
}
