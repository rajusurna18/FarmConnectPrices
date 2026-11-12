import React from 'react';

interface DisputeFiltersProps {
  selectedStatus: string;
  onSelectStatus: (status: string) => void;
  className?: string;
}

const FILTER_OPTIONS: Array<{ key: string; label: string }> = [
  { key: 'ALL', label: 'All Disputes' },
  { key: 'OPEN', label: 'Open' },
  { key: 'UNDER_REVIEW', label: 'Under Review' },
  { key: 'WAITING_FOR_INFORMATION', label: 'Waiting Info' },
  { key: 'RESOLVED', label: 'Resolved' },
  { key: 'REJECTED', label: 'Rejected' },
  { key: 'CLOSED', label: 'Closed' },
];

export const DisputeFilters: React.FC<DisputeFiltersProps> = ({
  selectedStatus,
  onSelectStatus,
  className = '',
}) => {
  return (
    <div
      className={`flex items-center gap-2 overflow-x-auto pb-2 scrollbar-thin scrollbar-thumb-slate-800 ${className}`}
      role="region"
      aria-label="Dispute status filters"
    >
      {FILTER_OPTIONS.map((opt) => {
        const isActive = selectedStatus === opt.key;
        return (
          <button
            key={opt.key}
            type="button"
            onClick={() => onSelectStatus(opt.key)}
            className={`px-3 py-1.5 text-xs font-medium rounded-xl whitespace-nowrap transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400 ${
              isActive
                ? 'bg-emerald-600 text-white font-semibold shadow-md shadow-emerald-950/50'
                : 'bg-slate-900/80 text-slate-400 border border-slate-800 hover:text-slate-200 hover:bg-slate-800'
            }`}
            aria-pressed={isActive}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
};
