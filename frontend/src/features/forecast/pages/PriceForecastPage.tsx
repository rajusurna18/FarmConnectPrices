import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  TrendingUp,
  TrendingDown,
  Minus,
  AlertTriangle,
  CheckCircle2,
  HelpCircle,
  ChevronDown,
  ChevronUp,
  Sparkles,
  Info,
  Calendar,
  Layers,
  Activity,
  Calculator,
} from 'lucide-react';
import { apiClient } from '../../../services/api';
import {
  getPriceForecast,
  evaluateForecastScenario,
} from '../services/forecastApi';
import type {
  ForecastHorizon,
  ForecastResponse,
  ForecastScenarioProfitabilityResponse,
} from '../services/forecastApi';
import { PriceForecastChart } from '../components/PriceForecastChart';

interface Crop {
  id: string;
  name: string;
}

interface Market {
  id: string;
  name: string;
  state?: string;
  district?: string;
}

interface FarmEconomicRecord {
  id: string;
  cropName?: string;
  farmName?: string;
  season?: string;
}

interface HistoricalPriceItem {
  priceDate: string;
  modalPrice: number;
}

interface AiDecisionResponseData {
  engine?: string;
  summary?: string;
  recommendation?: string;
  verifiedFacts?: string[];
}

export const PriceForecastPage: React.FC = () => {
  const [selectedCropId, setSelectedCropId] = useState<string>('');
  const [selectedMarketId, setSelectedMarketId] = useState<string>('');
  const [selectedHorizon, setSelectedHorizon] = useState<ForecastHorizon>('7_DAYS');
  const [showDetails, setShowDetails] = useState<boolean>(false);
  const [selectedEconomicRecordId, setSelectedEconomicRecordId] = useState<string>('');

  // Fetch Crops List
  const { data: crops = [], isLoading: isLoadingCrops } = useQuery({
    queryKey: ['cropsMaster'],
    queryFn: async () => {
      const res = await apiClient.get<Crop[]>('/api/v1/crops');
      return res.data;
    },
  });

  // Fetch Markets List
  const { data: markets = [], isLoading: isLoadingMarkets } = useQuery({
    queryKey: ['marketsMaster'],
    queryFn: async () => {
      const res = await apiClient.get<Market[]>('/api/v1/markets');
      return res.data;
    },
  });

  // Fetch Farmer Economics Records
  const { data: economicRecords = [] } = useQuery({
    queryKey: ['myFarmEconomics'],
    queryFn: async () => {
      try {
        const res = await apiClient.get<FarmEconomicRecord[]>('/api/v1/farm-economics');
        return res.data || [];
      } catch {
        return [];
      }
    },
  });

  // Auto-select first crop & market when available
  useEffect(() => {
    if (crops.length > 0 && !selectedCropId) {
      setSelectedCropId(crops[0].id);
    }
  }, [crops, selectedCropId]);

  useEffect(() => {
    if (markets.length > 0 && !selectedMarketId) {
      setSelectedMarketId(markets[0].id);
    }
  }, [markets, selectedMarketId]);

  // Main Forecast Query
  const {
    data: forecast,
    isLoading: isLoadingForecast,
    error: forecastError,
  } = useQuery<ForecastResponse>({
    queryKey: ['priceForecast', selectedCropId, selectedMarketId, selectedHorizon],
    queryFn: () => getPriceForecast(selectedCropId, selectedMarketId, selectedHorizon, 30),
    enabled: Boolean(selectedCropId && selectedMarketId),
  });

  // Historical Prices Query for Chart
  const { data: historicalPrices = [] } = useQuery({
    queryKey: ['marketPriceHistory', selectedCropId, selectedMarketId],
    queryFn: async () => {
      if (!selectedCropId || !selectedMarketId) return [];
      const res = await apiClient.get<HistoricalPriceItem[]>('/api/v1/market-prices', {
        params: { cropId: selectedCropId, marketId: selectedMarketId, limit: 14, qualityStatus: 'VERIFIED' },
      });
      const list: HistoricalPriceItem[] = (res.data || []).map((p) => ({
        priceDate: p.priceDate,
        modalPrice: p.modalPrice,
      }));
      list.sort((a, b) => a.priceDate.localeCompare(b.priceDate));
      return list;
    },
    enabled: Boolean(selectedCropId && selectedMarketId),
  });

  // AI Explanation Query
  const { data: aiExplanation } = useQuery<AiDecisionResponseData | null>({
    queryKey: ['aiForecastExplanation', selectedCropId, selectedMarketId, selectedHorizon],
    queryFn: async () => {
      if (!selectedCropId || !selectedMarketId) return null;
      const res = await apiClient.post<AiDecisionResponseData>('/api/v1/ai/decision', {
        decisionType: 'PRICE_FORECAST_EXPLANATION',
        cropId: selectedCropId,
        marketIds: [selectedMarketId],
      });
      return res.data;
    },
    enabled: Boolean(selectedCropId && selectedMarketId && forecast && forecast.forecastPrice != null),
  });

  // Scenario Profitability Query
  const { data: scenarioResult, isLoading: isLoadingScenario } = useQuery<ForecastScenarioProfitabilityResponse>({
    queryKey: ['forecastScenario', selectedEconomicRecordId, selectedHorizon],
    queryFn: () => evaluateForecastScenario(selectedEconomicRecordId, selectedHorizon),
    enabled: Boolean(selectedEconomicRecordId),
  });

  const getDirectionBadge = (dir?: string) => {
    switch (dir) {
      case 'UP':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
            <TrendingUp className="w-3.5 h-3.5" /> UP (Rising)
          </span>
        );
      case 'DOWN':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/30">
            <TrendingDown className="w-3.5 h-3.5" /> DOWN (Falling)
          </span>
        );
      case 'STABLE':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/30">
            <Minus className="w-3.5 h-3.5" /> STABLE
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/30">
            <HelpCircle className="w-3.5 h-3.5" /> UNCERTAIN
          </span>
        );
    }
  };

  const getConfidenceBadge = (conf?: string) => {
    switch (conf) {
      case 'HIGH':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold bg-emerald-500/20 text-emerald-300">
            <CheckCircle2 className="w-3.5 h-3.5" /> HIGH CONFIDENCE
          </span>
        );
      case 'MEDIUM':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold bg-amber-500/20 text-amber-300">
            <Info className="w-3.5 h-3.5" /> MEDIUM CONFIDENCE
          </span>
        );
      case 'LOW':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold bg-rose-500/20 text-rose-300">
            <AlertTriangle className="w-3.5 h-3.5" /> LOW CONFIDENCE
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold bg-slate-700 text-slate-300">
            INSUFFICIENT DATA
          </span>
        );
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 sm:p-6 lg:p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-emerald-500/10 rounded-xl border border-emerald-500/20 text-emerald-400">
              <Activity className="w-6 h-6" />
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">Price Forecast & Prediction</h1>
          </div>
          <p className="text-slate-400 text-xs sm:text-sm mt-1">
            Historical verified market price estimates & empirical error bounds for decision support
          </p>
        </div>

        {/* Global Disclaimer Banner */}
        <div className="bg-amber-500/10 border border-amber-500/20 text-amber-300/90 rounded-xl p-3 text-xs max-w-lg flex items-start gap-2.5">
          <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
          <span>
            Forecasts are empirical estimates based on verified historical market data and may differ from future prices.
            Always verify live market rates before selling.
          </span>
        </div>
      </div>

      {/* Control Filters Bar */}
      <div className="bg-slate-900/80 p-4 sm:p-5 rounded-2xl border border-slate-800 shadow-lg grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Crop Selection */}
        <div className="space-y-1.5">
          <label className="text-xs font-medium text-slate-300 flex items-center gap-1.5">
            <Layers className="w-3.5 h-3.5 text-emerald-400" /> Select Crop
          </label>
          <select
            value={selectedCropId}
            onChange={(e) => setSelectedCropId(e.target.value)}
            disabled={isLoadingCrops}
            className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-emerald-500"
          >
            {crops.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        {/* Market Selection */}
        <div className="space-y-1.5">
          <label className="text-xs font-medium text-slate-300 flex items-center gap-1.5">
            <Activity className="w-3.5 h-3.5 text-emerald-400" /> Select Mandi / Market
          </label>
          <select
            value={selectedMarketId}
            onChange={(e) => setSelectedMarketId(e.target.value)}
            disabled={isLoadingMarkets}
            className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-emerald-500"
          >
            {markets.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name} {m.district ? `(${m.district})` : ''}
              </option>
            ))}
          </select>
        </div>

        {/* Forecast Horizon Selection */}
        <div className="space-y-1.5 sm:col-span-2 lg:col-span-2">
          <label className="text-xs font-medium text-slate-300 flex items-center gap-1.5">
            <Calendar className="w-3.5 h-3.5 text-amber-400" /> Forecast Horizon
          </label>
          <div className="grid grid-cols-4 gap-2">
            {(['1_DAY', '3_DAYS', '7_DAYS', '14_DAYS'] as ForecastHorizon[]).map((h) => {
              const labels: Record<string, string> = {
                '1_DAY': '1 Day',
                '3_DAYS': '3 Days',
                '7_DAYS': '7 Days',
                '14_DAYS': '14 Days',
              };
              const active = selectedHorizon === h;
              return (
                <button
                  key={h}
                  onClick={() => setSelectedHorizon(h)}
                  className={`py-2 px-2 text-xs font-semibold rounded-xl border transition-all ${
                    active
                      ? 'bg-emerald-500/20 border-emerald-500 text-emerald-300 shadow-md shadow-emerald-950'
                      : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200 hover:border-slate-700'
                  }`}
                >
                  {labels[h]}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Main Forecast Content State */}
      {isLoadingForecast ? (
        <div className="bg-slate-900/60 p-12 rounded-2xl border border-slate-800 flex flex-col items-center justify-center space-y-3">
          <div className="w-8 h-8 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-400 text-sm">Calculating empirical price forecast & error bounds...</p>
        </div>
      ) : forecastError || !forecast ? (
        <div className="bg-rose-500/10 border border-rose-500/20 p-6 rounded-2xl text-rose-300 text-center space-y-2">
          <AlertTriangle className="w-8 h-8 text-rose-400 mx-auto" />
          <p className="font-semibold text-sm">Failed to retrieve price forecast</p>
          <p className="text-xs text-rose-400/80">
            Please ensure historical verified market prices exist for the selected crop and market.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Top Metric Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Current Verified Price Card */}
            <div className="bg-slate-900/80 p-5 rounded-2xl border border-slate-800 space-y-2">
              <div className="flex items-center justify-between text-xs text-slate-400 font-medium">
                <span>Latest Verified Price</span>
                <span className="text-slate-500">{forecast.latestObservationDate || 'N/A'}</span>
              </div>
              <div className="text-2xl font-bold text-slate-100">
                {forecast.currentVerifiedPrice != null
                  ? `₹${forecast.currentVerifiedPrice.toLocaleString('en-IN')}`
                  : 'N/A'}
                <span className="text-xs font-normal text-slate-400 ml-1">/ {forecast.unit}</span>
              </div>
              <p className="text-xs text-slate-500">Source: Verified Mandi Observations</p>
            </div>

            {/* Forecast Price Card */}
            <div className="bg-gradient-to-br from-slate-900 to-amber-950/20 p-5 rounded-2xl border border-amber-500/30 space-y-2 shadow-lg">
              <div className="flex items-center justify-between text-xs text-amber-400 font-medium">
                <span>Estimated Point Forecast</span>
                {getDirectionBadge(forecast.direction)}
              </div>
              <div className="text-3xl font-extrabold text-amber-300">
                {forecast.forecastPrice != null
                  ? `₹${forecast.forecastPrice.toLocaleString('en-IN')}`
                  : 'Unavailable'}
                <span className="text-xs font-normal text-amber-400/80 ml-1">/ {forecast.unit}</span>
              </div>
              <div className="flex items-center justify-between text-xs pt-1">
                <span className="text-slate-400">Horizon: {forecast.horizon}</span>
                {getConfidenceBadge(forecast.confidence)}
              </div>
            </div>

            {/* Empirical Range Card */}
            <div className="bg-slate-900/80 p-5 rounded-2xl border border-slate-800 space-y-2">
              <div className="flex items-center justify-between text-xs text-slate-400 font-medium">
                <span>Empirical Range</span>
                <Info className="w-3.5 h-3.5 text-slate-500" />
              </div>
              <div className="text-xl font-bold text-slate-100">
                {forecast.forecastLowerBound != null && forecast.forecastUpperBound != null
                  ? `₹${forecast.forecastLowerBound.toLocaleString('en-IN')} – ₹${forecast.forecastUpperBound.toLocaleString('en-IN')}`
                  : 'Interval Unavailable'}
              </div>
              <p className="text-xs text-slate-400">
                Derived from walk-forward backtest error ({forecast.backtest?.sampleCount || 0} residuals)
              </p>
            </div>

            {/* Model & Quality Card */}
            <div className="bg-slate-900/80 p-5 rounded-2xl border border-slate-800 space-y-2">
              <div className="flex items-center justify-between text-xs text-slate-400 font-medium">
                <span>Model Baseline</span>
                <span className="text-emerald-400 text-xs font-semibold">{forecast.dataQuality} QUALITY</span>
              </div>
              <div className="text-sm font-semibold text-slate-200">{forecast.model}</div>
              <div className="text-xs text-slate-400 space-y-0.5">
                <div>Observations: {forecast.observationsUsed} verified records</div>
                <div>
                  Backtest MAE:{' '}
                  {forecast.backtest?.mae != null ? `₹${forecast.backtest.mae}` : 'N/A'} | MAPE:{' '}
                  {forecast.backtest?.mape != null ? `${forecast.backtest.mape}%` : 'N/A'}
                </div>
              </div>
            </div>
          </div>

          {/* Forecast Visualization Chart */}
          <PriceForecastChart forecast={forecast} historicalPrices={historicalPrices} />

          {/* AI Forecast Explanation Section */}
          {aiExplanation && (
            <div className="bg-gradient-to-r from-slate-900 via-emerald-950/20 to-slate-900 p-6 rounded-2xl border border-emerald-500/20 shadow-xl space-y-4">
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <div className="flex items-center gap-2">
                  <div className="p-1.5 bg-emerald-500/20 rounded-lg text-emerald-400">
                    <Sparkles className="w-5 h-5" />
                  </div>
                  <h3 className="text-base font-semibold text-slate-100">AI Decision Intelligence Explanation</h3>
                </div>
                <span className="text-xs text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-md border border-emerald-500/20 font-mono">
                  {aiExplanation.engine || 'RULE_BASED'} ENGINE
                </span>
              </div>

              <div className="space-y-3">
                <p className="text-sm text-slate-200 leading-relaxed font-medium">{aiExplanation.summary}</p>

                {aiExplanation.recommendation && (
                  <div className="bg-slate-950/80 p-3.5 rounded-xl border border-slate-800 text-xs text-slate-300 space-y-1">
                    <span className="text-emerald-400 font-semibold uppercase tracking-wider block text-[10px]">
                      Recommendation Guide
                    </span>
                    <p>{aiExplanation.recommendation}</p>
                  </div>
                )}

                {aiExplanation.verifiedFacts && aiExplanation.verifiedFacts.length > 0 && (
                  <div className="space-y-1 pt-1">
                    <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      Verified Data Facts
                    </span>
                    <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
                      {aiExplanation.verifiedFacts.map((fact: string, idx: number) => (
                        <li key={idx} className="flex items-start gap-1.5 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800/80">
                          <span className="text-emerald-400 shrink-0">✓</span>
                          <span>{fact}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Farmer Read-Only Scenario Profitability Integration */}
          {economicRecords.length > 0 && (
            <div className="bg-slate-900/80 p-6 rounded-2xl border border-slate-800 space-y-4 shadow-xl">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800 pb-3">
                <div className="flex items-center gap-2">
                  <Calculator className="w-5 h-5 text-amber-400" />
                  <h3 className="text-base font-semibold text-slate-100">Farmer Scenario Profitability Evaluation</h3>
                </div>
                <span className="text-xs bg-amber-500/10 text-amber-400 px-2.5 py-1 rounded-md border border-amber-500/20 font-semibold">
                  READ-ONLY SCENARIO
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 items-center">
                <div className="space-y-1">
                  <label className="text-xs font-medium text-slate-400">Select Farm Crop Economic Record</label>
                  <select
                    value={selectedEconomicRecordId}
                    onChange={(e) => setSelectedEconomicRecordId(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-100"
                  >
                    <option value="">-- Choose Farm Economics Record --</option>
                    {economicRecords.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.cropName || 'Crop'} ({r.farmName || 'Farm'}) - {r.season || 'Season'}
                      </option>
                    ))}
                  </select>
                </div>
                <p className="text-xs text-slate-400">
                  Evaluates your current net profit versus scenario profit at the estimated forecast price.
                </p>
              </div>

              {isLoadingScenario ? (
                <div className="p-4 text-center text-xs text-slate-400">Calculating scenario economics...</div>
              ) : scenarioResult ? (
                <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                  <div className="space-y-1">
                    <span className="text-slate-400">Current Price Profit</span>
                    <div className="text-base font-bold text-slate-200">
                      ₹{scenarioResult.currentEvaluation && typeof scenarioResult.currentEvaluation === 'object' && 'estimatedProfit' in scenarioResult.currentEvaluation && (scenarioResult.currentEvaluation as { estimatedProfit?: number }).estimatedProfit != null
                        ? Number((scenarioResult.currentEvaluation as { estimatedProfit: number }).estimatedProfit).toLocaleString('en-IN')
                        : '0'}
                    </div>
                  </div>
                  <div className="space-y-1">
                    <span className="text-amber-400 font-medium">Scenario Profit @ Forecast</span>
                    <div className="text-base font-bold text-amber-300">
                      ₹{scenarioResult.scenarioEvaluation && typeof scenarioResult.scenarioEvaluation === 'object' && 'estimatedProfit' in scenarioResult.scenarioEvaluation && (scenarioResult.scenarioEvaluation as { estimatedProfit?: number }).estimatedProfit != null
                        ? Number((scenarioResult.scenarioEvaluation as { estimatedProfit: number }).estimatedProfit).toLocaleString('en-IN')
                        : '0'}
                    </div>
                  </div>
                  <div className="space-y-1">
                    <span className="text-slate-400">Estimated Profit Difference</span>
                    <div
                      className={`text-base font-bold ${
                        (scenarioResult.scenarioProfitDelta || 0) >= 0 ? 'text-emerald-400' : 'text-rose-400'
                      }`}
                    >
                      {(scenarioResult.scenarioProfitDelta || 0) >= 0 ? '+' : ''}
                      ₹{Number(scenarioResult.scenarioProfitDelta || 0).toLocaleString('en-IN')}
                    </div>
                  </div>
                </div>
              ) : null}
            </div>
          )}

          {/* Expandable Forecast Details & Model Audit Drawer */}
          <div className="bg-slate-900/60 rounded-2xl border border-slate-800 overflow-hidden">
            <button
              onClick={() => setShowDetails(!showDetails)}
              className="w-full p-4 text-left flex items-center justify-between text-sm font-semibold text-slate-300 hover:bg-slate-800/50 transition-colors"
            >
              <span className="flex items-center gap-2">
                <Info className="w-4 h-4 text-emerald-400" />
                Forecast Details & Model Performance Audit
              </span>
              {showDetails ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </button>

            {showDetails && (
              <div className="p-4 border-t border-slate-800 space-y-4 text-xs text-slate-300 bg-slate-950/60">
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                  <div>
                    <span className="text-slate-500 block">Model Type & Version</span>
                    <span className="font-semibold text-slate-200">{forecast.model}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Verified Observations Used</span>
                    <span className="font-semibold text-slate-200">{forecast.observationsUsed} records</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Latest Observation Date</span>
                    <span className="font-semibold text-slate-200">{forecast.latestObservationDate || 'N/A'}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Backtest Horizon Evaluated</span>
                    <span className="font-semibold text-slate-200">{forecast.backtest?.horizonEvaluated || forecast.horizon}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Mean Absolute Error (MAE)</span>
                    <span className="font-semibold text-slate-200">
                      {forecast.backtest?.mae != null ? `₹${forecast.backtest.mae}` : 'N/A'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Mean Absolute Percentage Error (MAPE)</span>
                    <span className="font-semibold text-slate-200">
                      {forecast.backtest?.mape != null ? `${forecast.backtest.mape}%` : 'N/A'}
                    </span>
                  </div>
                </div>

                {forecast.limitations && forecast.limitations.length > 0 && (
                  <div className="space-y-1.5 pt-2 border-t border-slate-800/80">
                    <span className="text-slate-400 font-semibold block text-[11px]">Model Limitations & Guardrails</span>
                    <ul className="list-disc list-inside space-y-1 text-slate-400">
                      {forecast.limitations.map((lim, idx) => (
                        <li key={idx}>{lim}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
