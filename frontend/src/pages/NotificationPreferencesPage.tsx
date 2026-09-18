import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Navbar } from '../components/navigation/Navbar';
import { Footer } from '../components/navigation/Footer';
import {
  useNotificationPreferences,
  useUpdateNotificationPreferences,
} from '../hooks/useNotificationPreferences';
import {
  NOTIFICATION_CATEGORIES,
  type NotificationPreference,
} from '../types/notificationPreference';
import {
  Bell,
  ArrowLeft,
  Save,
  RotateCcw,
  CheckCircle2,
  AlertCircle,
  Tag,
  ShoppingBag,
  CreditCard,
  Truck,
  Star,
  ShieldCheck,
} from 'lucide-react';

function getCategoryIcon(iconName: string) {
  switch (iconName) {
    case 'Tag':
      return <Tag className="w-5 h-5 text-amber-400" />;
    case 'ShoppingBag':
      return <ShoppingBag className="w-5 h-5 text-emerald-400" />;
    case 'CreditCard':
      return <CreditCard className="w-5 h-5 text-blue-400" />;
    case 'Truck':
      return <Truck className="w-5 h-5 text-teal-400" />;
    case 'Star':
      return <Star className="w-5 h-5 text-yellow-400" />;
    default:
      return <Bell className="w-5 h-5 text-emerald-400" />;
  }
}

export const NotificationPreferencesPage: React.FC = () => {
  const { data: remotePreferences, isLoading, isError, error, refetch } = useNotificationPreferences();
  const updateMutation = useUpdateNotificationPreferences();

  // Local state for interactive switches
  const [localPrefs, setLocalPrefs] = useState<NotificationPreference>({
    offers: true,
    orders: true,
    payments: true,
    delivery: true,
    reviews: true,
  });

  const [hasChanges, setHasChanges] = useState(false);
  const [saveSuccessMessage, setSaveSuccessMessage] = useState<string | null>(null);

  // Sync loaded remote data to local state
  useEffect(() => {
    if (remotePreferences) {
      setLocalPrefs(remotePreferences);
      setHasChanges(false);
    }
  }, [remotePreferences]);

  // Compute dirty state
  const handleToggle = (key: keyof NotificationPreference) => {
    setLocalPrefs((prev) => {
      const next = { ...prev, [key]: !prev[key] };
      if (remotePreferences) {
        const isDifferent =
          next.offers !== remotePreferences.offers ||
          next.orders !== remotePreferences.orders ||
          next.payments !== remotePreferences.payments ||
          next.delivery !== remotePreferences.delivery ||
          next.reviews !== remotePreferences.reviews;
        setHasChanges(isDifferent);
      } else {
        setHasChanges(true);
      }
      return next;
    });
    setSaveSuccessMessage(null);
  };

  const handleResetDefaults = () => {
    const defaultState: NotificationPreference = {
      offers: true,
      orders: true,
      payments: true,
      delivery: true,
      reviews: true,
    };
    setLocalPrefs(defaultState);
    if (remotePreferences) {
      const isDifferent =
        defaultState.offers !== remotePreferences.offers ||
        defaultState.orders !== remotePreferences.orders ||
        defaultState.payments !== remotePreferences.payments ||
        defaultState.delivery !== remotePreferences.delivery ||
        defaultState.reviews !== remotePreferences.reviews;
      setHasChanges(isDifferent);
    } else {
      setHasChanges(true);
    }
    setSaveSuccessMessage(null);
  };

  const handleSaveChanges = () => {
    updateMutation.mutate(localPrefs, {
      onSuccess: () => {
        setHasChanges(false);
        setSaveSuccessMessage('Notification preferences saved successfully!');
        setTimeout(() => setSaveSuccessMessage(null), 4000);
      },
    });
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between select-none">
      <Navbar />

      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-20 w-full flex-grow">
        {/* Navigation Breadcrumb */}
        <div className="mb-6 flex items-center justify-between">
          <Link
            to="/notifications"
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Notifications</span>
          </Link>
          <span className="text-xs font-mono text-slate-500">In-App Controls</span>
        </div>

        {/* Page Header */}
        <div className="mb-8 border-b border-slate-900 pb-6">
          <div className="flex items-center space-x-3 mb-2">
            <div className="p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
              <Bell className="w-7 h-7" />
            </div>
            <div>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
                Notification Preferences
              </h1>
              <p className="text-slate-400 text-xs sm:text-sm mt-0.5">
                Control which in-app notification categories you receive. Historical notifications remain preserved.
              </p>
            </div>
          </div>
        </div>

        {/* Success Banner */}
        {saveSuccessMessage && (
          <div className="mb-6 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center space-x-3 shadow-lg">
            <CheckCircle2 className="w-5 h-5 shrink-0" />
            <span className="text-xs sm:text-sm font-semibold">{saveSuccessMessage}</span>
          </div>
        )}

        {/* Mutation Error Banner */}
        {updateMutation.isError && (
          <div className="mb-6 p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-400 flex items-center space-x-3 shadow-lg">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span className="text-xs sm:text-sm font-semibold">
              {(updateMutation.error as Error)?.message || 'Failed to save changes. Please try again.'}
            </span>
          </div>
        )}

        {/* Loading Skeleton */}
        {isLoading && (
          <div className="space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div
                key={i}
                className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 animate-pulse flex items-center justify-between"
              >
                <div className="flex items-center space-x-4">
                  <div className="w-10 h-10 rounded-xl bg-slate-800" />
                  <div className="space-y-2">
                    <div className="h-4 bg-slate-800 rounded w-40" />
                    <div className="h-3 bg-slate-800/70 rounded w-64" />
                  </div>
                </div>
                <div className="w-14 h-8 bg-slate-800 rounded-full" />
              </div>
            ))}
          </div>
        )}

        {/* Error Fetching State */}
        {isError && (
          <div className="p-8 rounded-3xl bg-slate-900/80 border border-rose-500/20 text-center space-y-4 my-6">
            <AlertCircle className="w-10 h-10 text-rose-400 mx-auto" />
            <div>
              <h3 className="text-lg font-bold text-white">Failed to load preferences</h3>
              <p className="text-slate-400 text-xs mt-1">
                {(error as Error)?.message || 'Could not connect to preferences service.'}
              </p>
            </div>
            <button
              onClick={() => refetch()}
              className="px-5 py-2.5 bg-rose-600 hover:bg-rose-500 text-white font-semibold rounded-xl text-xs transition-colors"
            >
              Retry Loading
            </button>
          </div>
        )}

        {/* Preference Settings List */}
        {!isLoading && !isError && (
          <div className="space-y-4">
            <div className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 sm:p-8 space-y-6 shadow-2xl backdrop-blur-xl">
              <div className="flex items-center justify-between border-b border-slate-800/80 pb-4">
                <div className="flex items-center space-x-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <h2 className="text-sm font-bold uppercase tracking-wider text-slate-300">
                    Category Event Controls
                  </h2>
                </div>
                {hasChanges && (
                  <span className="px-2.5 py-1 text-[11px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30 rounded-full animate-pulse">
                    Unsaved changes
                  </span>
                )}
              </div>

              <div className="divide-y divide-slate-800/60">
                {NOTIFICATION_CATEGORIES.map((category) => {
                  const isEnabled = localPrefs[category.key];
                  return (
                    <div
                      key={category.key}
                      className="py-5 first:pt-0 last:pb-0 flex flex-col sm:flex-row sm:items-center justify-between gap-4 group"
                    >
                      <div className="flex items-start space-x-4">
                        <div className="p-3 rounded-2xl bg-slate-950 border border-slate-800 shrink-0 group-hover:scale-105 transition-transform">
                          {getCategoryIcon(category.iconName)}
                        </div>
                        <div>
                          <h3 className="text-base font-bold text-white flex items-center space-x-2">
                            <span>{category.title}</span>
                            <span
                              className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                                isEnabled
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : 'bg-slate-800 text-slate-500'
                              }`}
                            >
                              {isEnabled ? 'ENABLED' : 'OFF'}
                            </span>
                          </h3>
                          <p className="text-xs text-slate-400 leading-relaxed mt-1 max-w-lg">
                            {category.description}
                          </p>
                        </div>
                      </div>

                      {/* Custom Accessible Toggle Switch */}
                      <div className="flex items-center justify-end">
                        <button
                          type="button"
                          role="switch"
                          aria-checked={isEnabled}
                          onClick={() => handleToggle(category.key)}
                          className={`relative inline-flex h-8 w-14 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-emerald-400 focus:ring-offset-2 focus:ring-offset-slate-950 ${
                            isEnabled ? 'bg-emerald-500' : 'bg-slate-800'
                          }`}
                        >
                          <span className="sr-only">Toggle {category.title}</span>
                          <span
                            className={`pointer-events-none inline-block h-7 w-7 transform rounded-full bg-slate-950 shadow-lg ring-0 transition duration-200 ease-in-out flex items-center justify-center text-[10px] font-extrabold ${
                              isEnabled
                                ? 'translate-x-6 text-emerald-400'
                                : 'translate-x-0 text-slate-500'
                            }`}
                          >
                            {isEnabled ? 'ON' : 'OFF'}
                          </span>
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Bottom Controls Bar */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4">
              <button
                type="button"
                onClick={handleResetDefaults}
                className="w-full sm:w-auto px-4 py-3 rounded-2xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 hover:text-white text-xs font-semibold transition-all flex items-center justify-center space-x-2 shadow-sm"
              >
                <RotateCcw className="w-4 h-4 text-slate-400" />
                <span>Reset to Defaults</span>
              </button>

              <div className="w-full sm:w-auto flex items-center space-x-3">
                <button
                  type="button"
                  onClick={handleSaveChanges}
                  disabled={!hasChanges || updateMutation.isPending}
                  className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-xl shadow-emerald-500/20 disabled:opacity-40 disabled:cursor-not-allowed transition-all flex items-center justify-center space-x-2"
                >
                  {updateMutation.isPending ? (
                    <div className="w-4 h-4 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <Save className="w-4 h-4" />
                  )}
                  <span>{updateMutation.isPending ? 'Saving...' : 'Save Changes'}</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
};
