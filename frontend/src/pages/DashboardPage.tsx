import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/hooks/useAuth';
import { apiClient } from '../services/api';

interface BackendUserMeResponse {
  uid: string;
  email: string;
  displayName: string;
  role: string;
  status: string;
}

export const DashboardPage: React.FC = () => {
  const { currentUser, userDocument, logout } = useAuth();
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
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white transition-colors">
      {/* Header */}
      <header className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <Link to="/" className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
            FarmConnectPrices
          </Link>
          <div className="flex items-center space-x-4">
            <span className="text-sm font-medium text-gray-600 dark:text-gray-300">
              {currentUser?.displayName || currentUser?.email}
            </span>
            <button
              onClick={logout}
              className="bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-800 dark:text-gray-200 px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">Authenticated Dashboard</h1>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            Welcome to the FarmConnectPrices protected foundation. Basic user authentication is active.
          </p>
        </div>

        {/* User Profile Card */}
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6 space-y-6">
          <h2 className="text-xl font-bold border-b border-gray-200 dark:border-gray-700 pb-3">User Profile Identity</h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-sm">
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium block">Display Name</span>
              <span className="font-semibold text-base">{userDocument?.displayName || currentUser?.displayName || 'N/A'}</span>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium block">Email Address</span>
              <span className="font-semibold text-base">{currentUser?.email}</span>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium block">Firebase UID</span>
              <span className="font-mono text-xs text-gray-700 dark:text-gray-300">{currentUser?.uid}</span>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium block">System Role</span>
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-300">
                {userDocument?.role || 'USER'}
              </span>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium block">Account Status</span>
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/50 dark:text-blue-300">
                {userDocument?.status || 'ACTIVE'}
              </span>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium block">Email Verified</span>
              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                currentUser?.emailVerified
                  ? 'bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-300'
                  : 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/50 dark:text-yellow-300'
              }`}>
                {currentUser?.emailVerified ? 'VERIFIED' : 'UNVERIFIED'}
              </span>
            </div>
          </div>
        </div>

        {/* Spring Boot API Integration Test */}
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6 space-y-4">
          <h2 className="text-xl font-bold">Protected Backend API Test</h2>
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Click below to send a request to <code className="font-mono bg-gray-100 dark:bg-gray-700 px-1 py-0.5 rounded">GET /api/v1/users/me</code> with the Firebase ID token in the <code className="font-mono bg-gray-100 dark:bg-gray-700 px-1 py-0.5 rounded">Authorization: Bearer</code> header.
          </p>

          <button
            onClick={fetchBackendUserMe}
            disabled={loadingApi}
            className="bg-emerald-600 text-white font-medium px-4 py-2 rounded-md hover:bg-emerald-700 disabled:opacity-50 transition-colors"
          >
            {loadingApi ? 'Calling API...' : 'Fetch /api/v1/users/me'}
          </button>

          {apiError && (
            <div className="p-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 rounded-lg text-sm">
              {apiError}
            </div>
          )}

          {apiResponse && (
            <div className="mt-4 p-4 bg-gray-900 text-emerald-400 rounded-lg font-mono text-xs overflow-x-auto">
              <pre>{JSON.stringify(apiResponse, null, 2)}</pre>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};
