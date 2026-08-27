import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  TrendingUp,
  Filter,
  ArrowRight,
  ShieldCheck,
  RefreshCw,
  Calendar,
  Store,
  Database,
  Info,
} from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { useMarketPrices } from '../hooks/useMarketPrices';
import { useLocationCascade } from '../../markets/hooks/useLocationCascade';
import { useMarkets } from '../../markets/hooks/useMarkets';
import { useCrops } from '../hooks/useCrops';
import { MarketPriceFilterBar } from '../components/MarketPriceFilterBar';
import { MarketPriceFilterDrawer } from '../components/MarketPriceFilterDrawer';
import { QualityStatusBadge } from '../components/QualityStatusBadge';
import { DataAvailabilityBadge } from '../../../components/common/DataAvailabilityBadge';
import type { MarketPriceFilterState } from '../types';

export const MarketPriceListPage: React.FC = () => {
  const [filters, setFilters] = useState<MarketPriceFilterState>({
    unit: 'QUINTAL'
  });
  const [isDrawerOpen, setIsDrawerOpen] = useState<boolean>(false);

  // Price records query
  const { data: prices, isLoading: isPricesLoading, isError, refetch } = useMarketPrices(filters);

  // Dynamic Location Cascade query (States & Districts)
  const {
    states: dynamicStates = [],
    districts: dynamicDistricts = [],
    isStatesLoading,
    isDistrictsLoading,
  } = useLocationCascade(filters.state, filters.district);

  // Dynamic Markets query (filtered by selected State / District)
  const {
    data: rawMarkets = [],
    isLoading: isMarketsLoading,
  } = useMarkets({
    state: filters.state,
    district: filters.district,
  });

  const availableMarkets = rawMarkets.map((m) => ({ id: m.id, name: m.name }));

  // Dynamic Crops query (All master agricultural crops)
  const {
    data: rawCrops = [],
    isLoading: isCropsLoading,
  } = useCrops();

  const availableCrops = rawCrops.map((c) => ({ id: c.id, name: c.name }));

  const hasActiveFilters = Boolean(
    filters.state ||
    filters.district ||
    filters.marketId ||
    filters.cropId ||
    filters.qualityStatus ||
    filters.priceDate ||
    filters.fromDate ||
    filters.toDate ||
    (filters.unit && filters.unit !== 'QUINTAL')
  );

  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between overflow-x-hidden font-sans select-none">
      <Navbar />

      <main className="w-full flex-grow pt-[calc(env(safe-area-inset-top)+5.5rem)] pb-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto space-y-8">
        
        {/* HERO BANNER SECTION */}
        <section className="relative rounded-3xl bg-slate-900/60 border border-slate-800 p-6 sm:p-10 overflow-hidden shadow-2xl backdrop-blur-xl">
          <div className="relative z-10 max-w-3xl space-y-3 sm:space-y-4">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold uppercase tracking-wider">
              <TrendingUp className="w-3.5 h-3.5" />
              <span>Real Market Price Data Engine</span>
            </div>

            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight leading-tight">
              Agricultural <span className="text-emerald-400">Market Price Discovery</span>
            </h1>

            <p className="text-sm sm:text-base text-slate-300 leading-relaxed">
              Explore authentic wholesale mandi rates, price ranges, and normalized unit conversions (KG, QUINTAL, TONNE) with original AGMARKNET observation preservation.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-3 text-xs font-medium">
              <span className="px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center space-x-1.5">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>AGMARKNET Official Source Observation</span>
              </span>
              <span className="px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-800 text-slate-400 font-mono">
                Currency: INR (₹) • Display Unit: {filters.unit || 'QUINTAL'}
              </span>
            </div>
          </div>
        </section>

        {/* FILTER BAR SECTION */}
        <section className="w-full">
          {/* Mobile Filter Trigger Button */}
          <div className="xl:hidden flex items-center justify-between bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800 backdrop-blur-md">
            <div className="flex items-center space-x-2">
              <Filter className="w-4 h-4 text-emerald-400" />
              <span className="text-xs font-semibold text-slate-200 uppercase tracking-wider">Filter Market Prices</span>
            </div>
            <button
              type="button"
              onClick={() => setIsDrawerOpen(true)}
              className="min-h-[44px] px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs flex items-center space-x-1.5 shadow-md shadow-emerald-950/50"
            >
              <span>Filters</span>
              {hasActiveFilters && (
                <span className="w-2 h-2 rounded-full bg-amber-400" />
              )}
            </button>
          </div>

          {/* Desktop Filter Bar */}
          <MarketPriceFilterBar
            filters={filters}
            onFilterChange={(f) => setFilters(f)}
            availableStates={dynamicStates}
            availableDistricts={dynamicDistricts}
            availableCrops={availableCrops}
            availableMarkets={availableMarkets}
            isStatesLoading={isStatesLoading}
            isDistrictsLoading={isDistrictsLoading}
            isMarketsLoading={isMarketsLoading}
            isCropsLoading={isCropsLoading}
          />
        </section>

        {/* RESULTS SECTION */}
        <section className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 px-1">
            <div className="flex items-center space-x-3">
              <h2 className="text-lg sm:text-xl font-bold text-white flex items-center space-x-2">
                <span>Market Price Records</span>
                {prices && <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">{prices.length}</span>}
              </h2>
              {/* Telemetry Status Badge */}
              <DataAvailabilityBadge
                status={
                  isPricesLoading
                    ? 'LOADING'
                    : isError
                    ? 'TEMPORARILY_UNAVAILABLE'
                    : prices && prices.length > 0
                    ? 'REAL_DATA'
                    : 'NO_DATA'
                }
                recordCount={prices?.length || 0}
                onRetry={() => refetch()}
              />
            </div>

            {hasActiveFilters && (
              <button
                type="button"
                onClick={() => setFilters({ unit: 'QUINTAL' })}
                className="text-xs text-slate-400 hover:text-emerald-400 transition-colors flex items-center space-x-1 self-start sm:self-auto"
              >
                <RefreshCw className="w-3 h-3" />
                <span>Clear Filters</span>
              </button>
            )}
          </div>

          {/* Loading State Skeleton */}
          {isPricesLoading && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {[1, 2, 3, 4, 5, 6].map((n) => (
                <div key={n} className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800 animate-pulse space-y-4">
                  <div className="h-4 bg-slate-800 rounded w-1/3" />
                  <div className="h-6 bg-slate-800 rounded w-3/4" />
                  <div className="h-4 bg-slate-800 rounded w-1/2" />
                </div>
              ))}
            </div>
          )}

          {/* Error State (HTTP 503 / Network Failure) */}
          {isError && (
            <div className="p-10 rounded-3xl bg-rose-950/40 border border-rose-800/50 text-center space-y-4 shadow-xl backdrop-blur-md">
              <div className="w-12 h-12 rounded-2xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-center mx-auto text-rose-400">
                <Database className="w-6 h-6" />
              </div>
              <div className="space-y-1 max-w-lg mx-auto">
                <h3 className="text-lg font-bold text-rose-200">Market data temporarily unavailable</h3>
                <p className="text-xs text-rose-300/80 leading-relaxed">
                  Live AGMARKNET telemetry has not reached FarmConnectPrices right now. Please try again shortly.
                </p>
              </div>
              <button
                type="button"
                onClick={() => refetch()}
                className="px-6 py-2.5 min-h-[44px] rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold shadow-lg shadow-rose-950/60 transition-colors inline-flex items-center space-x-2"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Retry</span>
              </button>
            </div>
          )}

          {/* Empty State (HTTP 200 with 0 Records) */}
          {!isPricesLoading && !isError && prices?.length === 0 && (
            <div className="p-12 rounded-3xl bg-slate-900/40 border border-slate-800 text-center space-y-3">
              <TrendingUp className="w-10 h-10 text-slate-600 mx-auto" />
              <h3 className="text-base font-bold text-white">No market data available</h3>
              <p className="text-xs text-slate-400 max-w-md mx-auto">
                No AGMARKNET observations were found for the selected filters.
              </p>
              {hasActiveFilters && (
                <button
                  type="button"
                  onClick={() => setFilters({ unit: 'QUINTAL' })}
                  className="mt-2 px-5 py-2.5 min-h-[48px] rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-semibold text-xs inline-flex items-center space-x-1.5"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Reset All Filters</span>
                </button>
              )}
            </div>
          )}

          {/* Price Cards Grid */}
          {!isPricesLoading && !isError && prices && prices.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {prices.map((p) => (
                <motion.div
                  key={p.id}
                  whileHover={{ y: -3 }}
                  transition={{ duration: 0.2 }}
                  className="bg-slate-900/70 border border-slate-800 hover:border-emerald-500/40 p-5 rounded-2xl flex flex-col justify-between space-y-4 backdrop-blur-md shadow-lg"
                >
                  <div className="space-y-3">
                    {/* Header: Quality Badge & Date */}
                    <div className="flex items-center justify-between">
                      <QualityStatusBadge status={p.qualityStatus} />
                      <span className="text-[11px] font-mono text-slate-400 flex items-center">
                        <Calendar className="w-3 h-3 mr-1 text-slate-500" />
                        {p.priceDate}
                      </span>
                    </div>

                    {/* Commodity Name & Category */}
                    <div>
                      <div className="flex items-center justify-between">
                        <h3 className="text-lg font-bold text-white tracking-tight">{p.cropName}</h3>
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-950 text-slate-400 border border-slate-800">
                          {p.unit}
                        </span>
                      </div>
                      <p className="text-xs text-slate-400 flex items-center pt-1">
                        <Store className="w-3.5 h-3.5 text-emerald-400 mr-1 shrink-0" />
                        <span>{p.marketName}</span>
                      </p>
                    </div>

                    {/* Price Figures Display */}
                    <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-2">
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-slate-400 font-medium">Modal Rate:</span>
                        <strong className="text-base font-extrabold text-emerald-400 font-mono">
                          ₹{p.modalPrice.toLocaleString()} <span className="text-[10px] text-slate-500 font-normal">/ {p.unit}</span>
                        </strong>
                      </div>

                      <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1 border-t border-slate-900">
                        <span>Range (Min ─ Max):</span>
                        <span className="font-mono text-slate-300">
                          ₹{p.minPrice.toLocaleString()} ─ ₹{p.maxPrice.toLocaleString()}
                        </span>
                      </div>

                      {/* Source Conversion Transparency Note */}
                      {p.conversionApplied && (
                        <div className="text-[10px] text-amber-400 font-mono bg-amber-500/10 border border-amber-500/20 p-2 rounded-lg flex items-center space-x-1.5 mt-2">
                          <Info className="w-3.5 h-3.5 shrink-0 text-amber-400" />
                          <span>Converted from AGMARKNET ₹{((p.modalPrice / (p.conversionFactor || 1))).toLocaleString()} / {p.sourceUnit || 'QUINTAL'}</span>
                        </div>
                      )}
                    </div>

                    {/* Source Metadata */}
                    <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1">
                      <span className="flex items-center text-slate-500">
                        <Database className="w-3 h-3 mr-1 text-slate-500" /> Source:
                      </span>
                      <span className="font-semibold text-slate-300 truncate max-w-[170px]">
                        {p.sourceName}
                      </span>
                    </div>
                  </div>

                  {/* View Details CTA */}
                  <Link
                    to={`/market-prices/${p.id}`}
                    className="w-full min-h-[48px] py-2.5 px-4 rounded-xl bg-slate-950 hover:bg-emerald-600 text-slate-200 hover:text-white font-semibold text-xs flex items-center justify-between transition-all border border-slate-800 hover:border-emerald-500/50 group"
                  >
                    <span>View Full Price Breakdown</span>
                    <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-white group-hover:translate-x-1 transition-all" />
                  </Link>
                </motion.div>
              ))}
            </div>
          )}
        </section>
      </main>

      {/* Mobile Drawer */}
      <MarketPriceFilterDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        filters={filters}
        onApply={(f) => setFilters(f)}
        onReset={() => setFilters({ unit: 'QUINTAL' })}
        availableStates={dynamicStates}
        availableDistricts={dynamicDistricts}
        availableCrops={availableCrops}
        availableMarkets={availableMarkets}
      />

      <Footer />
    </div>
  );
};

export default MarketPriceListPage;

