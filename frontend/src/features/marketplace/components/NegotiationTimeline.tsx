import React from 'react';
import type { Offer, OfferRound } from '../../../types/offer';

interface NegotiationTimelineProps {
  offer: Offer;
  rounds: OfferRound[];
}

export const NegotiationTimeline: React.FC<NegotiationTimelineProps> = ({ offer, rounds }) => {
  if (!rounds || rounds.length === 0) {
    return (
      <div className="text-center py-6 text-slate-400 text-xs italic">
        No negotiation history recorded yet.
      </div>
    );
  }

  const getActionBadge = (action: string) => {
    switch (action) {
      case 'OFFER':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-950 text-emerald-300 border border-emerald-700/60">Initial Offer</span>;
      case 'COUNTER':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-950 text-amber-300 border border-amber-700/60">Counter-Offer</span>;
      case 'ACCEPT':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-800 text-white border border-emerald-500">Accepted</span>;
      case 'REJECT':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-950 text-rose-300 border border-rose-700/60">Rejected</span>;
      case 'CANCEL':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-800 text-slate-300 border border-slate-600">Cancelled</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-800 text-slate-300">{action}</span>;
    }
  };

  const getSenderLabel = (round: OfferRound) => {
    if (round.senderUid === offer.buyerUid) {
      return `Buyer / Customer (${round.senderRole || 'Buyer'})`;
    }
    if (round.senderUid === offer.farmerId) {
      return `Farmer (Listing Owner)`;
    }
    return round.senderRole || 'Participant';
  };

  return (
    <div className="space-y-6">
      <div className="relative pl-6 border-l-2 border-emerald-800/60 space-y-6">
        {rounds.map((round, idx) => (
          <div key={idx} className="relative group">
            {/* Dot marker */}
            <div className={`absolute -left-[31px] top-1 w-4 h-4 rounded-full border-2 flex items-center justify-center text-[9px] font-bold ${
              round.action === 'ACCEPT'
                ? 'bg-emerald-500 border-emerald-300 text-slate-950'
                : round.action === 'REJECT'
                ? 'bg-rose-600 border-rose-400 text-white'
                : round.action === 'CANCEL'
                ? 'bg-slate-600 border-slate-400 text-white'
                : 'bg-emerald-950 border-emerald-500 text-emerald-300'
            }`}>
              {round.roundNumber}
            </div>

            {/* Content card */}
            <div className="bg-emerald-950/60 border border-emerald-800/50 rounded-xl p-4 shadow-sm hover:border-emerald-700/60 transition-colors">
              <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-emerald-200">{getSenderLabel(round)}</span>
                  {getActionBadge(round.action)}
                </div>
                <span className="text-[11px] text-slate-400">
                  {new Date(round.timestamp).toLocaleString('en-IN', {
                    day: '2-digit',
                    month: 'short',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>
              </div>

              <div className="flex items-baseline gap-2 mb-1">
                <span className="text-base font-bold text-emerald-100">
                  ₹{round.price?.toLocaleString('en-IN')} <span className="text-xs font-normal text-emerald-400">/ {round.priceUnit}</span>
                </span>
                <span className="text-xs text-slate-300">
                  • Quantity: <strong className="text-emerald-200">{round.quantity} {round.quantityUnit}</strong>
                </span>
              </div>

              {round.message && (
                <p className="text-xs text-slate-300 bg-emerald-900/30 border border-emerald-800/40 rounded-lg p-2.5 mt-2 italic font-serif">
                  "{round.message}"
                </p>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Terminal State Cards */}
      {offer.status === 'ACCEPTED' && (
        <div className="bg-gradient-to-r from-emerald-950 via-emerald-900 to-teal-950 border-2 border-emerald-500 rounded-2xl p-5 shadow-xl text-slate-100">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 rounded-full bg-emerald-500/20 border border-emerald-400/50 flex items-center justify-center text-xl">
              🤝
            </div>
            <div>
              <h3 className="text-lg font-bold text-emerald-200">Commercial Negotiation Accepted</h3>
              <p className="text-xs text-emerald-400">Official agreed marketplace price proposal</p>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 bg-emerald-950/80 border border-emerald-800/60 rounded-xl p-3 text-xs mb-3">
            <div>
              <span className="text-slate-400 block">Crop</span>
              <span className="font-semibold text-emerald-200">{offer.cropName}</span>
            </div>
            <div>
              <span className="text-slate-400 block">Agreed Quantity</span>
              <span className="font-semibold text-emerald-200">{offer.agreedQuantity} {offer.quantityUnit}</span>
            </div>
            <div>
              <span className="text-slate-400 block">Agreed Commercial Price</span>
              <span className="font-bold text-emerald-300">₹{offer.agreedPrice?.toLocaleString('en-IN')} / {offer.agreedPriceUnit}</span>
            </div>
          </div>

          <div className="bg-amber-950/50 border border-amber-800/60 rounded-xl p-3 text-xs text-amber-200 flex items-center gap-2">
            <span>ℹ️</span>
            <span>
              <strong>Note:</strong> Order creation, inventory commitment, payment, and delivery logistics will be handled in a future module.
            </span>
          </div>
        </div>
      )}

      {offer.status === 'REJECTED' && (
        <div className="bg-rose-950/40 border border-rose-800/60 rounded-xl p-4 text-xs text-rose-300 flex items-center gap-3">
          <span className="text-xl">❌</span>
          <div>
            <strong className="block text-sm font-semibold text-rose-200">Negotiation Rejected</strong>
            This commercial offer thread was closed without agreement.
          </div>
        </div>
      )}

      {offer.status === 'CANCELLED' && (
        <div className="bg-slate-900/60 border border-slate-700/60 rounded-xl p-4 text-xs text-slate-300 flex items-center gap-3">
          <span className="text-xl">🚫</span>
          <div>
            <strong className="block text-sm font-semibold text-slate-200">Offer Cancelled</strong>
            This offer was cancelled by the buyer before completion.
          </div>
        </div>
      )}

      {offer.status === 'EXPIRED' && (
        <div className="bg-amber-950/40 border border-amber-800/60 rounded-xl p-4 text-xs text-amber-300 flex items-center gap-3">
          <span className="text-xl">⏳</span>
          <div>
            <strong className="block text-sm font-semibold text-amber-200">Offer Expired</strong>
            This offer exceeded its valid negotiation period.
          </div>
        </div>
      )}
    </div>
  );
};
