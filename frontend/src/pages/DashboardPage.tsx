import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { LayoutDashboard, User, ShieldCheck, Cpu, RefreshCw, Store, ArrowRight, CheckCircle2, Award } from 'lucide-react';
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

  const displayName = userDocument?.displayName || currentUser?.displayName || currentUser?.email?.split('@')[0] || 'User';
  const roleName = userDocument?.role || 'USER';

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 font-sans flex flex-col justify-between select-none">
      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-20 space-y-8 w-full flex-grow">
        
        {/* PAGE HEADER */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-900"
        >
          <div>
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono mb-2">
              <LayoutDashboard className="w-3.5 h-3.5" />
              <span>Agricultural Market Portal</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              Application Dashboard
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Welcome back, <strong className="text-emerald-400 font-semibold">{displayName}</strong>. Your authentication and market intelligence parameters are active.
            </p>
          </div>

          <div className="flex items-center space-x-3 self-start md:self-auto">
            <Link
              to="/markets"
              className="px-4 py-2.5 text-xs font-semibold rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-950/50 transition-all flex items-center space-x-1.5 min-h-[44px]"
            >
              <Store className="w-4 h-4" />
              <span>Explore Markets</span>
            </Link>
            <Link
              to="/profile"
              className="px-4 py-2.5 text-xs font-semibold rounded-xl bg-slate-900 border border-slate-800 text-slate-200 hover:text-white transition-colors flex items-center space-x-1.5 min-h-[44px]"
            >
              <User className="w-4 h-4" />
              <span>My Profile</span>
            </Link>
          </div>
        </motion.div>

        {/* PRIMARY USER IDENTITY CARD */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <GlassCard className="p-6 sm:p-8 border-slate-800/80 shadow-2xl relative overflow-hidden">
            <div className="flex items-center justify-between border-b border-slate-900 pb-4 mb-6">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-white">Identity & Authentication Verification</h2>
                  <p className="text-xs text-slate-400">Authenticated via Firebase Auth & Spring Boot Backend API</p>
                </div>
              </div>
              <span className="text-[11px] font-mono font-semibold px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 hidden sm:inline">
                Active Session
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
              <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                <span className="text-slate-400 font-medium block">Display Name</span>
                <span className="font-bold text-sm text-white block truncate">{displayName}</span>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                <span className="text-slate-400 font-medium block">Account Email</span>
                <div className="flex items-center justify-between pt-0.5">
                  <span className="font-semibold text-xs text-slate-200 truncate">{currentUser?.email}</span>
                  {currentUser?.emailVerified && (
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0 ml-1" />
                  )}
                </div>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                <span className="text-slate-400 font-medium block">Primary Role</span>
                <span className="inline-flex items-center mt-0.5 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  <Award className="w-3 h-3 mr-1" />
                  {roleName}
                </span>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                <span className="text-slate-400 font-medium block">System Identifier (UID)</span>
                <code className="font-mono text-[11px] text-slate-400 block truncate pt-0.5 font-semibold">
                  {currentUser?.uid}
                </code>
              </div>
            </div>
          </GlassCard>
        </motion.div>

        {/* QUICK SHORTCUTS GRID */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* MARKET DIRECTORY CARD */}
          <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: 0.2 }}
          >
            <GlassCard className="p-6 border-slate-800/80 shadow-xl space-y-4 flex flex-col justify-between h-full">
              <div className="space-y-2">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mb-3">
                  <Store className="w-5 h-5" />
                </div>
                <h3 className="text-lg font-bold text-white">Recognized Agricultural Markets</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Browse official trading mandis, wholesale centers, and supported commodity coverage across regional agricultural networks.
                </p>
              </div>

              <Link
                to="/markets"
                className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-slate-950 hover:bg-emerald-600 text-slate-200 hover:text-white font-semibold text-xs flex items-center justify-between transition-all border border-slate-800 hover:border-emerald-500/50 group"
              >
                <span>Browse Market Master Directory</span>
                <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-white group-hover:translate-x-1 transition-all" />
              </Link>
            </GlassCard>
          </motion.div>

          {/* BACKEND TELEMETRY API CARD */}
          <motion.div
            initial={{ opacity: 0, x: 10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: 0.3 }}
          >
            <GlassCard className="p-6 border-slate-800/80 shadow-xl space-y-4 flex flex-col justify-between h-full">
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400 mb-1">
                    <Cpu className="w-5 h-5" />
                  </div>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-950 text-slate-400 border border-slate-800">
                    Spring Boot 3.3.4
                  </span>
                </div>
                <h3 className="text-lg font-bold text-white">Protected API Verification</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Execute authenticated Spring Boot REST requests to <code className="font-mono bg-slate-950 px-1.5 py-0.5 rounded text-emerald-400">GET /api/v1/users/me</code> using Firebase ID token authentication headers.
                </p>
              </div>

              <div className="space-y-3 pt-2">
                <button
                  onClick={fetchBackendUserMe}
                  disabled={loadingApi}
                  className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 font-semibold text-xs flex items-center justify-center space-x-2 border border-slate-800 transition-all disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 text-emerald-400 ${loadingApi ? 'animate-spin' : ''}`} />
                  <span>{loadingApi ? 'Verifying API Token...' : 'Test Backend /api/v1/users/me'}</span>
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
        </div>

      </main>

      <Footer />
    </div>
  );
};

export default DashboardPage;

