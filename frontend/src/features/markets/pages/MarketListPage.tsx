import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Store, Filter, MapPin, ArrowRight, ShieldCheck, RefreshCw, Layers, Info } from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { useMarkets } from '../hooks/useMarkets';
import { useLocationCascade } from '../hooks/useLocationCascade';
import { MarketFilterDrawer } from '../components/MarketFilterDrawer';
import { MARKET_TYPE_LABELS } from '../types';
import type { MarketFilterState } from '../types';
import { MarketNetworkVisual } from '../../../components/3d/MarketNetworkVisual';

export const MarketListPage: React.FC = () => {
  const [filters, setFilters] = useState<MarketFilterState>({});
  const [isDrawerOpen, setIsDrawerOpen] = useState<boolean>(false);
  const [isMobile, setIsMobile] = useState<boolean>(false);

  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  const { data: markets, isLoading, isError, refetch } = useMarkets(filters);

  // Dynamic location cascading via TanStack Query
  const { states, districts, areas } = useLocationCascade(filters.state, filters.district);

  // Check for district fallback condition
  const isMandalFiltered = Boolean(filters.mandal);
  const hasNoMandalResults = isMandalFiltered && (!markets || markets.length === 0);

  const handleStateChange = (stateVal?: string) => {
    setFilters({
      state: stateVal || undefined,
      district: undefined,
      mandal: undefined,
      type: filters.type,
      cropId: filters.cropId,
    });
  };

  const handleDistrictChange = (districtVal?: string) => {
    setFilters((prev) => ({
      ...prev,
      district: districtVal || undefined,
      mandal: undefined,
    }));
  };

  const cropsList = [
    { id: 'crop-paddy', name: 'Rice / Paddy' },
    { id: 'crop-chilli', name: 'Red Chilli' },
    { id: 'crop-tomato', name: 'Tomato' },
    { id: 'crop-cotton', name: 'Cotton' },
    { id: 'crop-turmeric', name: 'Turmeric' },
    { id: 'crop-maize', name: 'Maize' },
    { id: 'crop-onion', name: 'Onion' }
  ];

  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between overflow-x-hidden font-sans select-none">
      {/* Navigation */}
      <Navbar />

      {/* Main Content Container */}
      <main className="w-full flex-grow pt-[calc(env(safe-area-inset-top)+5.5rem)] pb-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto space-y-8">
        
        {/* HERO BANNER SECTION */}
        <section className="relative rounded-3xl bg-slate-900/60 border border-slate-800 p-6 sm:p-10 overflow-hidden shadow-2xl backdrop-blur-xl">
          {/* Subtle 3D Background Visual */}
          <MarketNetworkVisual isMobile={isMobile} />

          <div className="relative z-20 max-w-3xl space-y-3 sm:space-y-4">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold uppercase tracking-wider">
              <Store className="w-3.5 h-3.5" />
              <span>Market Master Directory</span>
            </div>

            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight leading-tight">
              Explore Recognized <span className="text-emerald-400">Agricultural Markets</span>
            </h1>

            <p className="text-sm sm:text-base text-slate-300 leading-relaxed">
              Discover official agricultural trading yards, mandis, and wholesale centers with location-cascading discovery.
            </p>

            {/* Platform Source Notice */}
            <div className="pt-2 flex flex-wrap items-center gap-3 text-xs text-emerald-400/90 font-medium">
              <span className="px-3 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center space-x-1.5">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>AGMARKNET Source Dataset 35985678-0d79-46b4-9ed6-6f13308a1d24</span>
              </span>
            </div>
          </div>
        </section>

        {/* FILTER BAR SECTION */}
        <section className="w-full">
          {/* Mobile Filter Trigger Button */}
          <div className="md:hidden flex items-center justify-between bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800 backdrop-blur-md">
            <div className="flex items-center space-x-2">
              <Filter className="w-4 h-4 text-emerald-400" />
              <span className="text-xs font-semibold text-slate-200 uppercase tracking-wider">Filter Markets</span>
            </div>
            <button
              onClick={() => setIsDrawerOpen(true)}
              className="min-h-[44px] px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs flex items-center space-x-1.5 shadow-md shadow-emerald-950/50"
            >
              <span>Filters</span>
              {(filters.state || filters.district || filters.mandal || filters.type || filters.cropId) && (
                <span className="w-2 h-2 rounded-full bg-amber-400" />
              )}
            </button>
          </div>

          {/* Desktop Inline Filter Bar with Cascading Support */}
          <div className="hidden md:grid grid-cols-5 gap-3 bg-slate-900/80 p-4 rounded-2xl border border-slate-800 backdrop-blur-md">
            {/* State Filter */}
            <div>
              <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">State</label>
              <select
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
              <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">District</label>
              <select
                value={filters.district || ''}
                onChange={(e) => handleDistrictChange(e.target.value)}
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

            {/* Mandal / Area Filter (Conditional render) */}
            <div>
              <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Mandal / Area</label>
              {areas && areas.length > 0 ? (
                <select
                  value={filters.mandal || ''}
                  onChange={(e) => setFilters((prev) => ({ ...prev, mandal: e.target.value || undefined }))}
                  disabled={!filters.district}
                  className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500 disabled:opacity-50"
                >
                  <option value="">All Areas</option>
                  {areas.map((area) => (
                    <option key={area} value={area}>
                      {area}
                    </option>
                  ))}
                </select>
              ) : (
                <div className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950/50 border border-slate-800/60 text-slate-500 text-xs flex items-center">
                  <span>Unavailable</span>
                </div>
              )}
            </div>

            {/* Market Type Filter */}
            <div>
              <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Market Type</label>
              <select
                value={filters.type || ''}
                onChange={(e) => setFilters((prev) => ({ ...prev, type: e.target.value || undefined }))}
                className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500"
              >
                <option value="">All Types</option>
                {Object.entries(MARKET_TYPE_LABELS).map(([val, label]) => (
                  <option key={val} value={val}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            {/* Crop Filter */}
            <div>
              <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Supported Crop</label>
              <select
                value={filters.cropId || ''}
                onChange={(e) => setFilters((prev) => ({ ...prev, cropId: e.target.value || undefined }))}
                className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs focus:outline-none focus:border-emerald-500"
              >
                <option value="">All Crops</option>
                {cropsList.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </section>

        {/* RESULTS SECTION */}
        <section className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <h2 className="text-lg sm:text-xl font-bold text-white flex items-center space-x-2">
              <span>Discovered Markets</span>
              {markets && <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">{markets.length}</span>}
            </h2>
            {(filters.state || filters.district || filters.mandal || filters.type || filters.cropId) && (
              <button
                onClick={() => setFilters({})}
                className="text-xs text-slate-400 hover:text-emerald-400 transition-colors flex items-center space-x-1"
              >
                <RefreshCw className="w-3 h-3" />
                <span>Clear Filters</span>
              </button>
            )}
          </div>

          {/* Mandal Availability Fallback Alert */}
          {hasNoMandalResults && (
            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs flex items-center space-x-3">
              <Info className="w-4 h-4 flex-shrink-0 text-amber-400" />
              <span>Mandal/Area-specific market data unavailable for the selected sub-area. Showing markets for the selected district.</span>
            </div>
          )}

          {/* Loading State Skeleton */}
          {isLoading && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {[1, 2, 3, 4].map((n) => (
                <div key={n} className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800 animate-pulse space-y-4">
                  <div className="h-4 bg-slate-800 rounded w-1/3" />
                  <div className="h-6 bg-slate-800 rounded w-3/4" />
                  <div className="h-4 bg-slate-800 rounded w-1/2" />
                </div>
              ))}
            </div>
          )}

          {/* Error State */}
          {isError && (
            <div className="p-8 rounded-2xl bg-rose-950/30 border border-rose-800/40 text-center space-y-3">
              <p className="text-sm text-rose-300">Failed to load agricultural market data.</p>
              <button
                onClick={() => refetch()}
                className="px-4 py-2 min-h-[44px] rounded-xl bg-slate-900 border border-slate-700 text-white text-xs font-semibold"
              >
                Retry
              </button>
            </div>
          )}

          {/* Empty State */}
          {!isLoading && !isError && markets?.length === 0 && (
            <div className="p-12 rounded-3xl bg-slate-900/40 border border-slate-800 text-center space-y-3">
              <Store className="w-10 h-10 text-slate-600 mx-auto" />
              <h3 className="text-base font-bold text-white">No markets found matching filters</h3>
              <p className="text-xs text-slate-400 max-w-md mx-auto">
                Try selecting a different district, market type, or crop filter to explore available trading centers.
              </p>
              <button
                onClick={() => setFilters({})}
                className="mt-2 px-5 py-2.5 min-h-[48px] rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-semibold text-xs inline-flex items-center space-x-1.5"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Reset All Filters</span>
              </button>
            </div>
          )}

          {/* Market Cards Grid */}
          {!isLoading && !isError && markets && markets.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {markets.map((m) => (
                <motion.div
                  key={m.id}
                  whileHover={{ y: -3 }}
                  transition={{ duration: 0.2 }}
                  className="bg-slate-900/70 border border-slate-800 hover:border-emerald-500/40 p-5 rounded-2xl flex flex-col justify-between space-y-4 backdrop-blur-md shadow-lg"
                >
                  <div className="space-y-2">
                    {/* Type Badge & Status */}
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        {MARKET_TYPE_LABELS[m.type] || m.type}
                      </span>
                      <span className="text-[11px] font-medium text-slate-400 flex items-center space-x-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                        <span>{m.status}</span>
                      </span>
                    </div>

                    {/* Market Name & Code */}
                    <div>
                      <h3 className="text-lg font-bold text-white tracking-tight">{m.name}</h3>
                      <span className="text-[11px] font-mono text-slate-500 uppercase">{m.code}</span>
                    </div>

                    {/* Location Info */}
                    <div className="flex items-center text-xs text-slate-300 pt-1">
                      <MapPin className="w-3.5 h-3.5 text-amber-400 mr-1.5 flex-shrink-0" />
                      <span>{m.mandal ? `${m.mandal}, ` : ''}{m.district} • {m.state}</span>
                    </div>

                    {/* Supported Crops Preview */}
                    <div className="flex items-center text-xs text-slate-400 pt-2 border-t border-slate-800/80">
                      <Layers className="w-3.5 h-3.5 text-slate-500 mr-1.5 flex-shrink-0" />
                      <span>{m.supportedCropCount} Supported Crops</span>
                    </div>
                  </div>

                  {/* View Details CTA */}
                  <Link
                    to={`/markets/${m.id}`}
                    className="w-full min-h-[48px] py-2.5 px-4 rounded-xl bg-slate-950 hover:bg-emerald-600 text-slate-200 hover:text-white font-semibold text-xs flex items-center justify-between transition-all border border-slate-800 hover:border-emerald-500/50 group"
                  >
                    <span>View Market Details</span>
                    <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-white group-hover:translate-x-1 transition-all" />
                  </Link>
                </motion.div>
              ))}
            </div>
          )}
        </section>
      </main>

      {/* Mobile Drawer */}
      <MarketFilterDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        filters={filters}
        onApply={(f) => setFilters(f)}
        onReset={() => setFilters({})}
        availableStates={states}
        availableDistricts={districts}
        availableAreas={areas}
        availableCrops={cropsList}
        onStateChange={handleStateChange}
        onDistrictChange={handleDistrictChange}
      />

      {/* Footer */}
      <Footer />
    </div>
  );
};

export default MarketListPage;
