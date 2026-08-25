import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight, Sparkles, ShieldCheck, Sprout, Store, ShoppingBag } from 'lucide-react';
import { VideoBackground } from '../components/VideoBackground';
import { Hero3DElements } from '../components/3d/Hero3DElements';
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
    <section
      id="hero"
      className="relative min-h-[92vh] sm:min-h-screen w-full flex items-center justify-center overflow-hidden bg-slate-950 pt-[calc(env(safe-area-inset-top)+5rem)] pb-[calc(env(safe-area-inset-bottom)+2.5rem)] select-none"
    >
      {/* LAYER 1 & 2: Background Video & Atmospheric Contrast Overlays (z-0, z-10) */}
      <VideoBackground videoSrc="/videos/farmconnectprices-hero.mp4" />

      {/* LAYER 3: 3D Data Environment Layer (z-20 - strictly behind text) */}
      <Hero3DElements isMobile={isMobile} />

      {/* LAYER 4: Clean Hero Content Layer (z-40 - foreground text & CTAs) */}
      <div className="relative z-40 max-w-4xl mx-auto px-5 sm:px-6 lg:px-8 w-full text-center flex flex-col items-center justify-between space-y-5 sm:space-y-7">
        
        {/* Animated Platform Badge */}
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="inline-flex items-center space-x-2 px-3.5 sm:px-4 py-1.5 rounded-full bg-slate-900/85 border border-emerald-500/30 text-emerald-300 text-[11px] sm:text-xs font-medium backdrop-blur-xl shadow-lg shadow-slate-950/80"
        >
          <Sparkles className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
          <span>Agricultural Market Intelligence Platform</span>
        </motion.div>

        {/* Hero Title & Tagline Hierarchy */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.15 }}
          className="space-y-2.5 sm:space-y-3.5 max-w-3xl"
        >
          <h1 className="text-[clamp(2.4rem,6.5vw,5.5rem)] font-black tracking-tight text-white drop-shadow-2xl leading-[1.1]">
            <span className="bg-gradient-to-r from-white via-slate-100 to-emerald-200 bg-clip-text text-transparent">
              {APP_NAME}
            </span>
          </h1>

          <p className="text-[clamp(1.15rem,3.2vw,2.2rem)] font-semibold text-emerald-400 drop-shadow-md leading-snug">
            "{APP_TAGLINE}"
          </p>

          <p className="text-xs sm:text-base text-slate-300 font-normal max-w-xl mx-auto pt-1 sm:pt-2 drop-shadow-sm leading-relaxed px-2">
            Connect farmers, markets, buyers and customers through intelligent agricultural market data.
          </p>
        </motion.div>

        {/* Primary & Secondary Hero CTAs */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.25 }}
          className="pt-2 sm:pt-4 flex flex-col sm:flex-row items-center justify-center gap-3.5 sm:gap-4 w-full max-w-[320px] sm:max-w-md"
        >
          <a
            href="#market-prices"
            className="w-full sm:w-auto min-h-[50px] px-7 py-3.5 bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white font-bold rounded-2xl shadow-lg shadow-emerald-950/80 hover:shadow-emerald-600/30 transition-all flex items-center justify-center space-x-2.5 text-sm sm:text-base group focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400"
          >
            <span>Explore Market Intelligence</span>
            <ArrowRight className="w-4 h-4 sm:w-5 sm:h-5 group-hover:translate-x-1 transition-transform" />
          </a>

          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="w-full sm:w-auto min-h-[50px] px-7 py-3.5 bg-slate-900/90 hover:bg-slate-800/90 text-slate-200 hover:text-white font-semibold rounded-2xl border border-slate-700/80 hover:border-slate-500 backdrop-blur-xl transition-all flex items-center justify-center text-sm sm:text-base focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400"
          >
            <span>{isAuthenticated ? 'Go to Dashboard' : 'Get Started'}</span>
          </Link>
        </motion.div>

        {/* Compact Three-Role Ecosystem Pipeline Indicator */}
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.35 }}
          className="pt-2 sm:pt-3 w-full max-w-xl"
        >
          <div className="py-2.5 px-4 sm:px-6 rounded-2xl bg-slate-900/80 border border-slate-800/90 backdrop-blur-md shadow-lg flex items-center justify-between text-xs text-slate-300">
            <div className="flex items-center gap-1.5 font-medium">
              <Sprout className="w-4 h-4 text-emerald-400 shrink-0" />
              <span className="font-semibold text-emerald-400">Farmer</span>
            </div>
            <span className="text-slate-600 font-bold text-xs">─</span>
            <div className="flex items-center gap-1.5 font-medium">
              <Store className="w-4 h-4 text-amber-400 shrink-0" />
              <span className="font-semibold text-amber-400">Mediator / Buyer</span>
            </div>
            <span className="text-slate-600 font-bold text-xs">─</span>
            <div className="flex items-center gap-1.5 font-medium">
              <ShoppingBag className="w-4 h-4 text-sky-400 shrink-0" />
              <span className="font-semibold text-sky-400">Customer</span>
            </div>
          </div>
        </motion.div>

        {/* Backend Health Status Indicator */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.7, delay: 0.45 }}
          className="pt-1"
        >
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900/80 border border-slate-800/90 text-[11px] backdrop-blur-md shadow-md">
            <ShieldCheck className="w-3.5 h-3.5 text-slate-400" />
            <span className="text-slate-400 font-medium">Backend API:</span>
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



