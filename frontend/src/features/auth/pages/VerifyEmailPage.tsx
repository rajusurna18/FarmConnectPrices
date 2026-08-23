import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { sendEmailVerificationMail } from '../../../services/firebase';
import { getAuthErrorMessage } from '../utils/errorMapper';

export const VerifyEmailPage: React.FC = () => {
  const navigate = useNavigate();
  const { currentUser, logout, refreshUser } = useAuth();
  const [sending, setSending] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleResend = async () => {
    setMessage(null);
    setError(null);
    setSending(true);
    try {
      await sendEmailVerificationMail(currentUser);
      setMessage('A new verification email has been sent to your inbox.');
    } catch (err) {
      setError(getAuthErrorMessage(err));
    } finally {
      setSending(false);
    }
  };

  const handleRefresh = async () => {
    setMessage(null);
    setError(null);
    try {
      await refreshUser();
      if (currentUser?.emailVerified) {
        navigate('/dashboard');
      } else {
        setMessage('Status checked. Email is not verified yet. Please check your inbox or resend verification.');
      }
    } catch (err) {
      setError(getAuthErrorMessage(err));
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-gray-50 dark:bg-gray-900 transition-colors">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Link to="/" className="text-3xl font-extrabold text-emerald-600 dark:text-emerald-400">
          FarmConnectPrices
        </Link>
        <h2 className="mt-6 text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
          Verify your email address
        </h2>
        <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
          We have sent a verification link to <span className="font-semibold text-gray-900 dark:text-white">{currentUser?.email || 'your email'}</span>.
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white dark:bg-gray-800 py-8 px-4 shadow-xl sm:rounded-xl sm:px-10 border border-gray-100 dark:border-gray-700 text-center space-y-6">
          {error && (
            <div className="p-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 rounded-lg text-sm">
              {error}
            </div>
          )}

          {message && (
            <div className="p-3 bg-emerald-50 dark:bg-emerald-900/30 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300 rounded-lg text-sm">
              {message}
            </div>
          )}

          <div className="space-y-3">
            <button
              onClick={handleRefresh}
              className="w-full bg-emerald-600 text-white font-medium py-2.5 px-4 rounded-md shadow-sm hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-colors"
            >
              I've verified my email
            </button>

            <button
              onClick={handleResend}
              disabled={sending}
              className="w-full bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 font-medium py-2.5 px-4 rounded-md hover:bg-gray-200 dark:hover:bg-gray-600 disabled:opacity-50 transition-colors"
            >
              {sending ? 'Sending...' : 'Resend verification email'}
            </button>
          </div>

          <div className="pt-4 border-t border-gray-200 dark:border-gray-700 flex justify-between items-center text-sm">
            <Link to="/dashboard" className="text-emerald-600 hover:text-emerald-500 dark:text-emerald-400 font-medium">
              Go to Dashboard
            </Link>
            <button
              onClick={logout}
              className="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 font-medium"
            >
              Sign Out
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
