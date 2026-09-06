import { apiClient } from '../../../services/api';

export type ForecastHorizon = '1_DAY' | '3_DAYS' | '7_DAYS' | '14_DAYS';
export type ForecastConfidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'INSUFFICIENT_DATA';
export type ForecastDirection = 'UP' | 'DOWN' | 'STABLE' | 'UNCERTAIN';

export interface ForecastBacktestDto {
  sampleCount: number;
  mae: number | null;
  mape: number | null;
  horizonEvaluated: string;
}

export interface ForecastResponse {
  cropId: string;
  cropName: string;
  marketId: string;
  marketName: string;
  model: string;
  horizon: ForecastHorizon | string;
  currentVerifiedPrice: number | null;
  forecastPrice: number | null;
  forecastLowerBound: number | null;
  forecastUpperBound: number | null;
  direction: ForecastDirection;
  confidence: ForecastConfidence;
  dataQuality: 'GOOD' | 'LIMITED' | 'INSUFFICIENT' | string;
  observationsUsed: number;
  latestObservationDate: string | null;
  unit: string;
  currency: string;
  generatedAt: string;
  backtest: ForecastBacktestDto | null;
  limitations: string[];
  disclaimer: string;
}

export interface ForecastScenarioProfitabilityResponse {
  farmEconomicRecordId: string;
  currentPriceUsed: number | null;
  forecastPriceUsed: number | null;
  priceDifference: number | null;
  priceDifferencePercentage: number | null;
  isScenario: boolean;
  scenarioTag: string;
  currentEvaluation: unknown;
  scenarioEvaluation: unknown;
  scenarioRevenueDelta: number | null;
  scenarioProfitDelta: number | null;
  disclaimer: string;
}

export const getPriceForecast = async (
  cropId: string,
  marketId: string,
  horizon: ForecastHorizon = '7_DAYS',
  lookbackDays: number = 30,
  unit?: string
): Promise<ForecastResponse> => {
  const params: Record<string, unknown> = {
    cropId,
    marketId,
    horizon,
    lookbackDays,
  };
  if (unit) {
    params.unit = unit;
  }
  const response = await apiClient.get<ForecastResponse>('/api/v1/market-intelligence/forecast', { params });
  return response.data;
};

export const evaluateForecastScenario = async (
  farmEconomicRecordId: string,
  horizon: ForecastHorizon = '7_DAYS'
): Promise<ForecastScenarioProfitabilityResponse> => {
  const response = await apiClient.post<ForecastScenarioProfitabilityResponse>(
    `/api/v1/farm-economics/${encodeURIComponent(farmEconomicRecordId)}/forecast-scenario`,
    null,
    { params: { horizon } }
  );
  return response.data;
};
