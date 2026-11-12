import React from 'react';
import {
  type DisputeStatus,
  DISPUTE_STATUS_LABELS,
} from '../../../../types/dispute';

interface DisputeStatusBadgeProps {
  status: DisputeStatus | string;
  className?: string;
}

export const DisputeStatusBadge: React.FC<DisputeStatusBadgeProps> = ({ status, className = '' }) => {
  const normalizedStatus = status ? status.toUpperCase() : 'UNKNOWN';

  const getBadgeStyle = () => {
    switch (normalizedStatus) {
      case 'OPEN':
        return 'bg-amber-950/80 border-amber-700/60 text-amber-300';
      case 'UNDER_REVIEW':
        return 'bg-sky-950/80 border-sky-700/60 text-sky-300';
      case 'WAITING_FOR_INFORMATION':
        return 'bg-purple-950/80 border-purple-700/60 text-purple-300';
      case 'RESOLVED':
        return 'bg-emerald-950/80 border-emerald-700/60 text-emerald-300';
      case 'REJECTED':
        return 'bg-rose-950/80 border-rose-700/60 text-rose-300';
      case 'CLOSED':
        return 'bg-slate-800 border-slate-700 text-slate-300';
      default:
        return 'bg-slate-800/80 border-slate-700 text-slate-300';
    }
  };

  const label = DISPUTE_STATUS_LABELS[normalizedStatus as DisputeStatus] || normalizedStatus;

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border tracking-wide uppercase ${getBadgeStyle()} ${className}`}
      aria-label={`Dispute status: ${label}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current mr-1.5 opacity-80" aria-hidden="true" />
      {label}
    </span>
  );
};
