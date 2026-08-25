import React from 'react';
import { motion } from 'framer-motion';
import { TrendingUp, Sparkles, MapPin, Zap } from 'lucide-react';

interface HeroMarketCardsProps {
  isMobile?: boolean;
}

export const HeroMarketCards: React.FC<HeroMarketCardsProps> = ({ isMobile = false }) => {
  // Mobile layout simplifies cards to 1 key visual pill or compact stack
  if (isMobile) {
    return (
      <div className="w-full flex items-center justify-center px-4 pointer-events-none z-[3]">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.85 }}
          className="w-full max-w-[320px] px-3.5 py-2.5 rounded-2xl bg-slate-900/85 border border-emerald-500/30 backdrop-blur-xl shadow-xl flex items-center justify-between text-xs"
        >
          <div className="flex items-center gap-2.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <div className="text-left">
              <div className="text-[9px] font-bold text-emerald-400 uppercase tracking-wider flex items-center gap-1">
                <Sparkles className="w-2.5 h-2.5" /> Market Signal • Demo
              </div>
              <div className="text-xs font-bold text-slate-100 mt-0.5">Tomato ₹2,850 / Qtl</div>
            </div>
          </div>
          <div className="text-right">
            <span className="text-[11px] font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20 inline-flex items-center gap-0.5">
              <TrendingUp className="w-3 h-3 text-emerald-400" /> +8.4%
            </span>
            <div className="text-[10px] text-slate-400 mt-0.5">Hyderabad</div>
          </div>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="absolute inset-0 w-full h-full pointer-events-none overflow-hidden z-[3]">
      {/* Card 1: Top-Left Floating Crop Card */}
      <motion.div
        initial={{ opacity: 0, x: -30, y: 20 }}
        animate={{ opacity: 1, x: 0, y: [0, -8, 0] }}
        transition={{
          opacity: { duration: 0.8, delay: 0.3 },
          x: { duration: 0.8, delay: 0.3 },
          y: { duration: 5, repeat: Infinity, ease: 'easeInOut' }
        }}
        className="absolute top-24 left-4 lg:left-12 max-w-[240px] p-4 rounded-2xl bg-slate-900/75 border border-emerald-500/20 backdrop-blur-xl shadow-xl shadow-slate-950/60"
      >
        <div className="flex items-center justify-between text-xs text-slate-400 font-medium mb-1">
          <span className="flex items-center gap-1 text-emerald-400 font-semibold uppercase tracking-wider text-[10px]">
            <Sparkles className="w-3 h-3 text-emerald-400" /> Visual Demo Signal
          </span>
          <span className="flex items-center gap-1 text-slate-400">
            <MapPin className="w-3 h-3 text-emerald-400" /> Hyd
          </span>
        </div>
        <div className="flex items-baseline justify-between mt-1">
          <h4 className="text-sm font-bold text-white tracking-wide">TOMATO</h4>
          <span className="text-xs font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20 flex items-center gap-0.5">
            <TrendingUp className="w-3 h-3" /> +8.4%
          </span>
        </div>
        <div className="text-lg font-extrabold text-slate-100 mt-1">
          ₹2,850 <span className="text-xs font-normal text-slate-400">/ Quintal</span>
        </div>
        <div className="text-[10px] text-slate-400 mt-1 flex items-center gap-1">
          <span>Hyderabad Wholesale Mandi</span>
        </div>
      </motion.div>

      {/* Card 2: Top-Right Market Demand Card */}
      <motion.div
        initial={{ opacity: 0, x: 30, y: 10 }}
        animate={{ opacity: 1, x: 0, y: [0, 8, 0] }}
        transition={{
          opacity: { duration: 0.8, delay: 0.4 },
          x: { duration: 0.8, delay: 0.4 },
          y: { duration: 6, repeat: Infinity, ease: 'easeInOut' }
        }}
        className="absolute top-28 right-4 lg:right-12 max-w-[220px] p-4 rounded-2xl bg-slate-900/75 border border-amber-500/20 backdrop-blur-xl shadow-xl shadow-slate-950/60"
      >
        <div className="text-[10px] uppercase font-bold tracking-wider text-amber-400 flex items-center gap-1">
          <Zap className="w-3 h-3 text-amber-400" /> Market Demand
        </div>
        <div className="flex items-baseline justify-between mt-1.5">
          <span className="text-xl font-black text-white tracking-tight">HIGH</span>
          <span className="text-xs font-bold text-emerald-400 flex items-center gap-0.5">
            <TrendingUp className="w-3 h-3" /> ▲ 12.6%
          </span>
        </div>
        <div className="text-[10px] text-slate-400 mt-1">
          Aggregated Regional Buying Power
        </div>
      </motion.div>

      {/* Card 3: Bottom-Right AI Intelligence Signal */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: [0, -6, 0] }}
        transition={{
          opacity: { duration: 0.8, delay: 0.5 },
          y: { duration: 5.5, repeat: Infinity, ease: 'easeInOut' }
        }}
        className="absolute bottom-16 right-6 lg:right-16 hidden md:block max-w-[230px] p-3.5 rounded-2xl bg-slate-900/75 border border-teal-500/20 backdrop-blur-xl shadow-xl shadow-slate-950/60"
      >
        <div className="text-[10px] uppercase font-bold tracking-wider text-teal-400 flex items-center gap-1">
          <span className="w-1.5 h-1.5 rounded-full bg-teal-400 animate-ping" /> AI Market Signal
        </div>
        <div className="text-xs font-bold text-slate-200 mt-1">
          SELLING WINDOW: <span className="text-emerald-400 font-extrabold">OPTIMAL</span>
        </div>
        <div className="text-[10px] text-slate-400 mt-0.5">
          Highest buyer liquidity expected next 48h
        </div>
      </motion.div>
    </div>
  );
};
