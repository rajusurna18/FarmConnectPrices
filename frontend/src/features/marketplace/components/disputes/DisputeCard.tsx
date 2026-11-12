import React from 'react';
import { Link } from 'react-router-dom';
import {
  type DisputeResponse,
  type DisputeCategory,
  DISPUTE_CATEGORY_LABELS,
} from '../../../../types/dispute';
import { DisputeStatusBadge } from './DisputeStatusBadge';

interface DisputeCardProps {
  dispute: DisputeResponse;
  viewMode?: 'mine' | 'related';
}

export const DisputeCard: React.FC<DisputeCardProps> = ({ dispute, viewMode = 'mine' }) => {
  const categoryLabel =
    DISPUTE_CATEGORY_LABELS[dispute.issueCategory as DisputeCategory] || dispute.issueCategory;

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <article
      className="bg-slate-900/80 hover:bg-slate-900 border border-slate-800 hover:border-slate-700/80 rounded-2xl p-5 sm:p-6 transition-all duration-200 shadow-lg space-y-4"
      aria-labelledby={`dispute-title-${dispute.disputeId}`}
    >
      {/* Header Row */}
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-800/80 pb-3">
        <div className="space-y-1">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs font-mono text-emerald-400 bg-emerald-950/60 border border-emerald-800/40 px-2 py-0.5 rounded-md">
              ID: {dispute.disputeId}
            </span>
            <span className="text-xs font-medium text-slate-400 bg-slate-800/80 px-2 py-0.5 rounded-md">
              Order: <span className="text-slate-200 font-mono">{dispute.orderId}</span>
            </span>
            {dispute.cropName && (
              <span className="text-xs font-medium text-slate-300">
                🌾 {dispute.cropName}
              </span>
            )}
          </div>
          <h3
            id={`dispute-title-${dispute.disputeId}`}
            className="text-base font-bold text-slate-100 hover:text-emerald-300 transition-colors pt-1"
          >
            <Link to={`/marketplace/disputes/${dispute.disputeId}`}>
              {dispute.subject}
            </Link>
          </h3>
        </div>

        <div className="flex items-center gap-2">
          <DisputeStatusBadge status={dispute.status} />
        </div>
      </div>

      {/* Category & Description */}
      <div className="space-y-2 text-xs">
        <div className="flex items-center gap-2 text-slate-300">
          <span className="font-semibold text-slate-400">Category:</span>
          <span className="px-2 py-0.5 bg-slate-800 rounded text-slate-200 font-medium">
            {categoryLabel}
          </span>
        </div>

        <p className="text-slate-300 line-clamp-2 leading-relaxed">
          {dispute.description}
        </p>

        {dispute.resolutionOutcome && (
          <div className="flex items-center gap-2 pt-1 text-xs">
            <span className="text-slate-400">Resolution:</span>
            <span className="px-2 py-0.5 rounded text-xs font-mono bg-emerald-950/60 text-emerald-300 border border-emerald-800/60 font-semibold">
              {dispute.resolutionOutcome}
            </span>
            {dispute.resolutionSummary && (
              <span className="text-slate-300 truncate max-w-xs">{dispute.resolutionSummary}</span>
            )}
          </div>
        )}
      </div>

      {/* Participants & Metadata Footer */}
      <div className="pt-3 border-t border-slate-800/60 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-400">
        <div className="flex items-center gap-3 flex-wrap">
          <div>
            <span className="text-slate-500">Initiated by: </span>
            <span className="text-slate-300 font-medium">
              {viewMode === 'mine' ? 'You' : dispute.createdByRole} ({dispute.createdByRole})
            </span>
          </div>
          <span className="text-slate-700">•</span>
          <div>
            <span className="text-slate-500">Reported: </span>
            <span className="text-slate-300 font-mono">{formatDate(dispute.createdAt)}</span>
          </div>
          {dispute.lastActivityAt && dispute.lastActivityAt !== dispute.createdAt && (
            <>
              <span className="text-slate-700">•</span>
              <div>
                <span className="text-slate-500">Active: </span>
                <span className="text-slate-300 font-mono">{formatDate(dispute.lastActivityAt)}</span>
              </div>
            </>
          )}
        </div>

        <Link
          to={`/marketplace/disputes/${dispute.disputeId}`}
          className="inline-flex items-center gap-1 px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-xl shadow-md transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400"
          aria-label={`View details for dispute ${dispute.disputeId}`}
        >
          <span>View Details</span>
          <span aria-hidden="true">→</span>
        </Link>
      </div>
    </article>
  );
};
