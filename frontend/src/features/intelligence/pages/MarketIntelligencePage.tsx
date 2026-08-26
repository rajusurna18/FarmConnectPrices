import React, { useState } from 'react';
import {
  TrendingUp, Minus, ShieldCheck,
  Building2, RefreshCw, AlertCircle, ArrowUpRight, ArrowDownRight, Calendar
} from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { useMarketComparison, useMarketIntelligenceSummary, usePriceTrends } from '../hooks/useMarketIntelligence';
import { useLocationCascade } from '../../markets/hooks/useLocationCascade';
import type { IntelligenceFilterState } from '../types';

export const MarketIntelligencePage: React.FC = () => {
  const [filters, setFilters] = useState<IntelligenceFilterState>({
    cropId: 'crop-chilli',
    unit: 'QUINTAL'
  });

  const { states, districts } = useLocationCascade(filters.state, filters.district);
  const { data: comparison, isLoading: isCompLoading, isError: isCompError, refetch: refetchComp } = useMarketComparison(filters);
  const { data: summary } = useMarketIntelligenceSummary(filters);
  const { data: trend } = usePriceTrends(filters);

  const cropsList = [
    { id: 'crop-chilli', name: 'Red Chilli' },
    { id: 'crop-paddy', name: 'Rice / Paddy' },
    { id: 'crop-tomato', name: 'Tomato' },
    { id: 'crop-cotton', name: 'Cotton' },
    { id: 'crop-turmeric', name: 'Turmeric' },
    { id: 'crop-maize', name: 'Maize' },
    { id: 'crop-onion', name: 'Onion' }
  ];

  const handleStateChange = (stateVal?: string) => {
    setFilters((prev) => ({
      ...prev,
      state: stateVal || undefined,
      district: undefined
    }));
  };

  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between overflow-x-hidden font-sans select-none">
      <Navbar />

      <main className="w-full flex-grow pt-[calc(env(safe-area-inset-top)+5.5rem)] pb-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto space-y-8">
        
        {/* HERO HEADER */}
        <section className="relative rounded-3xl bg-slate-900/60 border border-slate-800 p-6 sm:p-10 overflow-hidden shadow-2xl backdrop-blur-xl">
          <div className="relative z-20 max-w-3xl space-y-3">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold uppercase tracking-wider">
              <TrendingUp className="w-3.5 h-3.5" />
              <span>Market Intelligence Engine</span>
            </div>

            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight leading-tight">
              Transparent <span className="text-emerald-400">Market Price Comparison</span> & Intelligence
            </h1>

            <p className="text-sm sm:text-base text-slate-300 leading-relaxed">
              Compare reported crop prices across agricultural markets, analyze price spreads, and view verified historical trends with strict unit consistency.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-3 text-xs font-medium text-emerald-400">
              <span className="px-3 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center space-x-1.5">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Deterministic Calculations • Verified Data Policy Only</span>
              </span>
            </div>
          </div>
        </section>

        {/* FILTER BAR SECTION */}
        <section className="w-full bg-slate-900/80 p-4 rounded-2xl border border-slate-800 backdrop-blur-md grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Crop Filter */}
          <div>
            <label htmlFor="intel-crop-select" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
              Crop
            </label>
            <select
              id="intel-crop-select"
              name="cropId"
              value={filters.cropId || ''}
              onChange={(e) => setFilters((prev) => ({ ...prev, cropId: e.target.value || 'crop-chilli' }))}
              className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500"
            >
              {cropsList.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          {/* State Filter */}
          <div>
            <label htmlFor="intel-state-select" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
              State Scope
            </label>
            <select
              id="intel-state-select"
              name="state"
              value={filters.state || ''}
              onChange={(e) => handleStateChange(e.target.value)}
              className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="">All States</option>
              {states.map((st) => (
                <option key={st} value={st}>
                  {st}
                </option>
              ))}
            </select>
          </div>

          {/* District Filter */}
          <div>
            <label htmlFor="intel-district-select" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
              District Scope
            </label>
            <select
              id="intel-district-select"
              name="district"
              value={filters.district || ''}
              onChange={(e) => setFilters((prev) => ({ ...prev, district: e.target.value || undefined }))}
              disabled={!filters.state}
              className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500 disabled:opacity-50"
            >
              <option value="">All Districts</option>
              {districts.map((dist) => (
                <option key={dist} value={dist}>
                  {dist}
                </option>
              ))}
            </select>
          </div>

          {/* Unit Filter */}
          <div>
            <label htmlFor="intel-unit-select" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
              Unit
            </label>
            <select
              id="intel-unit-select"
              name="unit"
              value={filters.unit || 'QUINTAL'}
              onChange={(e) => setFilters((prev) => ({ ...prev, unit: e.target.value }))}
              className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="QUINTAL">₹ / QUINTAL</option>
              <option value="KG">₹ / KG</option>
              <option value="TONNE">₹ / TONNE</option>
            </select>
          </div>
        </section>

        {/* SUMMARY METRICS CARDS */}
        {summary && (
          <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Highest Price */}
            <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 backdrop-blur-md space-y-1">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Highest Reported Price</span>
              <div className="text-2xl font-extrabold text-white">
                ₹{comparison?.highestMarket?.modalPrice?.toLocaleString() || summary.maxObservedModalPrice.toLocaleString()}
                <span className="text-xs text-slate-400 font-normal"> / {filters.unit || 'QUINTAL'}</span>
              </div>
              <p className="text-xs text-emerald-400 font-medium truncate">
                {comparison?.highestMarket?.marketName || 'Market High'}
              </p>
            </div>

            {/* Lowest Price */}
            <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 backdrop-blur-md space-y-1">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Lowest Reported Price</span>
              <div className="text-2xl font-extrabold text-white">
                ₹{comparison?.lowestMarket?.modalPrice?.toLocaleString() || summary.minObservedModalPrice.toLocaleString()}
                <span className="text-xs text-slate-400 font-normal"> / {filters.unit || 'QUINTAL'}</span>
              </div>
              <p className="text-xs text-amber-400 font-medium truncate">
                {comparison?.lowestMarket?.marketName || 'Market Low'}
              </p>
            </div>

            {/* Price Spread */}
            <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 backdrop-blur-md space-y-1">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Price Spread (Diff)</span>
              <div className="text-2xl font-extrabold text-emerald-400">
                ₹{comparison?.priceDifference?.toLocaleString() || (summary.maxObservedModalPrice - summary.minObservedModalPrice).toLocaleString()}
              </div>
              {comparison?.percentageDifference != null && (
                <p className="text-xs text-slate-400">
                  {comparison.percentageDifference.toFixed(1)}% market variation
                </p>
              )}
            </div>

            {/* Trend Direction */}
            <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 backdrop-blur-md space-y-1">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Price Trend Direction</span>
              <div className="flex items-center space-x-2 pt-0.5">
                {summary.trendDirection === 'INCREASING' && (
                  <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-bold">
                    <ArrowUpRight className="w-4 h-4 mr-1" /> Increasing
                  </span>
                )}
                {summary.trendDirection === 'DECREASING' && (
                  <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20 text-xs font-bold">
                    <ArrowDownRight className="w-4 h-4 mr-1" /> Decreasing
                  </span>
                )}
                {(summary.trendDirection === 'STABLE' || summary.trendDirection === 'INSUFFICIENT_DATA') && (
                  <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-slate-800 text-slate-300 text-xs font-bold">
                    <Minus className="w-4 h-4 mr-1" /> {summary.trendDirection === 'STABLE' ? 'Stable' : 'Insufficient Data'}
                  </span>
                )}
              </div>
              {summary.latestObservationDate && (
                <p className="text-[11px] text-slate-500 flex items-center pt-1">
                  <Calendar className="w-3 h-3 mr-1" />
                  <span>Latest Verified: {summary.latestObservationDate}</span>
                </p>
              )}
            </div>
          </section>
        )}

        {/* MARKET COMPARISON TABLE SECTION */}
        <section className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <h2 className="text-lg sm:text-xl font-bold text-white flex items-center space-x-2">
              <Building2 className="w-5 h-5 text-emerald-400" />
              <span>Cross-Market Price Ranking ({filters.unit || 'QUINTAL'})</span>
            </h2>
            <button
              type="button"
              onClick={() => refetchComp()}
              className="text-xs text-slate-400 hover:text-emerald-400 transition-colors flex items-center space-x-1"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Refresh</span>
            </button>
          </div>

          {/* Loading Skeleton */}
          {isCompLoading && (
            <div className="p-8 rounded-2xl bg-slate-900/60 border border-slate-800 text-center animate-pulse">
              <div className="h-6 bg-slate-800 rounded w-1/3 mx-auto mb-4" />
              <div className="h-4 bg-slate-800 rounded w-1/2 mx-auto" />
            </div>
          )}

          {/* Error State */}
          {isCompError && (
            <div className="p-8 rounded-2xl bg-rose-950/30 border border-rose-800/40 text-center space-y-3">
              <AlertCircle className="w-8 h-8 text-rose-400 mx-auto" />
              <p className="text-sm text-rose-300">Failed to load market price comparison data.</p>
            </div>
          )}

          {/* Comparison Table */}
          {!isCompLoading && !isCompError && comparison && (
            <div className="overflow-x-auto rounded-2xl border border-slate-800 bg-slate-900/70 backdrop-blur-md">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/80 text-slate-400 uppercase text-[10px] font-mono border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-3">Rank</th>
                    <th className="px-4 py-3">Market</th>
                    <th className="px-4 py-3">Location</th>
                    <th className="px-4 py-3">Min Price</th>
                    <th className="px-4 py-3">Modal Price (Rank Metric)</th>
                    <th className="px-4 py-3">Max Price</th>
                    <th className="px-4 py-3">Price Date</th>
                    <th className="px-4 py-3">Source</th>
                    <th className="px-4 py-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {comparison.markets.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="px-4 py-8 text-center text-slate-500">
                        No verified market prices available for the selected crop, location, or unit ({filters.unit || 'QUINTAL'}).
                      </td>
                    </tr>
                  ) : (
                    comparison.markets.map((m, idx) => (
                      <tr key={m.marketId} className="hover:bg-slate-800/30 transition-colors">
                        <td className="px-4 py-3.5 font-mono font-bold text-slate-400">#{idx + 1}</td>
                        <td className="px-4 py-3.5 font-bold text-white">{m.marketName}</td>
                        <td className="px-4 py-3.5 text-slate-400">{m.district ? `${m.district}, ` : ''}{m.state}</td>
                        <td className="px-4 py-3.5 font-mono text-slate-300">₹{m.minPrice.toLocaleString()}</td>
                        <td className="px-4 py-3.5 font-mono font-bold text-emerald-400 text-sm">
                          ₹{m.modalPrice.toLocaleString()}
                        </td>
                        <td className="px-4 py-3.5 font-mono text-slate-300">₹{m.maxPrice.toLocaleString()}</td>
                        <td className="px-4 py-3.5 text-slate-400">{m.priceDate}</td>
                        <td className="px-4 py-3.5 text-slate-400">{m.sourceName}</td>
                        <td className="px-4 py-3.5">
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                            VERIFIED
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* TIME SERIES TREND SECTION */}
        {trend && trend.points.length > 0 && (
          <section className="p-6 rounded-3xl bg-slate-900/60 border border-slate-800 backdrop-blur-xl space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-white">Historical Price Trend Analysis</h3>
                <p className="text-xs text-slate-400">Deterministic observation timeline for {trend.cropName}</p>
              </div>
              <span className="text-xs font-mono font-semibold px-3 py-1 rounded-full bg-slate-800 text-slate-300">
                {trend.points.length} Historical Points
              </span>
            </div>

            <div className="space-y-3 pt-2">
              {trend.points.map((pt) => (
                <div key={pt.priceDate} className="flex items-center justify-between p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs">
                  <div className="flex items-center space-x-3">
                    <Calendar className="w-4 h-4 text-emerald-400" />
                    <span className="font-mono text-white font-semibold">{pt.priceDate}</span>
                  </div>
                  <div className="flex items-center space-x-6 font-mono">
                    <span className="text-slate-400">Min: ₹{pt.minPrice.toLocaleString()}</span>
                    <span className="text-emerald-400 font-bold">Modal: ₹{pt.modalPrice.toLocaleString()}</span>
                    <span className="text-slate-400">Max: ₹{pt.maxPrice.toLocaleString()}</span>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}
      </main>

      <Footer />
    </div>
  );
};

export default MarketIntelligencePage;
