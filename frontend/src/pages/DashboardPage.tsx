import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  LayoutDashboard,
  User,
  ShieldCheck,
  Cpu,
  RefreshCw,
  Store,
  ArrowRight,
  CheckCircle2,
  Award,
  Sprout,
  ShoppingBag,
  BrainCircuit,
  Sparkles,
  ChevronRight,
  Layers,
} from 'lucide-react';
import { useAuth } from '../features/auth/hooks/useAuth';
import { apiClient } from '../services/api';
import { GlassCard } from '../components/ui/GlassCard';
import { Navbar } from '../components/navigation/Navbar';
import { Footer } from '../components/navigation/Footer';

interface BackendUserMeResponse {
  uid: string;
  email: string;
  displayName: string;
  role: string;
  status: string;
}

export const DashboardPage: React.FC = () => {
  const { currentUser, userDocument } = useAuth();
  const [apiResponse, setApiResponse] = useState<BackendUserMeResponse | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [loadingApi, setLoadingApi] = useState(false);

  const fetchBackendUserMe = async () => {
    setLoadingApi(true);
    setApiError(null);
    setApiResponse(null);
    try {
      const res = await apiClient.get<BackendUserMeResponse>('/api/v1/users/me');
      setApiResponse(res.data);
    } catch (err: unknown) {
      console.error('[Dashboard] Error fetching /api/v1/users/me:', err);
      setApiError('Failed to fetch authenticated user profile from Spring Boot backend.');
    } finally {
      setLoadingApi(false);
    }
  };

  const displayName =
    userDocument?.displayName || currentUser?.displayName || currentUser?.email?.split('@')[0] || 'User';
  const roleName = (userDocument?.role as string) || 'USER';

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 font-sans flex flex-col justify-between select-none">
      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-20 space-y-10 w-full flex-grow">
        
        {/* 1. HERO / WELCOME HEADER */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-slate-900"
        >
          <div>
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono mb-2">
              <LayoutDashboard className="w-3.5 h-3.5" />
              <span>Agricultural Market Overview</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              Application Dashboard
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Welcome back, <strong className="text-emerald-400 font-semibold">{displayName}</strong>. Access key market highlights, AI insights, and navigation shortcuts.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3 self-start md:self-auto">
            <Link
              to="/markets"
              className="px-4 py-2.5 text-xs font-semibold rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-950/50 transition-all flex items-center space-x-1.5 min-h-[44px]"
            >
              <Store className="w-4 h-4" />
              <span>Explore Markets</span>
            </Link>

            {roleName === 'FARMER' && (
              <Link
                to="/farms"
                className="px-4 py-2.5 text-xs font-semibold rounded-xl bg-slate-900 border border-slate-800 text-emerald-400 hover:text-emerald-300 transition-colors flex items-center space-x-1.5 min-h-[44px]"
              >
                <Sprout className="w-4 h-4" />
                <span>My Farms</span>
              </Link>
            )}

            <Link
              to="/profile"
              className="px-4 py-2.5 text-xs font-semibold rounded-xl bg-slate-900 border border-slate-800 text-slate-200 hover:text-white transition-colors flex items-center space-x-1.5 min-h-[44px]"
            >
              <User className="w-4 h-4" />
              <span>My Profile</span>
            </Link>
          </div>
        </motion.div>

        {/* 2. MINIMAL USER IDENTITY INDICATOR */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <GlassCard className="p-5 sm:p-6 border-slate-800/80 shadow-xl relative overflow-hidden">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center space-x-3.5">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <h2 className="text-base font-bold text-white">{displayName}</h2>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                      <Award className="w-3 h-3 mr-1" />
                      {roleName}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-0.5 flex items-center space-x-2">
                    <span>{currentUser?.email}</span>
                    {currentUser?.emailVerified && (
                      <span className="text-emerald-400 font-semibold flex items-center">
                        <CheckCircle2 className="w-3 h-3 ml-1 mr-0.5 inline" /> Verified
                      </span>
                    )}
                  </p>
                </div>
              </div>

              <div className="flex items-center space-x-3 text-xs text-slate-400 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-900">
                <span className="font-mono text-[11px] bg-slate-950 px-3 py-1 rounded-lg border border-slate-900 truncate max-w-[180px]">
                  UID: {currentUser?.uid?.substring(0, 10)}...
                </span>
                <Link
                  to="/profile"
                  className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 transition-colors flex items-center space-x-1"
                >
                  <span>Full Profile</span>
                  <ChevronRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          </GlassCard>
        </motion.div>

        {/* 3. FEATURE PREVIEWS GRID (SHORT CARDS WITH CTAS) */}
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <h2 className="text-lg font-bold text-white flex items-center space-x-2">
              <Sparkles className="w-4 h-4 text-emerald-400" />
              <span>Platform Highlights & Previews</span>
            </h2>
            <span className="text-xs text-slate-500 font-mono">Overview Mode</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            
            {/* FEATURE PREVIEW 1: MARKET INTELLIGENCE */}
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: 0.15 }}
            >
              <GlassCard className="p-6 border-slate-800/80 shadow-xl space-y-4 flex flex-col justify-between h-full hover:border-emerald-500/40 transition-colors">
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                      <Store className="w-5 h-5" />
                    </div>
                    <span className="text-[10px] font-mono px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-semibold">
                      Module 06
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-white">Market Directory</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    Discover recognized agricultural markets, wholesale mandis, and crop coverage across regional trading networks.
                  </p>
                </div>

                <div className="pt-3 border-t border-slate-900">
                  <Link
                    to="/markets"
                    className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-slate-950 hover:bg-emerald-600 text-slate-200 hover:text-white font-semibold text-xs flex items-center justify-between transition-all border border-slate-800 hover:border-emerald-500/50 group"
                  >
                    <span>Explore Markets Directory</span>
                    <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-white group-hover:translate-x-1 transition-all" />
                  </Link>
                </div>
              </GlassCard>
            </motion.div>

            {/* FEATURE PREVIEW 2: AI INSIGHTS */}
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: 0.2 }}
            >
              <GlassCard className="p-6 border-slate-800/80 shadow-xl space-y-4 flex flex-col justify-between h-full hover:border-purple-500/40 transition-colors">
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400">
                      <BrainCircuit className="w-5 h-5" />
                    </div>
                    <span className="text-[10px] font-mono px-2.5 py-0.5 rounded-full bg-purple-500/10 text-purple-400 border border-purple-500/20 font-semibold">
                      Predictive Core
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-white">AI Market Forecasting</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    Harvest trajectory modeling, price variance analytics, and demand surge forecasting tailored for agricultural decision makers.
                  </p>
                </div>

                <div className="pt-3 border-t border-slate-900">
                  <a
                    href="/#ai-insights"
                    className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-slate-950 hover:bg-purple-600 text-slate-200 hover:text-white font-semibold text-xs flex items-center justify-between transition-all border border-slate-800 hover:border-purple-500/50 group"
                  >
                    <span>Explore AI Insights</span>
                    <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-white group-hover:translate-x-1 transition-all" />
                  </a>
                </div>
              </GlassCard>
            </motion.div>

            {/* FEATURE PREVIEW 3: FARM MANAGEMENT (OR ROLE SHORTCUT) */}
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: 0.25 }}
            >
              <GlassCard className="p-6 border-slate-800/80 shadow-xl space-y-4 flex flex-col justify-between h-full hover:border-emerald-500/40 transition-colors">
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
                      <Sprout className="w-5 h-5" />
                    </div>
                    <span className="text-[10px] font-mono px-2.5 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 font-semibold">
                      {roleName === 'FARMER' ? 'Farmer Portal' : 'Ecosystem View'}
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-white">
                    {roleName === 'FARMER' ? 'Farm & Crop Management' : 'Agricultural Ecosystem'}
                  </h3>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    {roleName === 'FARMER'
                      ? 'Manage registered farm land, seasonal crop varieties, and track upcoming harvest windows.'
                      : 'Explore tailored capabilities for Farmers, Mediators / Buyers, and Customers across the platform.'}
                  </p>
                </div>

                <div className="pt-3 border-t border-slate-900">
                  <Link
                    to={roleName === 'FARMER' ? '/farms' : '/profile'}
                    className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-slate-950 hover:bg-emerald-600 text-slate-200 hover:text-white font-semibold text-xs flex items-center justify-between transition-all border border-slate-800 hover:border-emerald-500/50 group"
                  >
                    <span>{roleName === 'FARMER' ? 'Manage Farms' : 'View Profile Setup'}</span>
                    <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-white group-hover:translate-x-1 transition-all" />
                  </Link>
                </div>
              </GlassCard>
            </motion.div>

          </div>
        </div>

        {/* 4. THREE-ROLE ECOSYSTEM OVERVIEW */}
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.3 }}
          className="space-y-4"
        >
          <div className="flex items-center justify-between px-1">
            <h2 className="text-lg font-bold text-white flex items-center space-x-2">
              <Layers className="w-4 h-4 text-emerald-400" />
              <span>Three-Role Ecosystem Overview</span>
            </h2>
            <span className="text-xs text-slate-500 font-mono">Platform Roles</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            
            {/* ROLE 1: FARMER */}
            <GlassCard className="p-5 border-slate-800/80 space-y-3">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  <Sprout className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">Farmer Profile</h3>
                  <span className="text-[10px] text-emerald-400 font-mono">Cultivation & Mandi Link</span>
                </div>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                Register farms, map Kharif/Rabi crop varieties, and access direct mandi arrival signals to plan harvest sales.
              </p>
            </GlassCard>

            {/* ROLE 2: MEDIATOR / BUYER */}
            <GlassCard className="p-5 border-slate-800/80 space-y-3">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  <Store className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">Mediator / Buyer Profile</h3>
                  <span className="text-[10px] text-amber-400 font-mono">Supply Sourcing</span>
                </div>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                Connect directly with regional harvests, verified quality classifications, and transparent wholesale price signals.
              </p>
            </GlassCard>

            {/* ROLE 3: CUSTOMER */}
            <GlassCard className="p-5 border-slate-800/80 space-y-3">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 rounded-xl bg-pink-500/10 text-pink-400 border border-pink-500/20">
                  <ShoppingBag className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">Customer Profile</h3>
                  <span className="text-[10px] text-pink-400 font-mono">Origin & Price Traceability</span>
                </div>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                Access farm-to-table origin transparency, regional crop tracking, and fair retail commodity price baselines.
              </p>
            </GlassCard>

          </div>
        </motion.div>

        {/* 5. PROTECTED BACKEND API TELEMETRY VERIFICATION */}
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.35 }}
        >
          <GlassCard className="p-6 border-slate-800/80 shadow-xl space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-900 pb-4">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400 shrink-0">
                  <Cpu className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Backend Telemetry Verification</h3>
                  <p className="text-xs text-slate-400">
                    Execute authenticated REST request to <code className="font-mono bg-slate-950 px-1.5 py-0.5 rounded text-emerald-400">GET /api/v1/users/me</code>
                  </p>
                </div>
              </div>

              <span className="text-[10px] font-mono px-2.5 py-1 rounded-full bg-slate-950 text-slate-400 border border-slate-800 self-start sm:self-auto">
                Spring Boot 3.3.4
              </span>
            </div>

            <div className="space-y-3">
              <button
                onClick={fetchBackendUserMe}
                disabled={loadingApi}
                className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 font-semibold text-xs flex items-center justify-center space-x-2 border border-slate-800 transition-all disabled:opacity-50"
              >
                <RefreshCw className={`w-3.5 h-3.5 text-emerald-400 ${loadingApi ? 'animate-spin' : ''}`} />
                <span>{loadingApi ? 'Verifying Token...' : 'Test Authenticated Endpoint /api/v1/users/me'}</span>
              </button>

              {apiError && (
                <div className="p-3 bg-rose-500/10 border border-rose-500/30 text-rose-300 rounded-xl text-[11px]">
                  {apiError}
                </div>
              )}

              {apiResponse && (
                <div className="p-3 bg-slate-950 border border-slate-900 rounded-xl font-mono text-[11px] text-emerald-400 overflow-x-auto max-h-36">
                  <pre>{JSON.stringify(apiResponse, null, 2)}</pre>
                </div>
              )}
            </div>
          </GlassCard>
        </motion.div>

      </main>

      <Footer />
    </div>
  );
};

export default DashboardPage;
