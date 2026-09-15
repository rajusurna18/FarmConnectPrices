import React from 'react';
import { useUserRatingSummaryQuery } from '../api/marketplaceReviewsApi';

interface RatingSummaryCardProps {
  userId: string;
  userName?: string;
}

export const RatingSummaryCard: React.FC<RatingSummaryCardProps> = ({ userId, userName }) => {
  const { data: summary, isLoading, isError } = useUserRatingSummaryQuery(userId);

  if (isLoading) {
    return (
      <div className="bg-slate-900/60 border border-slate-800 p-5 rounded-2xl animate-pulse">
        <div className="h-4 bg-slate-800 rounded w-1/3 mb-3" />
        <div className="h-8 bg-slate-800 rounded w-1/2" />
      </div>
    );
  }

  if (isError || !summary) {
    return null;
  }

  const total = summary.totalReviews || 0;
  const avg = (summary.averageRating || 0).toFixed(1);

  const getPercentage = (count: number) => {
    if (!total || total === 0) return 0;
    return Math.round((count / total) * 100);
  };

  const stars = [
    { label: '5 Stars', count: summary.fiveStarCount || 0 },
    { label: '4 Stars', count: summary.fourStarCount || 0 },
    { label: '3 Stars', count: summary.threeStarCount || 0 },
    { label: '2 Stars', count: summary.twoStarCount || 0 },
    { label: '1 Star', count: summary.oneStarCount || 0 },
  ];

  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
      <div className="flex items-center justify-between border-b border-slate-800 pb-3">
        <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
          <span>🛡️</span> Trust & Rating Summary {userName ? `— ${userName}` : ''}
        </h4>
        <span className="text-[11px] font-medium text-emerald-400 bg-emerald-950/60 border border-emerald-800/60 px-2.5 py-0.5 rounded-full">
          Verified Reviews
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
        {/* Rating Score Badge */}
        <div className="flex flex-col items-center justify-center text-center p-4 bg-slate-950/80 border border-slate-800 rounded-xl">
          <span className="text-4xl font-extrabold text-amber-400">{avg}</span>
          <div className="flex items-center text-amber-400 text-sm my-1">
            {[1, 2, 3, 4, 5].map((s) => (
              <span key={s}>{Math.round(summary.averageRating || 0) >= s ? '★' : '☆'}</span>
            ))}
          </div>
          <span className="text-xs text-slate-400 font-medium">
            Based on {total} {total === 1 ? 'transaction' : 'transactions'}
          </span>
        </div>

        {/* Breakdown Bars */}
        <div className="md:col-span-2 space-y-1.5">
          {stars.map((s) => {
            const pct = getPercentage(s.count);
            return (
              <div key={s.label} className="flex items-center gap-3 text-xs">
                <span className="w-14 text-slate-400 font-mono text-[11px] text-right">{s.label}</span>
                <div className="flex-1 bg-slate-950 h-2 rounded-full overflow-hidden border border-slate-800">
                  <div
                    className="bg-amber-400 h-full rounded-full transition-all duration-500"
                    style={{ width: `${pct}%` }}
                  />
                </div>
                <span className="w-10 text-[11px] text-slate-400 font-mono text-right">{pct}%</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
