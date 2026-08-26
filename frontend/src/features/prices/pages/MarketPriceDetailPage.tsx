import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowLeft,
  TrendingUp,
  Store,
  MapPin,
  Calendar,
  Clock,
  ShieldCheck,
  Database,
  Layers,
  Info,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { GlassCard } from '../../../components/ui/GlassCard';
import { useMarketPrice } from '../hooks/useMarketPrices';
import { QualityStatusBadge } from '../components/QualityStatusBadge';
import { SOURCE_TYPE_LABELS } from '../types';

export const MarketPriceDetailPage: React.FC = () => {
  const { priceId } = useParams<{ priceId: string }>();
  const { data: price, isLoading, isError, refetch } = useMarketPrice(priceId);

  // Skeleton Loader State
  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 font-sans flex flex-col justify-between select-none">
        <Navbar />
        <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-16 space-y-6 w-full flex-grow">
          <div className="h-12 bg-slate-900/60 rounded-2xl animate-pulse border border-slate-800" />
          <div className="h-44 bg-slate-900/60 rounded-2xl animate-pulse border border-slate-800" />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="h-56 bg-slate-900/60 rounded-2xl animate-pulse border border-slate-800" />
            <div className="h-56 bg-slate-900/60 rounded-2xl animate-pulse border border-slate-800" />
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  // Error / Not Found State
  if (isError || !price) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 font-sans flex flex-col justify-between">
        <Navbar />
        <main className="flex-grow flex items-center justify-center p-6 pt-28">
          <GlassCard className="max-w-md w-full p-8 text-center border-rose-500/30 shadow-2xl space-y-4">
            <div className="w-12 h-12 rounded-full bg-rose-500/10 border border-rose-500/30 flex items-center justify-center mx-auto text-rose-400">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h2 className="text-xl font-bold text-white">Market Price Record Not Found</h2>
            <p className="text-xs text-slate-400">
              The requested price observation ID ({priceId}) could not be retrieved from the backend API.
            </p>
            <div className="flex items-center justify-center space-x-3 pt-2">
              <button
                onClick={() => refetch()}
                className="px-4 py-2.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 text-xs font-semibold rounded-xl flex items-center space-x-1.5 min-h-[44px]"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Retry</span>
              </button>
              <Link
                to="/market-prices"
                className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl shadow transition-colors min-h-[44px] flex items-center"
              >
                Back to Price Discovery
              </Link>
            </div>
          </GlassCard>
        </main>
        <Footer />
      </div>
    );
  }

  const priceVariance = price.maxPrice - price.minPrice;

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 font-sans flex flex-col justify-between select-none relative">
      <Navbar />

      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-20 space-y-8 w-full flex-grow relative z-10">
        
        {/* BACK NAVIGATION BUTTON */}
        <div>
          <Link
            to="/market-prices"
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors py-1"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Market Price Discovery</span>
          </Link>
        </div>

        {/* HERO BANNER */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="p-6 sm:p-8 rounded-3xl bg-slate-900/60 border border-slate-800 shadow-2xl backdrop-blur-xl space-y-4"
        >
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-4">
            <div className="space-y-1">
              <div className="flex items-center space-x-2">
                <QualityStatusBadge status={price.qualityStatus} />
                <span className="text-xs font-mono text-slate-500">Record ID: {price.id}</span>
              </div>
              <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight pt-1">
                {price.crop?.name || 'Commodity Price Record'}
              </h1>
              <p className="text-sm text-slate-400 flex items-center pt-0.5">
                <Store className="w-4 h-4 text-emerald-400 mr-1.5 shrink-0" />
                <span>{price.market?.name} • {price.market?.state}</span>
              </p>
            </div>

            <div className="flex flex-col items-start sm:items-end text-xs text-slate-400 shrink-0 space-y-1">
              <span className="flex items-center">
                <Calendar className="w-3.5 h-3.5 text-slate-500 mr-1.5" />
                Business Date: <strong className="text-white ml-1 font-mono">{price.priceDate}</strong>
              </span>
              <span className="flex items-center text-[11px]">
                <Clock className="w-3 h-3 text-slate-500 mr-1.5" />
                Observed: {new Date(price.observedAt).toLocaleString()}
              </span>
            </div>
          </div>

          {/* Development seed notice */}
          {price.source?.name?.includes('Development') && (
            <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs flex items-center space-x-2">
              <ShieldCheck className="w-4 h-4 shrink-0 text-amber-400" />
              <span>
                <strong>Development Reference Seed Record:</strong> This price observation is provided for development reference and is clearly marked as non-live reference telemetry.
              </span>
            </div>
          )}
        </motion.div>

        {/* PRIMARY PRICE FIGURES PANEL */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <GlassCard className="p-6 sm:p-8 border-emerald-500/30 shadow-2xl space-y-6">
            <h2 className="text-base font-bold text-white border-b border-slate-900 pb-3 flex items-center space-x-2">
              <TrendingUp className="w-4.5 h-4.5 text-emerald-400" />
              <span>Market Price Parameters ({price.currency})</span>
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              
              {/* MINIMUM PRICE */}
              <div className="p-5 rounded-2xl bg-slate-950/80 border border-slate-900 space-y-1 text-center">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">Minimum Price</span>
                <span className="text-2xl font-black text-slate-200 font-mono block pt-1">
                  ₹{price.minPrice.toLocaleString()}
                </span>
                <span className="text-[11px] text-slate-500 block">per {price.unit}</span>
              </div>

              {/* MODAL PRICE (HIGHLIGHTED CORE FIGURE) */}
              <div className="p-5 rounded-2xl bg-emerald-500/10 border-2 border-emerald-500/40 space-y-1 text-center shadow-lg shadow-emerald-500/10">
                <span className="text-xs font-extrabold text-emerald-400 uppercase tracking-wider block">Modal / Representative Rate</span>
                <span className="text-3xl font-black text-white font-mono block pt-1">
                  ₹{price.modalPrice.toLocaleString()}
                </span>
                <span className="text-xs font-semibold text-emerald-300 block">per {price.unit}</span>
              </div>

              {/* MAXIMUM PRICE */}
              <div className="p-5 rounded-2xl bg-slate-950/80 border border-slate-900 space-y-1 text-center">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">Maximum Price</span>
                <span className="text-2xl font-black text-slate-200 font-mono block pt-1">
                  ₹{price.maxPrice.toLocaleString()}
                </span>
                <span className="text-[11px] text-slate-500 block">per {price.unit}</span>
              </div>

            </div>

            {/* Price Spread Bar */}
            <div className="pt-2 border-t border-slate-900 flex items-center justify-between text-xs text-slate-400">
              <span className="flex items-center">
                <Info className="w-3.5 h-3.5 text-slate-500 mr-1.5" />
                Price Spread (Max ─ Min):
              </span>
              <span className="font-mono font-bold text-slate-200">
                ₹{priceVariance.toLocaleString()} / {price.unit}
              </span>
            </div>
          </GlassCard>
        </motion.div>

        {/* 2-COLUMN DETAIL BREAKDOWN GRID */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          
          {/* LEFT: MARKET & LOCATION DETAILS */}
          <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: 0.15 }}
          >
            <GlassCard className="p-6 border-slate-800 shadow-2xl space-y-4 h-full flex flex-col justify-between">
              <div className="space-y-4">
                <h3 className="text-base font-bold text-white border-b border-slate-900 pb-3 flex items-center space-x-2">
                  <Store className="w-4.5 h-4.5 text-emerald-400" />
                  <span>Market Master Identity</span>
                </h3>

                <div className="space-y-3 text-xs">
                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                    <span className="text-slate-400 font-medium block">Market Name</span>
                    <strong className="text-sm text-white block">{price.market?.name}</strong>
                    <span className="text-[10px] font-mono text-slate-500 uppercase">Code: {price.market?.code}</span>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-900">
                      <span className="text-[10px] text-slate-500 block">Market Type</span>
                      <strong className="text-xs text-slate-200">{price.market?.type}</strong>
                    </div>

                    <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-900">
                      <span className="text-[10px] text-slate-500 block">Status</span>
                      <strong className="text-xs text-emerald-400">{price.market?.status}</strong>
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                    <span className="text-slate-400 font-medium flex items-center">
                      <MapPin className="w-3.5 h-3.5 text-amber-400 mr-1.5" /> Regional Location
                    </span>
                    <span className="text-slate-200 font-semibold block pt-0.5">
                      {price.market?.mandal}, {price.market?.district} • {price.market?.state}
                    </span>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-slate-900">
                <Link
                  to={`/markets/${price.market?.id}`}
                  className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 text-xs font-semibold flex items-center justify-between border border-slate-800 transition-colors"
                >
                  <span>View Full Market Directory Profile</span>
                  <ArrowLeft className="w-3.5 h-3.5 rotate-180 text-emerald-400" />
                </Link>
              </div>
            </GlassCard>
          </motion.div>

          {/* RIGHT: SOURCE & DATA QUALITY DETAILS */}
          <motion.div
            initial={{ opacity: 0, x: 10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: 0.2 }}
          >
            <GlassCard className="p-6 border-slate-800 shadow-2xl space-y-4 h-full flex flex-col justify-between">
              <div className="space-y-4">
                <h3 className="text-base font-bold text-white border-b border-slate-900 pb-3 flex items-center space-x-2">
                  <Database className="w-4.5 h-4.5 text-emerald-400" />
                  <span>Source Metadata & Integrity</span>
                </h3>

                <div className="space-y-3 text-xs">
                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                    <span className="text-slate-400 font-medium block">Source Classification</span>
                    <span className="text-xs font-bold text-emerald-400 block">
                      {SOURCE_TYPE_LABELS[price.source?.type] || price.source?.type}
                    </span>
                  </div>

                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                    <span className="text-slate-400 font-medium block">Data Origin Entity</span>
                    <strong className="text-xs text-white block">{price.source?.name}</strong>
                    {price.source?.reference && (
                      <span className="text-[10px] font-mono text-slate-500 block pt-0.5">
                        Ref: {price.source.reference}
                      </span>
                    )}
                  </div>

                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                    <span className="text-slate-400 font-medium flex items-center">
                      <Layers className="w-3.5 h-3.5 text-purple-400 mr-1.5" /> Commodity Taxonomy
                    </span>
                    <span className="text-slate-200 font-semibold block pt-0.5">
                      {price.crop?.name} ({price.crop?.category})
                    </span>
                    {price.crop?.scientificName && (
                      <span className="text-[10px] italic text-slate-400 block">{price.crop.scientificName}</span>
                    )}
                  </div>
                </div>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-950/90 border border-slate-900 text-[11px] text-slate-400 space-y-1">
                <span className="font-semibold text-slate-300 block">Production Data Quality Policy:</span>
                <p className="leading-relaxed">
                  Market prices are platform reference data. Client applications cannot modify trusted price records.
                </p>
              </div>
            </GlassCard>
          </motion.div>

        </div>

      </main>

      <Footer />
    </div>
  );
};

export default MarketPriceDetailPage;
