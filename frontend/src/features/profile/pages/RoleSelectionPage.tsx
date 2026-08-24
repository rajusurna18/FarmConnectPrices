import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useProfile } from '../hooks/useProfile';
import type { PrimaryRole } from '../types';

export const RoleSelectionPage: React.FC = () => {
  const [selectedRole, setSelectedRole] = useState<PrimaryRole | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const { selectRole, isSelectingRole } = useProfile();
  const navigate = useNavigate();

  const handleConfirmRole = async () => {
    if (!selectedRole) {
      setErrorMsg('Please select a role before continuing.');
      return;
    }
    setErrorMsg(null);
    try {
      await selectRole({ role: selectedRole });
      navigate('/profile/edit', { replace: true });
    } catch (err: unknown) {
      console.error('Role selection failed:', err);
      setErrorMsg('Failed to set role. Please try again.');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white flex flex-col justify-between py-12 px-4 sm:px-6 lg:px-8 transition-colors">
      {/* Brand Header */}
      <header className="max-w-5xl mx-auto w-full text-center mb-8">
        <Link to="/" className="text-3xl font-extrabold text-emerald-600 dark:text-emerald-400 tracking-tight">
          FarmConnectPrices
        </Link>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400 font-medium">
          Real Prices. Better Markets. Smarter Decisions.
        </p>
      </header>

      {/* Main Onboarding Card Container */}
      <main className="max-w-5xl mx-auto w-full bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-200 dark:border-gray-700 p-6 sm:p-10 transition-all duration-300">
        <div className="text-center max-w-2xl mx-auto mb-8 space-y-2">
          <span className="inline-block px-3 py-1 text-xs font-semibold uppercase tracking-wider text-emerald-700 dark:text-emerald-300 bg-emerald-100 dark:bg-emerald-950/60 rounded-full">
            Onboarding • Step 1 of 2
          </span>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-gray-900 dark:text-white">
            Choose Your Platform Role
          </h1>
          <p className="text-sm sm:text-base text-gray-600 dark:text-gray-300">
            Select how you intend to participate in FarmConnectPrices. This customizes your profile and market experience.
          </p>
        </div>

        {errorMsg && (
          <div
            role="alert"
            className="mb-6 p-4 bg-red-50 dark:bg-red-900/30 border-l-4 border-red-500 text-red-700 dark:text-red-300 text-sm rounded-r-lg motion-reduce:transition-none transition-all duration-200"
          >
            {errorMsg}
          </div>
        )}

        {/* Three Role Cards Grid: 1 col on mobile, 3 cols on desktop */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Farmer Card */}
          <button
            type="button"
            onClick={() => setSelectedRole('FARMER')}
            aria-pressed={selectedRole === 'FARMER'}
            className={`group relative flex flex-col items-center justify-between p-6 rounded-xl border-2 text-left cursor-pointer transition-all duration-200 motion-reduce:transition-none focus:outline-none focus:ring-4 focus:ring-emerald-500/50 min-h-[220px] ${
              selectedRole === 'FARMER'
                ? 'border-emerald-600 dark:border-emerald-500 bg-emerald-50/70 dark:bg-emerald-950/40 shadow-lg scale-[1.02]'
                : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-emerald-300 dark:hover:border-emerald-700 hover:bg-gray-50/80 dark:hover:bg-gray-750'
            }`}
          >
            <div className="flex flex-col items-center text-center space-y-3 w-full">
              <div
                className={`w-14 h-14 rounded-2xl flex items-center justify-center text-3xl transition-transform duration-200 motion-reduce:transition-none group-hover:scale-110 ${
                  selectedRole === 'FARMER'
                    ? 'bg-emerald-600 text-white dark:bg-emerald-500'
                    : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300'
                }`}
              >
                🌾
              </div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                Farmer
              </h2>
              <p className="text-xs text-gray-600 dark:text-gray-300 leading-relaxed">
                Sell your agricultural produce and manage your farmer journey.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-gray-200/60 dark:border-gray-700/60 w-full flex items-center justify-center">
              <span
                className={`text-xs font-semibold ${
                  selectedRole === 'FARMER'
                    ? 'text-emerald-700 dark:text-emerald-300 font-bold'
                    : 'text-gray-400 dark:text-gray-500'
                }`}
              >
                {selectedRole === 'FARMER' ? '✓ Selected' : 'Select Farmer'}
              </span>
            </div>
          </button>

          {/* Mediator / Buyer Card */}
          <button
            type="button"
            onClick={() => setSelectedRole('MEDIATOR_BUYER')}
            aria-pressed={selectedRole === 'MEDIATOR_BUYER'}
            className={`group relative flex flex-col items-center justify-between p-6 rounded-xl border-2 text-left cursor-pointer transition-all duration-200 motion-reduce:transition-none focus:outline-none focus:ring-4 focus:ring-emerald-500/50 min-h-[220px] ${
              selectedRole === 'MEDIATOR_BUYER'
                ? 'border-emerald-600 dark:border-emerald-500 bg-emerald-50/70 dark:bg-emerald-950/40 shadow-lg scale-[1.02]'
                : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-emerald-300 dark:hover:border-emerald-700 hover:bg-gray-50/80 dark:hover:bg-gray-750'
            }`}
          >
            <div className="flex flex-col items-center text-center space-y-3 w-full">
              <div
                className={`w-14 h-14 rounded-2xl flex items-center justify-center text-3xl transition-transform duration-200 motion-reduce:transition-none group-hover:scale-110 ${
                  selectedRole === 'MEDIATOR_BUYER'
                    ? 'bg-emerald-600 text-white dark:bg-emerald-500'
                    : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300'
                }`}
              >
                🏪
              </div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                Mediator / Buyer
              </h2>
              <p className="text-xs text-gray-600 dark:text-gray-300 leading-relaxed">
                Connect with farmers and participate in agricultural buying activities.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-gray-200/60 dark:border-gray-700/60 w-full flex items-center justify-center">
              <span
                className={`text-xs font-semibold ${
                  selectedRole === 'MEDIATOR_BUYER'
                    ? 'text-emerald-700 dark:text-emerald-300 font-bold'
                    : 'text-gray-400 dark:text-gray-500'
                }`}
              >
                {selectedRole === 'MEDIATOR_BUYER' ? '✓ Selected' : 'Select Mediator / Buyer'}
              </span>
            </div>
          </button>

          {/* Customer Card */}
          <button
            type="button"
            onClick={() => setSelectedRole('CUSTOMER')}
            aria-pressed={selectedRole === 'CUSTOMER'}
            className={`group relative flex flex-col items-center justify-between p-6 rounded-xl border-2 text-left cursor-pointer transition-all duration-200 motion-reduce:transition-none focus:outline-none focus:ring-4 focus:ring-emerald-500/50 min-h-[220px] ${
              selectedRole === 'CUSTOMER'
                ? 'border-emerald-600 dark:border-emerald-500 bg-emerald-50/70 dark:bg-emerald-950/40 shadow-lg scale-[1.02]'
                : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-emerald-300 dark:hover:border-emerald-700 hover:bg-gray-50/80 dark:hover:bg-gray-750'
            }`}
          >
            <div className="flex flex-col items-center text-center space-y-3 w-full">
              <div
                className={`w-14 h-14 rounded-2xl flex items-center justify-center text-3xl transition-transform duration-200 motion-reduce:transition-none group-hover:scale-110 ${
                  selectedRole === 'CUSTOMER'
                    ? 'bg-emerald-600 text-white dark:bg-emerald-500'
                    : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300'
                }`}
              >
                🛒
              </div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                Customer
              </h2>
              <p className="text-xs text-gray-600 dark:text-gray-300 leading-relaxed">
                Explore agricultural products and services as a customer.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-gray-200/60 dark:border-gray-700/60 w-full flex items-center justify-center">
              <span
                className={`text-xs font-semibold ${
                  selectedRole === 'CUSTOMER'
                    ? 'text-emerald-700 dark:text-emerald-300 font-bold'
                    : 'text-gray-400 dark:text-gray-500'
                }`}
              >
                {selectedRole === 'CUSTOMER' ? '✓ Selected' : 'Select Customer'}
              </span>
            </div>
          </button>
        </div>

        {/* Confirmation Action Button */}
        <div className="flex flex-col sm:flex-row items-center justify-end space-y-3 sm:space-y-0 sm:space-x-4 pt-4 border-t border-gray-200 dark:border-gray-700">
          <button
            type="button"
            onClick={handleConfirmRole}
            disabled={!selectedRole || isSelectingRole}
            className="w-full sm:w-auto min-h-[44px] px-8 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-medium rounded-xl shadow-md disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 motion-reduce:transition-none flex items-center justify-center space-x-2 text-base"
          >
            {isSelectingRole ? (
              <>
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>Saving Role...</span>
              </>
            ) : (
              <span>Continue to Profile Setup →</span>
            )}
          </button>
        </div>
      </main>

      {/* Footer */}
      <footer className="max-w-5xl mx-auto w-full text-center mt-8 text-xs text-gray-500 dark:text-gray-400">
        © {new Date().getFullYear()} FarmConnectPrices. All rights reserved.
      </footer>
    </div>
  );
};
