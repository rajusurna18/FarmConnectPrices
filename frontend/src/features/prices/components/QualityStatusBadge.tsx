import React from 'react';
import { ShieldCheck, AlertCircle, XCircle } from 'lucide-react';
import type { PriceQualityStatus } from '../types';

interface QualityStatusBadgeProps {
  status: PriceQualityStatus;
  className?: string;
}

export const QualityStatusBadge: React.FC<QualityStatusBadgeProps> = ({ status, className = '' }) => {
  if (status === 'VERIFIED') {
    return (
      <span className={`inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-semibold ${className}`}>
        <ShieldCheck className="w-3.5 h-3.5 shrink-0" />
        <span>Verified Data</span>
      </span>
    );
  }

  if (status === 'UNVERIFIED') {
    return (
      <span className={`inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/30 text-xs font-semibold ${className}`}>
        <AlertCircle className="w-3.5 h-3.5 shrink-0" />
        <span>Unverified Observation</span>
      </span>
    );
  }

  return (
    <span className={`inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/30 text-xs font-semibold ${className}`}>
      <XCircle className="w-3.5 h-3.5 shrink-0" />
      <span>Rejected Record</span>
    </span>
  );
};
