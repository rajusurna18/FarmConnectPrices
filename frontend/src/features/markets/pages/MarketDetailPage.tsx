import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, MapPin, Navigation, Sprout, Sparkles, CheckCircle2 } from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { useMarket, useMarketCrops } from '../hooks/useMarkets';
import { MARKET_TYPE_LABELS } from '../types';
import { MarketNetworkVisual } from '../../../components/3d/MarketNetworkVisual';

export const MarketDetailPage: React.FC = () => {
  const { marketId } = useParams<{ marketId: string }>();
  const { data: market, isLoading: isMarketLoading, isError: isMarketError } = useMarket(marketId);
  const { data: crops, isLoading: isCropsLoading } = useMarketCrops(marketId);
  const [isMobile, setIsMobile] = useState<boolean>(false);

  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between overflow-x-hidden font-sans select-none">
      {/* Navigation */}
      <Navbar />

      {/* Main Content */}
      <main className="w-full flex-grow pt-[calc(env(safe-area-inset-top)+5.5rem)] pb-16 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto space-y-6">
        
        {/* Navigation Top Bar */}
        <div>
          <Link
            to="/markets"
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors bg-slate-900/60 border border-slate-800 px-3.5 py-2.5 rounded-xl min-h-[44px]"
          >
            <ArrowLeft className="w-4 h-4 text-emerald-400" />
            <span>Back to Market Directory</span>
          </Link>
        </div>

        {/* Loading State */}
        {isMarketLoading && (
          <div className="bg-slate-900/60 p-8 rounded-3xl border border-slate-800 animate-pulse space-y-6">
            <div className="h-6 bg-slate-800 rounded w-1/4" />
            <div className="h-10 bg-slate-800 rounded w-2/3" />
            <div className="h-24 bg-slate-800 rounded w-full" />
          </div>
        )}

        {/* Error State */}
        {isMarketError && (
          <div className="p-10 rounded-3xl bg-rose-950/30 border border-rose-800/40 text-center space-y-4">
            <p className="text-base text-rose-300 font-bold">Agricultural market record not found.</p>
            <Link
              to="/markets"
              className="inline-block px-5 py-2.5 min-h-[48px] rounded-xl bg-slate-900 border border-slate-700 text-white font-semibold text-xs"
            >
              Return to Directory
            </Link>
          </div>
        )}

        {/* Market Details */}
        {!isMarketLoading && !isMarketError && market && (
          <div className="space-y-6">
            
            {/* HERO IDENTITY CARD */}
            <section className="relative rounded-3xl bg-slate-900/70 border border-slate-800 p-6 sm:p-8 overflow-hidden backdrop-blur-xl shadow-2xl space-y-4">
              <MarketNetworkVisual isMobile={isMobile} />

              <div className="relative z-20 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <span className="text-xs font-bold px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                    {MARKET_TYPE_LABELS[market.type] || market.type}
                  </span>
                  <span className="text-xs font-semibold px-3 py-1 rounded-full bg-slate-800 text-slate-300 flex items-center space-x-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    <span>Status: {market.status}</span>
                  </span>
                </div>

                <div>
                  <h1 className="text-2xl sm:text-4xl font-extrabold text-white tracking-tight">{market.name}</h1>
                  <span className="text-xs font-mono text-slate-500 tracking-wider">OFFICIAL CODE: {market.code}</span>
                </div>

                <div className="flex items-center text-xs sm:text-sm text-slate-300 pt-1">
                  <MapPin className="w-4 h-4 text-amber-400 mr-1.5 flex-shrink-0" />
                  <span>{market.location.mandal}, {market.location.district} • {market.location.state}</span>
                </div>
              </div>
            </section>

            {/* LOCATION & COORDINATES SPECIFICATION */}
            <section className="grid grid-cols-1 md:grid-cols-2 gap-5">
              {/* Location Hierarchy */}
              <div className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800 space-y-3">
                <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center space-x-2">
                  <MapPin className="w-4 h-4 text-emerald-400" />
                  <span>Location Master Hierarchy</span>
                </h3>
                <div className="space-y-2 text-xs sm:text-sm">
                  <div className="flex justify-between py-1 border-b border-slate-800/60">
                    <span className="text-slate-400">State:</span>
                    <strong className="text-white">{market.location.state || 'N/A'}</strong>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/60">
                    <span className="text-slate-400">District:</span>
                    <strong className="text-white">{market.location.district || 'N/A'}</strong>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/60">
                    <span className="text-slate-400">Mandal:</span>
                    <strong className="text-white">{market.location.mandal || 'N/A'}</strong>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/60">
                    <span className="text-slate-400">Village / Locality:</span>
                    <strong className="text-white">{market.location.village || 'N/A'}</strong>
                  </div>
                  <div className="flex justify-between py-1">
                    <span className="text-slate-400">Pincode:</span>
                    <strong className="text-amber-400 font-mono">{market.location.pincode || 'N/A'}</strong>
                  </div>
                </div>
              </div>

              {/* Geographic Coordinates */}
              <div className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800 space-y-3 flex flex-col justify-between">
                <div>
                  <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center space-x-2 mb-3">
                    <Navigation className="w-4 h-4 text-amber-400" />
                    <span>Geographic Telemetry</span>
                  </h3>
                  {market.latitude && market.longitude ? (
                    <div className="space-y-2 text-xs sm:text-sm">
                      <div className="flex justify-between py-1 border-b border-slate-800/60">
                        <span className="text-slate-400">Latitude:</span>
                        <strong className="text-white font-mono">{market.latitude}° N</strong>
                      </div>
                      <div className="flex justify-between py-1 border-b border-slate-800/60">
                        <span className="text-slate-400">Longitude:</span>
                        <strong className="text-white font-mono">{market.longitude}° E</strong>
                      </div>
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 italic py-2">Geographic coordinates pending GPS survey verification.</p>
                  )}
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800 text-xs text-slate-400 space-y-1">
                  <span className="font-semibold text-slate-200 block">Master Reference Authority</span>
                  <p className="text-[11px]">Official market location identifier verified under FarmConnectPrices master directory.</p>
                </div>
              </div>
            </section>

            {/* SUPPORTED CROPS LIST */}
            <section className="bg-slate-900/60 p-6 sm:p-8 rounded-2xl border border-slate-800 space-y-4">
              <div className="flex items-center space-x-2">
                <Sprout className="w-5 h-5 text-emerald-400" />
                <h3 className="text-lg font-bold text-white">Supported Agricultural Crops</h3>
              </div>

              {isCropsLoading && <p className="text-xs text-slate-400">Loading supported commodities...</p>}

              {!isCropsLoading && crops && crops.length > 0 && (
                <div className="flex flex-wrap gap-2.5">
                  {crops.map((c) => (
                    <div
                      key={c.id}
                      className="px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-semibold text-slate-200 flex items-center space-x-2"
                    >
                      <span className="w-2 h-2 rounded-full bg-emerald-400" />
                      <span>{c.cropName}</span>
                      <span className="text-[10px] text-slate-500 font-mono">({c.cropCategory})</span>
                    </div>
                  ))}
                </div>
              )}

              {!isCropsLoading && (!crops || crops.length === 0) && (
                <p className="text-xs text-slate-500 italic">No supported crops currently registered for this market.</p>
              )}
            </section>

            {/* MODULE 07 PRICING PLACEHOLDER (ZERO FAKE DATA) */}
            <section className="bg-slate-900/40 p-6 sm:p-8 rounded-3xl border border-emerald-500/20 text-center space-y-3 relative overflow-hidden">
              <div className="w-10 h-10 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-center mx-auto">
                <Sparkles className="w-5 h-5" />
              </div>
              <h3 className="text-lg font-bold text-white">Real Market Price Integration</h3>
              <p className="text-xs sm:text-sm text-slate-300 max-w-lg mx-auto leading-relaxed">
                Market prices, modal price trends, and daily arrival telemetry will be available in Module 07.
              </p>
              <div className="pt-2">
                <span className="inline-block text-[11px] font-semibold px-3 py-1 rounded-full bg-slate-950 border border-slate-800 text-slate-400">
                  Module 06 Foundation Established • No Fabricated Rates
                </span>
              </div>
            </section>

          </div>
        )}
      </main>

      {/* Footer */}
      <Footer />
    </div>
  );
};

export default MarketDetailPage;
