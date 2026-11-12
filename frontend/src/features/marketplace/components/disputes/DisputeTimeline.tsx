import React from 'react';
import type { DisputeHistoryEntry, DisputeHistoryAction } from '../../../../types/dispute';

interface DisputeTimelineProps {
  history: DisputeHistoryEntry[];
  className?: string;
}

const ACTION_LABELS: Record<DisputeHistoryAction, { label: string; icon: string; color: string }> = {
  DISPUTE_CREATED: { label: 'Dispute Opened', icon: '⚠️', color: 'text-amber-400 border-amber-600/50 bg-amber-950/40' },
  RESPONSE_ADDED: { label: 'Response Submitted', icon: '💬', color: 'text-sky-400 border-sky-600/50 bg-sky-950/40' },
  EVIDENCE_ADDED: { label: 'Evidence Registered', icon: '📎', color: 'text-indigo-400 border-indigo-600/50 bg-indigo-950/40' },
  STATUS_CHANGED: { label: 'Status Transitioned', icon: '🔄', color: 'text-purple-400 border-purple-600/50 bg-purple-950/40' },
  INFORMATION_REQUESTED: { label: 'Information Requested', icon: '❓', color: 'text-yellow-400 border-yellow-600/50 bg-yellow-950/40' },
  INFORMATION_SUBMITTED: { label: 'Information Provided', icon: '📝', color: 'text-blue-400 border-blue-600/50 bg-blue-950/40' },
  DISPUTE_RESOLVED: { label: 'Dispute Resolved', icon: '✅', color: 'text-emerald-400 border-emerald-600/50 bg-emerald-950/40' },
  DISPUTE_REJECTED: { label: 'Dispute Rejected', icon: '❌', color: 'text-rose-400 border-rose-600/50 bg-rose-950/40' },
  DISPUTE_CLOSED: { label: 'Dispute Closed', icon: '🔒', color: 'text-slate-400 border-slate-600/50 bg-slate-900/60' },
};

export const DisputeTimeline: React.FC<DisputeTimelineProps> = ({ history, className = '' }) => {
  const formatDate = (isoString?: string) => {
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

  if (!history || history.length === 0) {
    return (
      <div className="text-center py-6 text-slate-500 text-xs">
        No audit history entries recorded for this dispute.
      </div>
    );
  }

  return (
    <div className={`relative pl-6 sm:pl-8 border-l-2 border-slate-800 space-y-6 ${className}`} role="feed" aria-label="Dispute audit history timeline">
      {history.map((entry, index) => {
        const actionMeta =
          ACTION_LABELS[entry.actionType as DisputeHistoryAction] || {
            label: entry.actionType,
            icon: '📋',
            color: 'text-slate-400 border-slate-700 bg-slate-900',
          };

        return (
          <div key={entry.historyId || index} className="relative group">
            {/* Timeline bullet */}
            <span
              className={`absolute -left-[31px] sm:-left-[39px] top-1.5 w-6 h-6 rounded-full border flex items-center justify-center text-xs shadow-md ${actionMeta.color}`}
              aria-hidden="true"
            >
              <span>{actionMeta.icon}</span>
            </span>

            {/* Content card */}
            <div className="bg-slate-900/60 border border-slate-800/80 rounded-xl p-4 text-xs space-y-1.5 transition-colors group-hover:border-slate-700">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-slate-200">{actionMeta.label}</span>
                  {entry.actorRole && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-slate-800 text-slate-300">
                      {entry.actorRole}
                    </span>
                  )}
                </div>
                <time className="text-slate-500 font-mono text-[11px]" dateTime={entry.createdAt}>
                  {formatDate(entry.createdAt)}
                </time>
              </div>

              <p className="text-slate-300 leading-relaxed pt-0.5">{entry.description}</p>

              {entry.previousStatus && entry.newStatus && entry.previousStatus !== entry.newStatus && (
                <div className="pt-1 flex items-center gap-2 text-[11px] text-slate-400 font-mono">
                  <span className="text-slate-500">Status:</span>
                  <span className="px-1.5 py-0.5 bg-slate-800 rounded text-slate-300">{entry.previousStatus}</span>
                  <span>→</span>
                  <span className="px-1.5 py-0.5 bg-emerald-950/80 border border-emerald-800/60 text-emerald-300 rounded font-bold">
                    {entry.newStatus}
                  </span>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
