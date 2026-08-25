import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight, Sparkles, ShieldCheck } from 'lucide-react';
import { VideoBackground } from '../components/VideoBackground';
import { Hero3DElements } from '../components/3d/Hero3DElements';
import { HeroMarketCards } from '../components/HeroMarketCards';
import { useHealthStatus } from '../hooks/useHealthStatus';
import { useAuth } from '../features/auth/hooks/useAuth';
import { APP_NAME, APP_TAGLINE } from '../constants/app';

export const HeroSection: React.FC = () => {
  const { data: healthData, isLoading, isError } = useHealthStatus();
  const { isAuthenticated } = useAuth();
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
    <section id="hero" className="relative min-h-[92vh] sm:min-h-screen w-full flex items-center justify-center overflow-hidden bg-slate-950 pt-20 pb-12 sm:pb-16 select-none">
      {/* 1. Base Layer: Existing FarmConnectPrices Hero Video Background */}
      <VideoBackground videoSrc="/videos/farmconnectprices-hero.mp4" />

      {/* 2. Midground 3D Data Environment Layer */}
      <Hero3DElements isMobile={isMobile} />

      {/* 3. Foreground Floating Market Intelligence Cards */}
      <HeroMarketCards isMobile={isMobile} />

      {/* 4. Foreground Content & UI Layer */}
      <div className="relative z-10 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-20 text-center flex flex-col items-center space-y-6 sm:space-y-8">
        
        {/* Animated Platform Badge */}
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="inline-flex items-center space-x-2 px-3.5 sm:px-4 py-1.5 rounded-full bg-slate-900/85 border border-emerald-500/35 text-emerald-300 text-xs sm:text-sm font-medium backdrop-blur-xl shadow-lg shadow-slate-950/80"
        >
          <Sparkles className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-emerald-400 animate-pulse" />
          <span>Agricultural Market Intelligence Platform</span>
        </motion.div>

        {/* Hero Title & Tagline Hierarchy */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.1 }}
          className="space-y-3 sm:space-y-4 max-w-4xl"
        >
          <h1 className="text-4xl sm:text-6xl md:text-7xl font-black tracking-tight text-white drop-shadow-2xl leading-[1.15]">
            <span className="bg-gradient-to-r from-white via-slate-100 to-emerald-200 bg-clip-text text-transparent">
              {APP_NAME}
            </span>
          </h1>

          <p className="text-lg sm:text-3xl font-semibold text-emerald-400 drop-shadow-md">
            "{APP_TAGLINE}"
          </p>

          <p className="text-sm sm:text-lg text-slate-300 font-normal max-w-2xl mx-auto pt-1 sm:pt-2 drop-shadow-sm leading-relaxed px-2">
            Connect farmers, markets, buyers and customers through intelligent agricultural market data.
          </p>
        </motion.div>

        {/* Hero CTAs */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.2 }}
          className="pt-2 sm:pt-4 flex flex-col sm:flex-row items-center justify-center gap-3.5 sm:gap-4 w-full max-w-md"
        >
          <a
            href="#market-prices"
            className="w-full sm:w-auto min-h-[48px] sm:min-h-[52px] px-7 py-3.5 bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white font-bold rounded-2xl shadow-lg shadow-emerald-950/70 hover:shadow-emerald-600/30 transition-all flex items-center justify-center space-x-2.5 text-sm sm:text-base group focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400"
          >
            <span>Explore Market Intelligence</span>
            <ArrowRight className="w-4 h-4 sm:w-5 sm:h-5 group-hover:translate-x-1 transition-transform" />
          </a>

          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="w-full sm:w-auto min-h-[48px] sm:min-h-[52px] px-7 py-3.5 bg-slate-900/90 hover:bg-slate-800/90 text-slate-200 hover:text-white font-semibold rounded-2xl border border-slate-700/80 hover:border-slate-500 backdrop-blur-xl transition-all flex items-center justify-center text-sm sm:text-base focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400"
          >
            <span>{isAuthenticated ? 'Go to Dashboard' : 'Get Started'}</span>
          </Link>
        </motion.div>

        {/* 4-Step Role Pipeline Bar */}
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.3 }}
          className="pt-2 sm:pt-4 w-full max-w-2xl"
        >
          <div className="py-2.5 px-4 sm:px-6 rounded-2xl bg-slate-900/75 border border-slate-800/80 backdrop-blur-md shadow-lg flex items-center justify-between text-xs sm:text-sm text-slate-300">
            <div className="flex items-center gap-1.5 sm:gap-2 font-medium">
              <span>🌾</span>
              <span className="font-semibold text-emerald-400">Farmer</span>
            </div>
            <span className="text-slate-600 font-bold">→</span>
            <div className="flex items-center gap-1.5 sm:gap-2 font-medium">
              <span>📊</span>
              <span className="hidden sm:inline text-slate-200">Market Intelligence</span>
              <span className="sm:hidden text-slate-200">Intelligence</span>
            </div>
            <span className="text-slate-600 font-bold">→</span>
            <div className="flex items-center gap-1.5 sm:gap-2 font-medium">
              <span>🏪</span>
              <span className="font-semibold text-amber-400">Mediator / Buyer</span>
            </div>
            <span className="text-slate-600 font-bold">→</span>
            <div className="flex items-center gap-1.5 sm:gap-2 font-medium">
              <span>🛒</span>
              <span className="font-semibold text-sky-400">Customer</span>
            </div>
          </div>
        </motion.div>

        {/* Backend Health Status Indicator */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.8, delay: 0.4 }}
          className="pt-2"
        >
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-slate-900/80 border border-slate-800/90 text-xs backdrop-blur-md shadow-md">
            <ShieldCheck className="w-3.5 h-3.5 text-slate-400" />
            <span className="text-slate-400 font-medium">Backend System:</span>
            {isLoading && (
              <span className="text-amber-400 font-medium flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-amber-400 animate-ping" />
                Connecting...
              </span>
            )}
            {isError && (
              <span className="text-rose-400 font-medium flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-rose-400" />
                Offline / Error
              </span>
            )}
            {healthData && (
              <span className="inline-flex items-center gap-1.5 text-emerald-400 font-semibold">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                Online ({healthData.service})
              </span>
            )}
          </div>
        </motion.div>

      </div>
    </section>
  );
};

