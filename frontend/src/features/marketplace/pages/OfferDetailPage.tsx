import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  useOfferDetailQuery,
  useOfferHistoryQuery,
  useAcceptOfferMutation,
  useRejectOfferMutation,
  useCancelOfferMutation,
} from '../api/marketplaceOffersApi';
import { NegotiationTimeline } from '../components/NegotiationTimeline';
import { CounterOfferModal } from '../components/CounterOfferModal';

export const OfferDetailPage: React.FC = () => {
  const { offerId } = useParams<{ offerId: string }>();

  const [isCounterOpen, setIsCounterOpen] = useState<boolean>(false);

  const { data: offer, isLoading, isError, refetch } = useOfferDetailQuery(offerId || '');
  const { data: history } = useOfferHistoryQuery(offerId || '');

  const acceptMutation = useAcceptOfferMutation();
  const rejectMutation = useRejectOfferMutation();
  const cancelMutation = useCancelOfferMutation();

  if (isLoading) {
    return <div className="min-h-screen bg-slate-950 text-emerald-400 text-center py-16 text-sm">Loading offer details...</div>;
  }

  if (isError || !offer) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
        <div className="max-w-xl mx-auto bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center space-y-4">
          <h2 className="text-lg font-bold text-rose-300">Offer Not Found</h2>
          <p className="text-xs text-rose-200">The offer could not be loaded or you are not authorized to view it.</p>
          <Link to="/marketplace" className="inline-block px-4 py-2 bg-rose-900 text-xs rounded-xl text-white font-medium">
            Return to Marketplace
          </Link>
        </div>
      </div>
    );
  }

  const handleAccept = async () => {
    if (window.confirm('Are you sure you want to accept this proposal?')) {
      await acceptMutation.mutateAsync(offer.offerId);
      refetch();
    }
  };

  const handleReject = async () => {
    if (window.confirm('Are you sure you want to reject this offer?')) {
      await rejectMutation.mutateAsync(offer.offerId);
      refetch();
    }
  };

  const handleCancel = async () => {
    if (window.confirm('Are you sure you want to cancel your offer?')) {
      await cancelMutation.mutateAsync(offer.offerId);
      refetch();
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center justify-between">
          <Link
            to="/marketplace"
            className="text-xs font-medium text-emerald-400 hover:text-emerald-300 transition-colors flex items-center gap-1"
          >
            ← Back to Marketplace
          </Link>
          <span className="text-xs font-mono text-slate-400">Offer ID: {offer.offerId}</span>
        </div>

        {/* Header Overview Card */}
        <div className="bg-emerald-950/60 border border-emerald-800/50 rounded-2xl p-6 shadow-xl space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-emerald-200 flex items-center gap-2">
                <span>🌾 {offer.cropName}</span>
              </h1>
              <p className="text-xs text-emerald-400 mt-1">
                Listing Reference Asking Price: ₹{offer.askingPriceReference?.toLocaleString('en-IN')} / {offer.priceUnit}
              </p>
            </div>

            <div className="text-right">
              <span className="text-xs text-slate-400 block">Current Active Proposal</span>
              <span className="text-2xl font-extrabold text-emerald-300">
                ₹{offer.offeredPrice.toLocaleString('en-IN')}{' '}
                <span className="text-sm font-normal text-emerald-400">/ {offer.priceUnit}</span>
              </span>
              <span className="text-xs text-slate-300 block">
                Offered Quantity: <strong>{offer.offeredQuantity} {offer.quantityUnit}</strong>
              </span>
            </div>
          </div>

          {/* Action Bar */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-emerald-900/40">
            <div className="flex items-center gap-2 text-xs">
              <span className="text-slate-400">Status:</span>
              <span className="font-semibold text-emerald-300">{offer.status}</span>
              <span className="text-slate-400 ml-2">Round:</span>
              <span className="font-mono text-slate-200">{offer.roundNumber} / 10</span>
            </div>

            <div className="flex items-center gap-2">
              {offer.canAccept && (
                <button
                  onClick={handleAccept}
                  disabled={acceptMutation.isPending}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Accept Proposal
                </button>
              )}

              {offer.canCounter && (
                <button
                  onClick={() => setIsCounterOpen(true)}
                  className="px-4 py-2 bg-amber-600 hover:bg-amber-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Submit Counter
                </button>
              )}

              {offer.canReject && (
                <button
                  onClick={handleReject}
                  disabled={rejectMutation.isPending}
                  className="px-4 py-2 bg-rose-950/80 hover:bg-rose-900 border border-rose-700/60 text-rose-300 font-medium text-xs rounded-xl transition-colors"
                >
                  Reject
                </button>
              )}

              {offer.canCancel && (
                <button
                  onClick={handleCancel}
                  disabled={cancelMutation.isPending}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium text-xs rounded-xl transition-colors"
                >
                  Cancel Offer
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Negotiation History Section */}
        <div className="bg-emerald-950/40 border border-emerald-800/40 rounded-2xl p-6 space-y-4">
          <h2 className="text-lg font-bold text-emerald-300 flex items-center gap-2">
            <span>📜 Complete Negotiation Timeline</span>
          </h2>

          <NegotiationTimeline offer={offer} rounds={history || offer.rounds || []} />
        </div>
      </div>

      {isCounterOpen && (
        <CounterOfferModal
          offer={offer}
          isOpen={isCounterOpen}
          onClose={() => setIsCounterOpen(false)}
          onSuccess={() => refetch()}
        />
      )}
    </div>
  );
};
