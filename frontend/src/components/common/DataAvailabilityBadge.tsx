import React from 'react';
import { Wifi, WifiOff, Database, RefreshCw } from 'lucide-react';

export type DataStatus = 'REAL_DATA' | 'NO_DATA' | 'TEMPORARILY_UNAVAILABLE' | 'LOADING';

interface DataAvailabilityBadgeProps {
  status: DataStatus;
  recordCount?: number;
  onRetry?: () => void;
  className?: string;
}

export const DataAvailabilityBadge: React.FC<DataAvailabilityBadgeProps> = ({
  status,
  recordCount = 0,
  onRetry,
  className = '',
}) => {
  if (status === 'LOADING') {
    return (
      <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-950/40 border border-emerald-500/30 text-emerald-400 text-xs font-medium animate-pulse ${className}`}>
        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
        <span>Checking AGMARKNET Telemetry...</span>
      </div>
    );
  }

  if (status === 'TEMPORARILY_UNAVAILABLE') {
    return (
      <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-rose-950/50 border border-rose-500/40 text-rose-400 text-xs font-medium ${className}`}>
        <WifiOff className="w-3.5 h-3.5 text-rose-400" />
        <span>Telemetry Temporarily Unavailable</span>
        {onRetry && (
          <button
            onClick={onRetry}
            className="ml-1.5 px-2 py-0.5 rounded bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 transition-colors flex items-center gap-1"
            title="Retry connecting to backend"
          >
            <RefreshCw className="w-3 h-3" />
            <span>Retry</span>
          </button>
        )}
      </div>
    );
  }

  if (status === 'NO_DATA') {
    return (
      <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-amber-950/40 border border-amber-500/30 text-amber-400 text-xs font-medium ${className}`}>
        <Database className="w-3.5 h-3.5 text-amber-400" />
        <span>No AGMARKNET Observations Found</span>
      </div>
    );
  }

  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-950/50 border border-emerald-500/40 text-emerald-400 text-xs font-medium ${className}`}>
      <Wifi className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
      <span>AGMARKNET Live Telemetry ({recordCount} {recordCount === 1 ? 'Record' : 'Records'})</span>
    </div>
  );
};
