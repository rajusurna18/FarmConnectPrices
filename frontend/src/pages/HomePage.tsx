import React from 'react';
import { Link } from 'react-router-dom';
import { APP_NAME, APP_TAGLINE } from '../constants/app';
import { useHealthStatus } from '../hooks/useHealthStatus';
import { useAuth } from '../features/auth/hooks/useAuth';
import { VideoBackground } from '../components/VideoBackground';

export const HomePage: React.FC = () => {
  const { data: healthData, isLoading, isError } = useHealthStatus();
  const { isAuthenticated } = useAuth();

  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between overflow-x-hidden select-none">
      {/* Animated Hero Video Background */}
      <VideoBackground videoSrc="/videos/farmconnectprices-hero.mp4" />

      {/* Top Header Navigation */}
      <header className="relative z-10 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-5 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <span className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white drop-shadow-md">
            <span className="text-emerald-400">Farm</span>Connect<span className="text-emerald-400">Prices</span>
          </span>
        </div>

        <nav className="flex items-center space-x-3 sm:space-x-4">
          {isAuthenticated ? (
            <>
              <Link
                to="/dashboard"
                className="px-4 py-2 text-xs sm:text-sm font-semibold rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-900/30 transition-all duration-200 motion-reduce:transition-none"
              >
                Dashboard →
              </Link>
              <Link
                to="/profile"
                className="px-3.5 py-2 text-xs sm:text-sm font-medium rounded-xl bg-slate-900/80 hover:bg-slate-800 text-slate-200 border border-slate-700/60 backdrop-blur-md transition-colors"
              >
                Profile
              </Link>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="px-4 py-2 text-xs sm:text-sm font-medium text-slate-200 hover:text-white bg-slate-900/60 hover:bg-slate-800/80 border border-slate-700/50 rounded-xl backdrop-blur-md transition-all"
              >
                Sign In
              </Link>
              <Link
                to="/register"
                className="px-4 py-2 text-xs sm:text-sm font-semibold rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-900/30 transition-all duration-200 motion-reduce:transition-none"
              >
                Get Started
              </Link>
            </>
          )}
        </nav>
      </header>

      {/* Main Hero Foreground Content */}
      <main className="relative z-10 max-w-4xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-12 sm:py-20 text-center flex flex-col items-center justify-center space-y-8 my-auto">
        {/* Brand Badge */}
        <div className="inline-flex items-center space-x-2 px-4 py-1.5 rounded-full bg-slate-900/80 border border-emerald-500/30 text-emerald-300 text-xs sm:text-sm font-medium backdrop-blur-md shadow-inner motion-reduce:animate-none animate-pulse">
          <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
          <span>Next-Gen Agricultural Intelligence</span>
        </div>

        {/* Primary Dominant Title */}
        <div className="space-y-4 max-w-3xl">
          <h1 className="text-4xl sm:text-6xl md:text-7xl font-extrabold tracking-tight text-white drop-shadow-lg leading-tight sm:leading-none">
            {APP_NAME}
          </h1>

          <p className="text-lg sm:text-2xl md:text-3xl text-slate-200 font-medium max-w-2xl mx-auto drop-shadow-md leading-relaxed">
            {APP_TAGLINE}
          </p>
        </div>

        {/* Action Buttons */}
        <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-4 w-full max-w-md px-4">
          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="w-full sm:w-auto min-h-[48px] px-8 py-3.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl shadow-xl shadow-emerald-950/50 hover:shadow-emerald-600/30 transition-all duration-200 motion-reduce:transition-none flex items-center justify-center space-x-2 text-base"
          >
            <span>Explore Market Platform</span>
            <span>→</span>
          </Link>
          <Link
            to="/login"
            className="w-full sm:w-auto min-h-[48px] px-6 py-3.5 bg-slate-900/80 hover:bg-slate-800 text-slate-200 font-medium rounded-xl border border-slate-700/80 backdrop-blur-md transition-all flex items-center justify-center text-base"
          >
            <span>Access Account</span>
          </Link>
        </div>

        {/* Backend API Health Status Indicator */}
        <div className="pt-6 sm:pt-10 flex flex-col items-center">
          <div className="inline-flex items-center gap-2.5 px-4 py-2.5 rounded-2xl bg-slate-900/80 border border-slate-800/80 text-xs sm:text-sm backdrop-blur-md shadow-lg">
            <span className="text-slate-400 font-medium">Backend API Health:</span>
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
              <span className="inline-flex items-center gap-1.5 text-emerald-400 font-bold">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
                {healthData.status} ({healthData.service})
              </span>
            )}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="relative z-10 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-6 text-center text-xs text-slate-500 font-medium">
        © {new Date().getFullYear()} FarmConnectPrices. Real Prices. Better Markets. Smarter Decisions.
      </footer>
    </div>
  );
};
