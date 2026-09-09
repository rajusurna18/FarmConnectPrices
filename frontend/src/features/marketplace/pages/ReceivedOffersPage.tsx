import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  useReceivedOffersQuery,
  useAcceptOfferMutation,
  useRejectOfferMutation,
} from '../api/marketplaceOffersApi';
import { CounterOfferModal } from '../components/CounterOfferModal';
import type { Offer } from '../../../types/offer';

export const ReceivedOffersPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);
  const [counterModalOffer, setCounterModalOffer] = useState<Offer | null>(null);

  const { data, isLoading, isError, refetch } = useReceivedOffersQuery(statusFilter, page, 20);

  const acceptMutation = useAcceptOfferMutation();
  const rejectMutation = useRejectOfferMutation();

  const handleAccept = async (offerId: string) => {
    if (window.confirm('Are you sure you want to accept this commercial offer?')) {
      await acceptMutation.mutateAsync(offerId);
      refetch();
    }
  };

  const handleReject = async (offerId: string) => {
    if (window.confirm('Are you sure you want to reject this offer?')) {
      await rejectMutation.mutateAsync(offerId);
      refetch();
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-amber-950/80 text-amber-300 border border-amber-700/60">Requires Review</span>;
      case 'COUNTERED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-cyan-950/80 text-cyan-300 border border-cyan-700/60">Counter Sent</span>;
      case 'ACCEPTED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-950/80 text-emerald-300 border border-emerald-700/60">Accepted</span>;
      case 'REJECTED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-rose-950/80 text-rose-300 border border-rose-700/60">Rejected</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-slate-800 text-slate-300 border border-slate-600">Cancelled</span>;
      case 'EXPIRED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-slate-800 text-amber-400 border border-slate-700">Expired</span>;
      default:
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-slate-800 text-slate-300">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-emerald-900/40 pb-5">
          <div>
            <h1 className="text-2xl font-bold text-emerald-300 flex items-center gap-2">
              <span>📥 Received Commercial Offers</span>
            </h1>
            <p className="text-xs text-emerald-400/80 mt-1">
              Review, counter, accept, or reject commercial proposals received on your produce listings.
            </p>
          </div>
          <Link
            to="/marketplace/my-listings"
            className="px-4 py-2 bg-emerald-900/60 hover:bg-emerald-800/80 border border-emerald-700/50 rounded-xl text-xs font-medium text-emerald-200 transition-colors"
          >
            ← My Produce Listings
          </Link>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center justify-between gap-4 bg-emerald-950/60 border border-emerald-800/40 rounded-2xl p-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-emerald-400">Filter Status:</span>
            {['ALL', 'PENDING', 'COUNTERED', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED'].map((s) => (
              <button
                key={s}
                onClick={() => {
                  setStatusFilter(s);
                  setPage(0);
                }}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  statusFilter === s
                    ? 'bg-emerald-600 text-white font-semibold'
                    : 'bg-emerald-900/40 text-emerald-300 hover:bg-emerald-800/50'
                }`}
              >
                {s}
              </button>
            ))}
          </div>

          <button
            onClick={() => refetch()}
            className="px-3 py-1.5 bg-emerald-900/40 hover:bg-emerald-800/60 text-emerald-300 text-xs rounded-lg transition-colors"
          >
            🔄 Refresh
          </button>
        </div>

        {/* Content */}
        {isLoading ? (
          <div className="text-center py-12 text-emerald-400 text-sm">Loading received offers...</div>
        ) : isError ? (
          <div className="bg-rose-950/60 border border-rose-800 text-rose-300 p-4 rounded-xl text-xs">
            Failed to load received offers. Please try again.
          </div>
        ) : !data || data.items.length === 0 ? (
          <div className="bg-emerald-950/40 border border-emerald-800/40 rounded-2xl p-12 text-center text-slate-400 text-sm">
            No received offers found for the selected status.
          </div>
        ) : (
          <div className="space-y-4">
            {data.items.map((offer) => (
              <div
                key={offer.offerId}
                className="bg-emerald-950/60 border border-emerald-800/50 hover:border-emerald-700/70 rounded-2xl p-5 shadow-lg transition-all"
              >
                <div className="flex flex-wrap items-start justify-between gap-4 mb-3">
                  <div>
                    <div className="flex items-center gap-3">
                      <span className="text-lg font-bold text-emerald-200">{offer.cropName}</span>
                      {getStatusBadge(offer.status)}
                      <span className="text-xs text-slate-400 font-mono">Round #{offer.roundNumber}</span>
                    </div>
                    <p className="text-xs text-emerald-400/80 mt-0.5">
                      Initiated by: <strong className="text-emerald-200">{offer.buyerRole || 'Buyer'}</strong> ({offer.buyerUid.substring(0, 8)}...)
                    </p>
                    <p className="text-xs text-slate-400 mt-0.5">
                      Your Listing Asking Price Reference: ₹{offer.askingPriceReference?.toLocaleString('en-IN')} / {offer.priceUnit}
                    </p>
                  </div>

                  <div className="text-right">
                    <span className="text-xs text-slate-400 block">Offered Price</span>
                    <span className="text-lg font-bold text-amber-300">
                      ₹{offer.offeredPrice.toLocaleString('en-IN')}{' '}
                      <span className="text-xs font-normal text-emerald-400">/ {offer.priceUnit}</span>
                    </span>
                    <span className="text-xs text-slate-300 block">
                      Quantity: <strong>{offer.offeredQuantity} {offer.quantityUnit}</strong>
                    </span>
                  </div>
                </div>

                {offer.message && (
                  <p className="text-xs text-slate-300 bg-emerald-900/30 border border-emerald-800/40 rounded-xl p-3 mb-4 italic font-serif">
                    "{offer.message}"
                  </p>
                )}

                {/* Actions */}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-emerald-900/40 text-xs">
                  <span className="text-slate-400">
                    Received: {new Date(offer.createdAt).toLocaleDateString('en-IN')}
                  </span>

                  <div className="flex items-center gap-2">
                    <Link
                      to={`/marketplace/offers/${offer.offerId}`}
                      className="px-3.5 py-1.5 bg-emerald-900/60 hover:bg-emerald-800/80 border border-emerald-700/60 rounded-xl text-emerald-200 font-medium transition-colors"
                    >
                      View Timeline ({offer.rounds?.length || 1})
                    </Link>

                    {offer.canAccept && (
                      <button
                        onClick={() => handleAccept(offer.offerId)}
                        disabled={acceptMutation.isPending}
                        className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-medium rounded-xl shadow transition-colors"
                      >
                        Accept Proposal
                      </button>
                    )}

                    {offer.canCounter && (
                      <button
                        onClick={() => setCounterModalOffer(offer)}
                        className="px-3.5 py-1.5 bg-amber-600 hover:bg-amber-500 text-white font-medium rounded-xl shadow transition-colors"
                      >
                        Counter Offer
                      </button>
                    )}

                    {offer.canReject && (
                      <button
                        onClick={() => handleReject(offer.offerId)}
                        disabled={rejectMutation.isPending}
                        className="px-3.5 py-1.5 bg-rose-950/80 hover:bg-rose-900 border border-rose-700/60 text-rose-300 font-medium rounded-xl transition-colors"
                      >
                        Reject Offer
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {/* Pagination */}
            {data.totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 text-xs text-slate-400">
                <span>Page {page + 1} of {data.totalPages}</span>
                <div className="flex items-center gap-2">
                  <button
                    disabled={page === 0}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="px-3 py-1.5 bg-emerald-900/40 disabled:opacity-40 rounded-lg"
                  >
                    Previous
                  </button>
                  <button
                    disabled={!data.hasNext}
                    onClick={() => setPage((p) => p + 1)}
                    className="px-3 py-1.5 bg-emerald-900/40 disabled:opacity-40 rounded-lg"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {counterModalOffer && (
        <CounterOfferModal
          offer={counterModalOffer}
          isOpen={Boolean(counterModalOffer)}
          onClose={() => setCounterModalOffer(null)}
          onSuccess={() => refetch()}
        />
      )}
    </div>
  );
};
