import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import {
  useDisputeDetailQuery,
  useCloseDisputeMutation,
  extractDisputeApiErrorMessage,
} from '../api/marketplaceDisputesApi';
import {
  DISPUTE_CATEGORY_LABELS,
  isDisputeActive,
} from '../../../types/dispute';
import { DisputeStatusBadge } from '../components/disputes/DisputeStatusBadge';
import { DisputeTimeline } from '../components/disputes/DisputeTimeline';
import { DisputeResponseForm } from '../components/disputes/DisputeResponseForm';
import { DisputeEvidenceSection } from '../components/disputes/DisputeEvidenceSection';

export const DisputeDetailPage: React.FC = () => {
  const { disputeId } = useParams<{ disputeId: string }>();
  const { currentUser } = useAuth();

  const [isConfirmingClose, setIsConfirmingClose] = useState(false);
  const [closeError, setCloseError] = useState<string | null>(null);

  const { data, isLoading, isError, refetch } = useDisputeDetailQuery(disputeId || '');
  const closeMutation = useCloseDisputeMutation();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8 flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="w-8 h-8 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-xs text-emerald-400 font-medium">Loading dispute details...</p>
        </div>
      </div>
    );
  }

  if (isError || !data || !data.dispute) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
        <div className="max-w-xl mx-auto bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center space-y-4">
          <span className="text-3xl block">⚠️</span>
          <h2 className="text-lg font-bold text-rose-300">Dispute Not Found</h2>
          <p className="text-xs text-rose-200">
            The dispute could not be loaded or you are not authorized to access this record.
          </p>
          <div className="pt-2 flex items-center justify-center gap-3">
            <button
              type="button"
              onClick={() => refetch()}
              className="px-4 py-2 bg-rose-900 hover:bg-rose-800 text-white text-xs font-semibold rounded-xl transition-colors"
            >
              Retry
            </button>
            <Link
              to="/marketplace/disputes"
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl border border-slate-700 transition-colors"
            >
              ← My Disputes
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const { dispute, responses = [], evidence = [], history = [] } = data;

  const currentUid = currentUser?.uid;
  const isCreator = currentUid === dispute.createdByUid;
  const isParticipant =
    currentUid === dispute.createdByUid ||
    currentUid === dispute.farmerUid ||
    currentUid === dispute.buyerUid;
  const active = isDisputeActive(dispute.status);
  const canClose = isCreator && active;
  const canParticipate = isParticipant && active;

  const handleCloseDispute = async () => {
    setCloseError(null);
    try {
      await closeMutation.mutateAsync(dispute.disputeId);
      setIsConfirmingClose(false);
    } catch (err) {
      const msg = extractDisputeApiErrorMessage(err);
      setCloseError(msg);
    }
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return '—';
    try {
      return new Date(isoString).toLocaleString('en-IN', {
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
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Navigation & Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-xl">⚠️</span>
              <h1 className="text-xl md:text-2xl font-bold text-slate-100 font-mono">
                {dispute.disputeId}
              </h1>
              <DisputeStatusBadge status={dispute.status} />
              <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-950/60 text-amber-300 border border-amber-800/60">
                {DISPUTE_CATEGORY_LABELS[dispute.issueCategory as keyof typeof DISPUTE_CATEGORY_LABELS] ||
                  dispute.issueCategory}
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Created on <strong className="text-slate-300">{formatDate(dispute.createdAt)}</strong> · Last active:{' '}
              <span className="text-slate-300">{formatDate(dispute.lastActivityAt)}</span>
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Link
              to={`/marketplace/orders/${dispute.orderId}`}
              className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-medium rounded-xl transition-colors"
            >
              View Order →
            </Link>
            <Link
              to="/marketplace/disputes"
              className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-medium rounded-xl transition-colors"
            >
              ← My Disputes
            </Link>
          </div>
        </div>

        {/* Voluntary Close Banner / Confirmation */}
        {canClose && (
          <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-md">
            <div>
              <h4 className="text-xs font-semibold text-slate-200">Voluntary Dispute Resolution</h4>
              <p className="text-[11px] text-slate-400 mt-0.5">
                If you have reached an agreement with the counterparty, you can close this dispute voluntarily.
              </p>
            </div>
            {!isConfirmingClose ? (
              <button
                type="button"
                onClick={() => setIsConfirmingClose(true)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl border border-slate-700 transition-colors whitespace-nowrap self-start sm:self-auto"
              >
                Close Dispute
              </button>
            ) : (
              <div className="flex items-center gap-2 self-start sm:self-auto">
                <button
                  type="button"
                  onClick={handleCloseDispute}
                  disabled={closeMutation.isPending}
                  className="px-4 py-2 bg-rose-700 hover:bg-rose-600 disabled:opacity-50 text-white text-xs font-semibold rounded-xl transition-colors flex items-center gap-1.5"
                >
                  {closeMutation.isPending && (
                    <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  )}
                  <span>Confirm Close</span>
                </button>
                <button
                  type="button"
                  onClick={() => setIsConfirmingClose(false)}
                  disabled={closeMutation.isPending}
                  className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-xl transition-colors"
                >
                  Cancel
                </button>
              </div>
            )}
          </div>
        )}

        {closeError && (
          <div className="p-3 bg-rose-950/60 border border-rose-800 text-rose-200 text-xs rounded-xl">
            {closeError}
          </div>
        )}

        {/* Dispute Overview Card */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-5">
          <div className="space-y-2 border-b border-slate-800/80 pb-4">
            <span className="text-xs font-semibold uppercase tracking-wider text-amber-400">
              Subject
            </span>
            <h2 className="text-lg md:text-xl font-bold text-slate-100">{dispute.subject}</h2>
          </div>

          <div className="space-y-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Description
            </span>
            <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-4 text-xs md:text-sm text-slate-200 leading-relaxed whitespace-pre-wrap font-sans">
              {dispute.description}
            </div>
          </div>

          {/* Participant and Order Metadata Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-2 text-xs border-t border-slate-800/80">
            <div>
              <span className="text-slate-400 block font-medium">Order ID</span>
              <Link
                to={`/marketplace/orders/${dispute.orderId}`}
                className="font-mono text-emerald-400 hover:underline font-semibold"
              >
                {dispute.orderId}
              </Link>
            </div>
            <div>
              <span className="text-slate-400 block font-medium">Produce</span>
              <span className="font-semibold text-slate-200">{dispute.cropName || 'Marketplace Item'}</span>
            </div>
            <div>
              <span className="text-slate-400 block font-medium">Initiated By</span>
              <span className="font-mono text-slate-200">
                {dispute.createdByRole} ({dispute.createdByUid.slice(0, 8)}...)
              </span>
            </div>
            <div>
              <span className="text-slate-400 block font-medium">Counterparty</span>
              <span className="font-mono text-slate-200">
                {dispute.createdByUid === dispute.farmerUid ? (
                  <>Buyer: {dispute.buyerRole} ({dispute.buyerUid.slice(0, 8)}...)</>
                ) : (
                  <>Farmer: {dispute.farmerUid.slice(0, 8)}...</>
                )}
              </span>
            </div>
          </div>
        </div>

        {/* Resolution Outcome Banner */}
        {dispute.resolutionOutcome || dispute.resolutionSummary ? (
          <div className="bg-emerald-950/40 border border-emerald-800/70 rounded-2xl p-5 shadow-lg space-y-3">
            <div className="flex items-center gap-2">
              <span className="text-xl">⚖️</span>
              <h3 className="text-sm font-bold text-emerald-300 uppercase tracking-wider">
                Adjudication & Resolution
              </h3>
              {dispute.resolutionOutcome && (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-semibold bg-emerald-900/60 text-emerald-200 border border-emerald-700/60">
                  {dispute.resolutionOutcome}
                </span>
              )}
            </div>

            {dispute.resolutionSummary && (
              <p className="text-xs text-emerald-100 leading-relaxed bg-emerald-950/60 border border-emerald-900/60 rounded-xl p-3">
                {dispute.resolutionSummary}
              </p>
            )}

            <div className="flex flex-wrap items-center gap-4 text-[11px] text-emerald-300/80 pt-1">
              {dispute.resolvedBy && (
                <span>
                  Resolved by: <strong className="font-mono text-emerald-200">{dispute.resolvedBy}</strong>
                </span>
              )}
              {dispute.resolvedAt && (
                <span>
                  Resolved at: <strong className="font-mono text-emerald-200">{formatDate(dispute.resolvedAt)}</strong>
                </span>
              )}
              {dispute.closedAt && (
                <span>
                  Closed at: <strong className="font-mono text-emerald-200">{formatDate(dispute.closedAt)}</strong>
                </span>
              )}
            </div>
          </div>
        ) : (
          <div className="bg-slate-900/40 border border-slate-800 rounded-2xl p-4 text-xs text-slate-400 flex items-center gap-3">
            <span className="text-lg">⏳</span>
            <span>
              Resolution has not yet been finalized. Participants may exchange messages and register supporting evidence below.
            </span>
          </div>
        )}

        {/* Messages & Counterparty Responses */}
        <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl p-6 shadow-xl space-y-6">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-4">
            <div className="flex items-center gap-2">
              <span className="text-lg">💬</span>
              <h3 className="text-sm font-bold text-slate-200">
                Responses & Messages ({responses.length})
              </h3>
            </div>
            <span className="text-[11px] text-slate-400">
              Visible to all order participants
            </span>
          </div>

          {/* Response List */}
          {responses.length === 0 ? (
            <div className="text-center py-6 text-xs text-slate-500">
              No response messages have been posted yet.
            </div>
          ) : (
            <div className="space-y-3">
              {responses.map((resp) => {
                const isOwn = resp.authorUid === currentUid;
                return (
                  <div
                    key={resp.responseId}
                    className={`p-4 rounded-xl border text-xs space-y-2 ${
                      isOwn
                        ? 'bg-slate-800/70 border-slate-700/80 ml-4 sm:ml-8'
                        : 'bg-slate-950/70 border-slate-800/80 mr-4 sm:mr-8'
                    }`}
                  >
                    <div className="flex items-center justify-between text-[11px] text-slate-400">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-slate-200">
                          {resp.authorRole}
                        </span>
                        <span className="font-mono text-slate-400">
                          ({resp.authorUid.slice(0, 8)}...)
                        </span>
                        {isOwn && (
                          <span className="px-1.5 py-0.5 rounded text-[10px] bg-slate-700 text-slate-300 font-medium">
                            You
                          </span>
                        )}
                      </div>
                      <span className="font-mono">{formatDate(resp.createdAt)}</span>
                    </div>
                    <p className="text-slate-200 leading-relaxed whitespace-pre-wrap">
                      {resp.message}
                    </p>
                  </div>
                );
              })}
            </div>
          )}

          {/* Response Form */}
          {canParticipate ? (
            <div className="pt-4 border-t border-slate-800/80">
              <h4 className="text-xs font-semibold text-slate-300 mb-3">Add Response</h4>
              <DisputeResponseForm disputeId={dispute.disputeId} />
            </div>
          ) : !active ? (
            <div className="pt-4 border-t border-slate-800/80 text-center py-3 text-xs text-slate-500">
              This dispute is {dispute.status.toLowerCase()}. Further responses cannot be added.
            </div>
          ) : null}
        </div>

        {/* Evidence Section */}
        <DisputeEvidenceSection
          disputeId={dispute.disputeId}
          evidence={evidence}
          canAddEvidence={canParticipate}
        />

        {/* Audit Trail Timeline */}
        <DisputeTimeline history={history} />
      </div>
    </div>
  );
};
