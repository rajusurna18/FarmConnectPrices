import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { LayoutDashboard, User, ShieldCheck, Cpu, RefreshCw } from 'lucide-react';
import { useAuth } from '../features/auth/hooks/useAuth';
import { apiClient } from '../services/api';
import { GlassCard } from '../components/ui/GlassCard';
import { Navbar } from '../components/navigation/Navbar';

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

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 font-sans flex flex-col justify-between">
      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-16 space-y-8 w-full">
        {/* Welcome Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold mb-2">
              <LayoutDashboard className="w-3.5 h-3.5" />
              <span>Platform Portal</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white">Authenticated Dashboard</h1>
            <p className="text-sm text-slate-400 mt-1">
              Welcome back, <strong className="text-emerald-400">{userDocument?.displayName || currentUser?.displayName || currentUser?.email}</strong>. Your account identity and role parameters are active.
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <Link
              to="/profile"
              className="px-4 py-2 text-xs sm:text-sm font-semibold rounded-xl bg-slate-900 border border-slate-700 text-slate-200 hover:text-white flex items-center space-x-1.5"
            >
              <User className="w-4 h-4" />
              <span>View Profile</span>
            </Link>
          </div>
        </div>

        {/* User Identity Card */}
        <GlassCard className="p-6 sm:p-8 border-slate-800">
          <h2 className="text-xl font-bold text-white border-b border-slate-800 pb-3 mb-6 flex items-center">
            <ShieldCheck className="w-5 h-5 text-emerald-400 mr-2" />
            User Identity & Role Verification
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 text-sm">
            <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800/80">
              <span className="text-xs text-slate-400 font-medium block">Display Name</span>
              <span className="font-bold text-base text-white">{userDocument?.displayName || currentUser?.displayName || 'N/A'}</span>
            </div>

            <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800/80">
              <span className="text-xs text-slate-400 font-medium block">Email Address</span>
              <span className="font-semibold text-sm text-white truncate block">{currentUser?.email}</span>
            </div>

            <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800/80">
              <span className="text-xs text-slate-400 font-medium block">Assigned Role</span>
              <span className="inline-flex items-center mt-1 px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                {userDocument?.role || 'USER'}
              </span>
            </div>

            <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800/80">
              <span className="text-xs text-slate-400 font-medium block">Account Status</span>
              <span className="inline-flex items-center mt-1 px-3 py-1 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/30">
                {userDocument?.status || 'ACTIVE'}
              </span>
            </div>

            <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800/80">
              <span className="text-xs text-slate-400 font-medium block">Email Verification</span>
              <span className={`inline-flex items-center mt-1 px-3 py-1 rounded-full text-xs font-bold ${
                currentUser?.emailVerified
                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                  : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
              }`}>
                {currentUser?.emailVerified ? 'VERIFIED' : 'UNVERIFIED'}
              </span>
            </div>

            <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800/80">
              <span className="text-xs text-slate-400 font-medium block">Firebase UID</span>
              <span className="font-mono text-xs text-slate-400 truncate block mt-1">{currentUser?.uid}</span>
            </div>
          </div>
        </GlassCard>

        {/* Backend API Diagnostics Card */}
        <GlassCard className="p-6 sm:p-8 border-slate-800">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
            <h2 className="text-xl font-bold text-white flex items-center">
              <Cpu className="w-5 h-5 text-emerald-400 mr-2" />
              Protected Backend API Test
            </h2>
            <span className="text-xs font-mono text-slate-400">Spring Boot 21</span>
          </div>

          <p className="text-xs sm:text-sm text-slate-400 mb-4">
            Click below to execute a protected request to <code className="font-mono bg-slate-900 px-2 py-1 rounded text-emerald-400">GET /api/v1/users/me</code> with the Firebase ID token in the <code className="font-mono bg-slate-900 px-2 py-1 rounded text-emerald-400">Authorization: Bearer</code> header.
          </p>

          <button
            onClick={fetchBackendUserMe}
            disabled={loadingApi}
            className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-sm rounded-xl shadow-lg shadow-emerald-950/50 transition-all flex items-center space-x-2 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loadingApi ? 'animate-spin' : ''}`} />
            <span>{loadingApi ? 'Calling API...' : 'Fetch /api/v1/users/me'}</span>
          </button>

          {apiError && (
            <div className="mt-4 p-4 bg-rose-500/10 border border-rose-500/30 text-rose-300 rounded-2xl text-xs">
              {apiError}
            </div>
          )}

          {apiResponse && (
            <div className="mt-4 p-4 bg-slate-950 border border-slate-800 rounded-2xl font-mono text-xs text-emerald-400 overflow-x-auto">
              <pre>{JSON.stringify(apiResponse, null, 2)}</pre>
            </div>
          )}
        </GlassCard>
      </main>
    </div>
  );
};

export default DashboardPage;
