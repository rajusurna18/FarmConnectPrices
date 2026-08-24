import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useProfile } from '../hooks/useProfile';

export const ProfilePage: React.FC = () => {
  const { logout } = useAuth();
  const { profile, isLoading, isError, refetchProfile } = useProfile();
  const navigate = useNavigate();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-gray-600 dark:text-gray-300 font-medium text-sm">Loading user profile...</p>
        </div>
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4">
        <div className="max-w-md w-full bg-white dark:bg-gray-800 p-8 rounded-xl shadow-lg border border-red-200 dark:border-red-900/50 text-center space-y-4">
          <div className="text-red-500 text-4xl">⚠️</div>
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">Unable to Load Profile</h2>
          <p className="text-sm text-gray-600 dark:text-gray-400">
            There was an error fetching your profile details. Please check your connection and try again.
          </p>
          <button
            onClick={() => refetchProfile()}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium transition-colors"
          >
            Retry Loading
          </button>
        </div>
      </div>
    );
  }

  const isRoleUnassigned = profile.role === 'USER';

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white transition-colors">
      {/* Top Header Navigation */}
      <header className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <Link to="/" className="text-2xl font-extrabold text-emerald-600 dark:text-emerald-400">
            FarmConnectPrices
          </Link>
          <div className="flex items-center space-x-3">
            <Link
              to="/dashboard"
              className="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-emerald-600 dark:hover:text-emerald-400 px-3 py-2 rounded-lg transition-colors"
            >
              Dashboard
            </Link>
            <button
              onClick={logout}
              className="bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-800 dark:text-gray-200 px-3 py-2 rounded-lg text-sm font-medium transition-colors"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Main Page Container */}
      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 space-y-6 animate-fade-in">
        {/* Banner if role is unassigned */}
        {isRoleUnassigned && (
          <div className="bg-amber-50 dark:bg-amber-950/50 border-l-4 border-amber-500 p-4 rounded-r-xl shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <h3 className="font-bold text-amber-900 dark:text-amber-200 text-sm sm:text-base">
                Role Assignment Required
              </h3>
              <p className="text-xs sm:text-sm text-amber-800 dark:text-amber-300">
                Please select whether you are a Farmer or a Buyer to complete your account setup.
              </p>
            </div>
            <button
              onClick={() => navigate('/onboarding/role')}
              className="w-full sm:w-auto px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white font-medium text-sm rounded-lg shadow transition-colors whitespace-nowrap"
            >
              Select Role →
            </button>
          </div>
        )}

        {/* Profile Card Header */}
        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-200 dark:border-gray-700 p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row items-center sm:items-start justify-between gap-6 pb-6 border-b border-gray-200 dark:border-gray-700">
            {/* Avatar & Identifiers */}
            <div className="flex flex-col sm:flex-row items-center space-y-4 sm:space-y-0 sm:space-x-6 text-center sm:text-left">
              <div className="relative">
                <div className="w-20 h-20 sm:w-24 sm:h-24 bg-gradient-to-br from-emerald-500 to-teal-700 text-white rounded-full flex items-center justify-center text-3xl sm:text-4xl font-extrabold shadow-md">
                  {profile.displayName ? profile.displayName.charAt(0).toUpperCase() : 'U'}
                </div>
                <span className="absolute bottom-0 right-0 w-5 h-5 bg-green-500 border-2 border-white dark:border-gray-800 rounded-full" title="Active Account"></span>
              </div>
              <div className="space-y-1">
                <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">
                  {profile.displayName || 'Anonymous User'}
                </h1>
                <p className="text-sm text-gray-500 dark:text-gray-400 font-medium">
                  {profile.email}
                </p>
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-1">
                  <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800">
                    Role: {profile.role}
                  </span>
                  <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                    profile.profileCompleted
                      ? 'bg-blue-100 text-blue-800 dark:bg-blue-950/80 dark:text-blue-300 border border-blue-300 dark:border-blue-800'
                      : 'bg-yellow-100 text-yellow-800 dark:bg-yellow-950/80 dark:text-yellow-300 border border-yellow-300 dark:border-yellow-800'
                  }`}>
                    {profile.profileCompleted ? '✓ Profile Complete' : '⚠️ Profile Incomplete'}
                  </span>
                </div>
              </div>
            </div>

            {/* Edit Button */}
            <div className="w-full sm:w-auto flex justify-center">
              <Link
                to="/profile/edit"
                className="w-full sm:w-auto min-h-[44px] px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-medium rounded-xl shadow transition-all duration-200 motion-reduce:transition-none flex items-center justify-center space-x-2 text-sm"
              >
                <span>✏️ Edit Profile</span>
              </Link>
            </div>
          </div>

          {/* Detailed Info Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
            {/* Account Details Column */}
            <div className="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-5 border border-gray-100 dark:border-gray-700/50 space-y-4">
              <h3 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-700/60 pb-2">
                Account Details
              </h3>
              <div className="space-y-3 text-sm">
                <div>
                  <span className="text-gray-500 dark:text-gray-400 block text-xs">User ID (UID)</span>
                  <span className="font-mono text-xs text-gray-700 dark:text-gray-300 break-all">{profile.uid}</span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400 block text-xs">Phone Number</span>
                  <span className="font-semibold text-gray-800 dark:text-gray-200">
                    {profile.phoneNumber || 'Not provided'}
                  </span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400 block text-xs">Email Verification Status</span>
                  <span className={`inline-flex items-center text-xs font-semibold px-2 py-0.5 rounded ${
                    profile.emailVerified
                      ? 'text-green-700 dark:text-green-300 bg-green-100 dark:bg-green-950/60'
                      : 'text-yellow-700 dark:text-yellow-300 bg-yellow-100 dark:bg-yellow-950/60'
                  }`}>
                    {profile.emailVerified ? 'Verified' : 'Pending Verification'}
                  </span>
                </div>
              </div>
            </div>

            {/* Location Details Column */}
            <div className="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-5 border border-gray-100 dark:border-gray-700/50 space-y-4">
              <h3 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-700/60 pb-2">
                Location Metadata
              </h3>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <span className="text-gray-500 dark:text-gray-400 block text-xs">State</span>
                  <span className="font-semibold text-gray-800 dark:text-gray-200">
                    {profile.location?.state || '—'}
                  </span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400 block text-xs">District</span>
                  <span className="font-semibold text-gray-800 dark:text-gray-200">
                    {profile.location?.district || '—'}
                  </span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400 block text-xs">Mandal</span>
                  <span className="font-semibold text-gray-800 dark:text-gray-200">
                    {profile.location?.mandal || '—'}
                  </span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400 block text-xs">Village</span>
                  <span className="font-semibold text-gray-800 dark:text-gray-200">
                    {profile.location?.village || '—'}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
