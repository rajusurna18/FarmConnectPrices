import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight, Sparkles } from 'lucide-react';
import { VideoBackground } from '../components/VideoBackground';
import { Hero3DElements } from '../components/3d/Hero3DElements';
import { useHealthStatus } from '../hooks/useHealthStatus';
import { useAuth } from '../features/auth/hooks/useAuth';
import { APP_NAME, APP_TAGLINE } from '../constants/app';

export const HeroSection: React.FC = () => {
  const { data: healthData, isLoading, isError } = useHealthStatus();
  const { isAuthenticated } = useAuth();

  return (
    <section id="hero" className="relative min-h-screen w-full flex items-center justify-center overflow-hidden bg-slate-950 pt-20">
      {/* 1. Base Layer: Existing FarmConnectPrices Hero Video */}
      <VideoBackground videoSrc="/videos/farmconnectprices-hero.mp4" />

      {/* 2. 3D Floating Data Canvas Layer */}
      <Hero3DElements />

      {/* 3. Main Hero Content Foreground Layer */}
      <div className="relative z-10 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-16 sm:py-24 text-center flex flex-col items-center space-y-8">
        
        {/* Animated Badge */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="inline-flex items-center space-x-2 px-4 py-1.5 rounded-full bg-slate-900/80 border border-emerald-500/30 text-emerald-300 text-xs sm:text-sm font-medium backdrop-blur-xl shadow-lg"
        >
          <Sparkles className="w-4 h-4 text-emerald-400 animate-pulse" />
          <span>Next-Gen 3D Market Intelligence Platform</span>
        </motion.div>

        {/* Primary Title */}
        <motion.div
          initial={{ opacity: 0, y: 25 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.1 }}
          className="space-y-4 max-w-4xl"
        >
          <h1 className="text-4xl sm:text-6xl md:text-7xl font-extrabold tracking-tight text-white drop-shadow-xl leading-tight">
            <span className="text-white">{APP_NAME}</span>
          </h1>

          <p className="text-xl sm:text-3xl font-medium text-emerald-400 drop-shadow-md">
            "{APP_TAGLINE}"
          </p>

          <p className="text-base sm:text-xl text-slate-300 font-normal max-w-2xl mx-auto pt-2 drop-shadow-sm leading-relaxed">
            The digital bridge connecting farms, agricultural markets, buyers and customers through real-time data & AI decision intelligence.
          </p>
        </motion.div>

        {/* Hero CTAs */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.2 }}
          className="pt-4 flex flex-col sm:flex-row items-center justify-center gap-4 w-full max-w-md"
        >
          <a
            href="#market-prices"
            className="w-full sm:w-auto min-h-[50px] px-8 py-3.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-2xl shadow-xl shadow-emerald-950/60 hover:shadow-emerald-600/30 transition-all flex items-center justify-center space-x-2.5 text-base group"
          >
            <span>Explore Market Prices</span>
            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </a>

          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="w-full sm:w-auto min-h-[50px] px-8 py-3.5 bg-slate-900/90 hover:bg-slate-800 text-slate-200 font-medium rounded-2xl border border-slate-700/80 backdrop-blur-xl transition-all flex items-center justify-center text-base"
          >
            <span>{isAuthenticated ? 'Go to Dashboard' : 'Get Started'}</span>
          </Link>
        </motion.div>

        {/* Backend Health Status Indicator */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.8, delay: 0.3 }}
          className="pt-6"
        >
          <div className="inline-flex items-center gap-2.5 px-4 py-2 rounded-full bg-slate-900/80 border border-slate-800 text-xs sm:text-sm backdrop-blur-md shadow-lg">
            <span className="text-slate-400 font-medium">Backend System:</span>
            {isLoading && (
              <span className="text-amber-400 font-medium flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-amber-400 animate-ping"></span>
                Connecting...
              </span>
            )}
            {isError && (
              <span className="text-rose-400 font-medium flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-rose-400"></span>
                Offline / Error
              </span>
            )}
            {healthData && (
              <span className="inline-flex items-center gap-1.5 text-emerald-400 font-semibold">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                Online ({healthData.service})
              </span>
            )}
          </div>
        </motion.div>

      </div>
    </section>
  );
};
