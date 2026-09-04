import React, { useState } from 'react';
import {
  TrendingUp, TrendingDown, Minus, ShieldCheck,
  RefreshCw, AlertCircle, Calendar,
  Sparkles, CheckCircle2, AlertTriangle, Info, Clock, BarChart3
} from 'lucide-react';
import {
  ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid
} from 'recharts';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { useMarketTrends } from '../hooks/useMarketTrends';
import { useCrops } from '../../prices/hooks/useCrops';
import { useMarkets } from '../../markets/hooks/useMarkets';
import type { IntelligenceFilterState } from '../types';

export const MarketTrendsPage: React.FC = () => {
  const [filters, setFilters] = useState<IntelligenceFilterState>({
    cropId: undefined,
    marketId: undefined,
    period: '30D',
    startDate: undefined,
    endDate: undefined,
    unit: 'QUINTAL',
    includeAiExplanation: true
  });

  const { data: rawCrops = [] } = useCrops();
  const { data: markets = [] } = useMarkets();
  const { data: trendData, isLoading, isError, error, refetch } = useMarketTrends(filters);

  const availableCrops = rawCrops.map((c) => ({ id: c.id, name: c.name }));

  const handlePeriodChange = (period: '7D' | '30D' | '90D' | '6M' | '1Y' | 'CUSTOM') => {
    setFilters((prev) => ({
      ...prev,
      period,
      startDate: period === 'CUSTOM' ? prev.startDate : undefined,
      endDate: period === 'CUSTOM' ? prev.endDate : undefined
    }));
  };

  const getTrendBadge = (dir?: string) => {
    switch (dir) {
      case 'RISING':
        return (
          <span className="inline-flex items-center space-x-1 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-bold uppercase tracking-wider">
            <TrendingUp className="w-3.5 h-3.5" />
            <span>Rising Trend</span>
          </span>
        );
      case 'FALLING':
        return (
          <span className="inline-flex items-center space-x-1 px-3 py-1 rounded-full bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-bold uppercase tracking-wider">
            <TrendingDown className="w-3.5 h-3.5" />
            <span>Falling Trend</span>
          </span>
        );
      case 'STABLE':
        return (
          <span className="inline-flex items-center space-x-1 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/30 text-blue-400 text-xs font-bold uppercase tracking-wider">
            <Minus className="w-3.5 h-3.5" />
            <span>Stable Price</span>
          </span>
        );
      case 'INSUFFICIENT_DATA':
      default:
        return (
          <span className="inline-flex items-center space-x-1 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-bold uppercase tracking-wider">
            <AlertCircle className="w-3.5 h-3.5" />
            <span>Insufficient Data</span>
          </span>
        );
    }
  };

  const getVolatilityBadge = (vol?: string) => {
    switch (vol) {
      case 'LOW':
        return <span className="px-2.5 py-0.5 rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-semibold">Low Volatility</span>;
      case 'MEDIUM':
        return <span className="px-2.5 py-0.5 rounded-md bg-amber-500/10 text-amber-400 border border-amber-500/30 text-xs font-semibold">Medium Volatility</span>;
      case 'HIGH':
        return <span className="px-2.5 py-0.5 rounded-md bg-rose-500/10 text-rose-400 border border-rose-500/30 text-xs font-semibold">High Volatility</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-md bg-slate-800 text-slate-400 text-xs font-semibold">N/A</span>;
    }
  };

  const getQualityBadge = (qual?: string) => {
    switch (qual) {
      case 'GOOD':
        return <span className="px-2.5 py-0.5 rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-semibold">Good Quality</span>;
      case 'LIMITED':
        return <span className="px-2.5 py-0.5 rounded-md bg-amber-500/10 text-amber-400 border border-amber-500/30 text-xs font-semibold">Limited Quality</span>;
      case 'INSUFFICIENT':
      default:
        return <span className="px-2.5 py-0.5 rounded-md bg-rose-500/10 text-rose-400 border border-rose-500/30 text-xs font-semibold">Insufficient Data</span>;
    }
  };

  const getFreshnessBadge = (fresh?: string, dateStr?: string) => {
    if (fresh === 'FRESH') {
      return (
        <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-medium">
          <Clock className="w-3 h-3" />
          <span>Fresh Data ({dateStr})</span>
        </span>
      );
    }
    if (fresh === 'STALE') {
      return (
        <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-md bg-amber-500/10 text-amber-400 border border-amber-500/30 text-xs font-medium">
          <Clock className="w-3 h-3" />
          <span>Stale ({dateStr})</span>
        </span>
      );
    }
    return (
      <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-md bg-slate-800 text-slate-400 text-xs font-medium">
        <Clock className="w-3 h-3" />
        <span>No Data</span>
      </span>
    );
  };

  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between overflow-x-hidden font-sans select-none">
      <Navbar />

      <main className="w-full flex-grow pt-[calc(env(safe-area-inset-top)+5.5rem)] pb-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto space-y-8">
        
        {/* HERO SECTION */}
        <section className="relative rounded-3xl bg-slate-900/60 border border-slate-800 p-6 sm:p-10 overflow-hidden shadow-2xl backdrop-blur-xl">
          <div className="relative z-20 max-w-3xl space-y-3">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold uppercase tracking-wider">
              <BarChart3 className="w-3.5 h-3.5" />
              <span>Verified Market Data • Historical Trends</span>
            </div>

            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight leading-tight">
              Market Price <span className="text-emerald-400">Trend Intelligence</span>
            </h1>

            <p className="text-sm sm:text-base text-slate-300 leading-relaxed">
              Analyze historical price movement, volatility, and period averages computed strictly from verified AGMARKNET mandi observations.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-3 text-xs font-medium text-slate-400">
              <span className="px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-800 text-emerald-400 flex items-center space-x-1.5">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Deterministic Trend Analysis • Zero Synthetic Data</span>
              </span>
            </div>
          </div>
        </section>

        {/* CONTROLS & FILTERS */}
        <section className="w-full bg-slate-900/80 p-5 rounded-2xl border border-slate-800 backdrop-blur-md space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Commodity Selector */}
            <div>
              <label htmlFor="trend-crop-select" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                Commodity / Crop
              </label>
              <select
                id="trend-crop-select"
                value={filters.cropId || ''}
                onChange={(e) => setFilters((prev) => ({ ...prev, cropId: e.target.value || undefined }))}
                className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500 truncate"
              >
                <option value="">All Commodities ({availableCrops.length})</option>
                {availableCrops.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Mandi Selector */}
            <div>
              <label htmlFor="trend-market-select" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                Target Mandi / Market
              </label>
              <select
                id="trend-market-select"
                value={filters.marketId || ''}
                onChange={(e) => setFilters((prev) => ({ ...prev, marketId: e.target.value || undefined }))}
                className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500 truncate"
              >
                <option value="">All Mandis ({markets.length})</option>
                {markets.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Unit Selector */}
            <div>
              <label htmlFor="trend-unit-select" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                Display Unit
              </label>
              <select
                id="trend-unit-select"
                value={filters.unit || 'QUINTAL'}
                onChange={(e) => setFilters((prev) => ({ ...prev, unit: e.target.value }))}
                className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500"
              >
                <option value="QUINTAL">Quintal (100 KG)</option>
                <option value="KG">Kilogram (KG)</option>
                <option value="TONNE">Tonne (1,000 KG)</option>
              </select>
            </div>

            {/* AI Explanation Toggle */}
            <div className="flex flex-col justify-end">
              <label className="flex items-center space-x-2 text-xs font-semibold text-slate-300 cursor-pointer min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800">
                <input
                  type="checkbox"
                  checked={filters.includeAiExplanation ?? true}
                  onChange={(e) => setFilters((prev) => ({ ...prev, includeAiExplanation: e.target.checked }))}
                  className="w-4 h-4 text-emerald-500 bg-slate-900 border-slate-700 rounded focus:ring-emerald-500"
                />
                <span>Include AI Insight Summary</span>
              </label>
            </div>
          </div>

          {/* Period Selector Tabs */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/80">
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mr-2">Time Horizon:</span>
              {(['7D', '30D', '90D', '6M', '1Y', 'CUSTOM'] as const).map((p) => (
                <button
                  key={p}
                  type="button"
                  onClick={() => handlePeriodChange(p)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                    filters.period === p
                      ? 'bg-emerald-500 text-slate-950 font-bold shadow-md shadow-emerald-500/20'
                      : 'bg-slate-950 text-slate-400 hover:text-slate-200 border border-slate-800'
                  }`}
                >
                  {p}
                </button>
              ))}
            </div>

            {/* Custom Date Pickers */}
            {filters.period === 'CUSTOM' && (
              <div className="flex items-center space-x-2">
                <input
                  type="date"
                  value={filters.startDate || ''}
                  onChange={(e) => setFilters((prev) => ({ ...prev, startDate: e.target.value }))}
                  className="px-2.5 py-1 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200"
                />
                <span className="text-slate-500 text-xs">to</span>
                <input
                  type="date"
                  value={filters.endDate || ''}
                  onChange={(e) => setFilters((prev) => ({ ...prev, endDate: e.target.value }))}
                  className="px-2.5 py-1 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200"
                />
              </div>
            )}
          </div>
        </section>

        {/* LOADING & ERROR STATES */}
        {isLoading && (
          <div className="p-12 text-center rounded-2xl bg-slate-900/50 border border-slate-800 space-y-3">
            <RefreshCw className="w-8 h-8 text-emerald-400 animate-spin mx-auto" />
            <p className="text-sm font-medium text-slate-300">Evaluating verified historical market price trends...</p>
          </div>
        )}

        {isError && (
          <div className="p-6 rounded-2xl bg-rose-500/10 border border-rose-500/30 flex items-start space-x-3 text-rose-300">
            <AlertCircle className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />
            <div className="space-y-1">
              <h4 className="text-sm font-bold text-rose-200">Failed to load trend intelligence</h4>
              <p className="text-xs">{error instanceof Error ? error.message : 'An error occurred while fetching trend data.'}</p>
              <button
                onClick={() => refetch()}
                className="mt-2 px-3 py-1 rounded-lg bg-rose-500/20 text-rose-200 hover:bg-rose-500/30 text-xs font-semibold transition-all"
              >
                Retry Request
              </button>
            </div>
          </div>
        )}

        {/* TREND CONTENT */}
        {!isLoading && !isError && trendData && (
          <div className="space-y-8">
            
            {/* KPI STATS CARDS GRID */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              
              {/* Card 1: Latest Price */}
              <div className="bg-slate-900/80 p-5 rounded-2xl border border-slate-800 flex flex-col justify-between space-y-2 backdrop-blur-md">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Latest Observation</span>
                  {getFreshnessBadge(trendData.freshnessStatus, trendData.latestObservationDate)}
                </div>
                <div className="space-y-1">
                  <div className="text-2xl sm:text-3xl font-extrabold text-white">
                    {trendData.latestPrice != null ? `₹${trendData.latestPrice.toLocaleString('en-IN')}` : 'N/A'}
                  </div>
                  <div className="text-xs text-slate-400 font-medium">per {trendData.unit}</div>
                </div>
              </div>

              {/* Card 2: Period Average */}
              <div className="bg-slate-900/80 p-5 rounded-2xl border border-slate-800 flex flex-col justify-between space-y-2 backdrop-blur-md">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Historical Average</span>
                  <span className="text-xs font-semibold text-slate-400">{trendData.period} Period</span>
                </div>
                <div className="space-y-1">
                  <div className="text-2xl sm:text-3xl font-extrabold text-emerald-400">
                    {trendData.avgPrice != null ? `₹${trendData.avgPrice.toLocaleString('en-IN')}` : 'N/A'}
                  </div>
                  <div className="text-xs text-slate-400 font-medium">
                    Min: ₹{trendData.minPrice?.toLocaleString('en-IN')} | Max: ₹{trendData.maxPrice?.toLocaleString('en-IN')}
                  </div>
                </div>
              </div>

              {/* Card 3: Price Movement */}
              <div className="bg-slate-900/80 p-5 rounded-2xl border border-slate-800 flex flex-col justify-between space-y-2 backdrop-blur-md">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Price Movement</span>
                  {getTrendBadge(trendData.trendDirection)}
                </div>
                <div className="space-y-1">
                  <div className={`text-2xl sm:text-3xl font-extrabold ${
                    (trendData.absoluteChange ?? 0) > 0 ? 'text-emerald-400' : (trendData.absoluteChange ?? 0) < 0 ? 'text-rose-400' : 'text-slate-200'
                  }`}>
                    {trendData.absoluteChange != null
                      ? `${trendData.absoluteChange >= 0 ? '+' : ''}₹${trendData.absoluteChange.toLocaleString('en-IN')}`
                      : 'N/A'}
                  </div>
                  <div className="text-xs text-slate-400 font-medium">
                    {trendData.percentageChange != null ? `${trendData.percentageChange >= 0 ? '+' : ''}${trendData.percentageChange.toFixed(2)}% over period` : 'Zero start price'}
                  </div>
                </div>
              </div>

              {/* Card 4: Data Quality & Volatility */}
              <div className="bg-slate-900/80 p-5 rounded-2xl border border-slate-800 flex flex-col justify-between space-y-2 backdrop-blur-md">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Data & Volatility</span>
                  <span className="text-xs text-slate-400 font-semibold">{trendData.observationCount} records</span>
                </div>
                <div className="flex flex-wrap items-center gap-2 pt-1">
                  {getVolatilityBadge(trendData.volatility)}
                  {getQualityBadge(trendData.dataQuality)}
                </div>
                <div className="text-xs text-slate-500 pt-1">
                  {trendData.volatilityCvPercent != null ? `CV: ${trendData.volatilityCvPercent.toFixed(2)}%` : 'Insufficient records for CV'}
                </div>
              </div>
            </div>

            {/* CURRENT VS HISTORICAL AVERAGE BANNER */}
            {trendData.currentVsAverageStatement && (
              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-start space-x-3 text-slate-300">
                <Info className="w-5 h-5 text-emerald-400 flex-shrink-0 mt-0.5" />
                <div className="text-xs sm:text-sm font-medium leading-relaxed">
                  {trendData.currentVsAverageStatement}
                </div>
              </div>
            )}

            {/* HISTORICAL CHART SECTION */}
            <section className="bg-slate-900/80 p-6 rounded-2xl border border-slate-800 backdrop-blur-md space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h3 className="text-base font-bold text-white flex items-center space-x-2">
                    <Calendar className="w-4 h-4 text-emerald-400" />
                    <span>Verified Historical Price Trend</span>
                  </h3>
                  <p className="text-xs text-slate-400">
                    Plotting verified mandi modal prices ({trendData.cropName || 'Crop'} at {trendData.marketName || 'All Markets'}) over {trendData.period}.
                  </p>
                </div>
                <div className="text-xs font-medium text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
                  {trendData.points.length} Verified Points Plotted
                </div>
              </div>

              {trendData.points.length > 0 ? (
                <div className="h-72 w-full pt-4">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={trendData.points} margin={{ top: 10, right: 20, left: 10, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.5} />
                      <XAxis dataKey="priceDate" stroke="#94a3b8" fontSize={11} tickMargin={10} />
                      <YAxis stroke="#94a3b8" fontSize={11} domain={['auto', 'auto']} tickFormatter={(v) => `₹${v}`} />
                      <Tooltip
                        contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '0.75rem', color: '#f8fafc', fontSize: '12px' }}
                        formatter={(val: number) => [`₹${val.toLocaleString('en-IN')} / ${trendData.unit}`, 'Modal Price']}
                        labelFormatter={(label) => `Date: ${label}`}
                      />
                      <Line
                        type="monotone"
                        dataKey="modalPrice"
                        stroke="#10b981"
                        strokeWidth={3}
                        dot={{ r: 4, fill: '#10b981', strokeWidth: 2, stroke: '#0f172a' }}
                        activeDot={{ r: 6, fill: '#34d399' }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <div className="p-8 text-center bg-slate-950 rounded-xl border border-slate-800 text-slate-400 space-y-2">
                  <AlertTriangle className="w-6 h-6 text-amber-400 mx-auto" />
                  <p className="text-xs font-medium">No verified observation points available to plot for the selected period.</p>
                </div>
              )}
            </section>

            {/* MODULE 11 AI EXPLANATION CARD */}
            {trendData.aiExplanation && (
              <section className="bg-slate-900/90 p-6 rounded-2xl border border-emerald-500/30 backdrop-blur-md space-y-5">
                <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                  <div className="flex items-center space-x-2">
                    <Sparkles className="w-5 h-5 text-emerald-400 animate-pulse" />
                    <h3 className="text-base font-bold text-white">AI Decision Support Summary</h3>
                  </div>
                  <div className="flex items-center space-x-2">
                    <span className="text-[11px] font-semibold text-slate-400 bg-slate-950 px-2.5 py-1 rounded-md border border-slate-800">
                      Engine: {trendData.aiExplanation.engine || 'RULE_BASED'}
                    </span>
                    <span className="text-[11px] font-semibold text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-md border border-emerald-500/30">
                      Confidence: {trendData.aiExplanation.confidence || 'MEDIUM'}
                    </span>
                  </div>
                </div>

                <div className="space-y-4">
                  {/* Summary */}
                  <div>
                    <h4 className="text-xs font-bold uppercase text-slate-400 tracking-wider mb-1">Executive Analysis</h4>
                    <p className="text-sm text-slate-200 font-medium leading-relaxed bg-slate-950 p-3.5 rounded-xl border border-slate-800/80">
                      {trendData.aiExplanation.summary}
                    </p>
                  </div>

                  {/* Verified Facts */}
                  {trendData.aiExplanation.verifiedFacts && trendData.aiExplanation.verifiedFacts.length > 0 && (
                    <div>
                      <h4 className="text-xs font-bold uppercase text-slate-400 tracking-wider mb-1.5">Verified Statistics & Facts</h4>
                      <ul className="space-y-1.5">
                        {trendData.aiExplanation.verifiedFacts.map((fact, idx) => (
                          <li key={idx} className="flex items-start space-x-2 text-xs text-slate-300">
                            <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                            <span>{fact}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Risks & Limitations */}
                  {trendData.aiExplanation.risks && trendData.aiExplanation.risks.length > 0 && (
                    <div>
                      <h4 className="text-xs font-bold uppercase text-slate-400 tracking-wider mb-1.5">Mandi Dynamics & Risk Factors</h4>
                      <ul className="space-y-1.5">
                        {trendData.aiExplanation.risks.map((risk, idx) => (
                          <li key={idx} className="flex items-start space-x-2 text-xs text-amber-300/90">
                            <AlertTriangle className="w-4 h-4 text-amber-400 flex-shrink-0 mt-0.5" />
                            <span>{risk}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              </section>
            )}

          </div>
        )}

      </main>

      <Footer />
    </div>
  );
};

export default MarketTrendsPage;
