import React from 'react';
import { APP_NAME, APP_TAGLINE } from '../constants/app';
import { useHealthStatus } from '../hooks/useHealthStatus';

export const HomePage: React.FC = () => {
  const { data: healthData, isLoading, isError } = useHealthStatus();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-center p-6 text-center">
      <main className="max-w-2xl mx-auto space-y-6">
        <div className="space-y-3">
          <h1 className="text-4xl sm:text-5xl font-bold tracking-tight text-emerald-400">
            {APP_NAME}
          </h1>
          <p className="text-xl sm:text-2xl text-slate-300 font-medium">
            {APP_TAGLINE}
          </p>
        </div>

        <div className="pt-8 border-t border-slate-800 flex flex-col items-center gap-3">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-slate-900 border border-slate-800 text-sm">
            <span className="text-slate-400">Backend API Health:</span>
            {isLoading && (
              <span className="text-amber-400 font-medium">Connecting...</span>
            )}
            {isError && (
              <span className="text-rose-400 font-medium">Offline / Error</span>
            )}
            {healthData && (
              <span className="inline-flex items-center gap-1.5 text-emerald-400 font-medium">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                {healthData.status} ({healthData.service})
              </span>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};
