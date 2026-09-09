import React, { useState } from 'react';
import { useCounterOfferMutation } from '../api/marketplaceOffersApi';
import type { Offer } from '../../../types/offer';

interface CounterOfferModalProps {
  offer: Offer;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const CounterOfferModal: React.FC<CounterOfferModalProps> = ({
  offer,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const counterMutation = useCounterOfferMutation();

  const [counterQuantity, setCounterQuantity] = useState<number>(offer.offeredQuantity);
  const [quantityUnit, setQuantityUnit] = useState<string>(offer.quantityUnit || 'KG');
  const [counterPrice, setCounterPrice] = useState<number>(offer.offeredPrice);
  const [priceUnit, setPriceUnit] = useState<string>(offer.priceUnit || 'QUINTAL');
  const [message, setMessage] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (counterQuantity <= 0) {
      setErrorMessage('Counter quantity must be greater than zero.');
      return;
    }

    if (counterPrice <= 0) {
      setErrorMessage('Counter price must be greater than zero.');
      return;
    }

    try {
      await counterMutation.mutateAsync({
        offerId: offer.offerId,
        payload: {
          counterQuantity,
          quantityUnit,
          counterPrice,
          priceUnit,
          message: message.trim() || undefined,
        },
      });

      if (onSuccess) onSuccess();
      onClose();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } | string }; message?: string };
      const msg = errorObj.response?.data && typeof errorObj.response.data === 'object'
        ? errorObj.response.data.message
        : errorObj.response?.data || errorObj.message || 'Failed to submit counter-offer.';
      setErrorMessage(typeof msg === 'string' ? msg : 'Failed to submit counter-offer.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn">
      <div className="bg-emerald-950 border border-emerald-800/60 rounded-2xl max-w-lg w-full p-6 shadow-2xl text-slate-100 relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-emerald-400 hover:text-emerald-200 transition-colors p-1"
          aria-label="Close modal"
        >
          <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <h2 className="text-xl font-bold text-amber-300 mb-1 flex items-center gap-2">
          <span>🔄 Submit Counter-Offer</span>
        </h2>
        <p className="text-sm text-emerald-400/80 mb-4">
          Negotiation Round #{offer.roundNumber + 1} for <strong className="text-emerald-200">{offer.cropName}</strong>
        </p>

        {/* Current proposal reference */}
        <div className="bg-emerald-900/40 border border-emerald-700/50 rounded-xl p-3 mb-4 text-xs">
          <div className="text-amber-400 font-medium">Last Received Proposal</div>
          <div className="text-sm font-semibold text-emerald-200 mt-1">
            {offer.offeredQuantity} {offer.quantityUnit} @ ₹{offer.offeredPrice.toLocaleString('en-IN')} / {offer.priceUnit}
          </div>
          {offer.message && <p className="text-slate-300 italic mt-1 font-serif">"{offer.message}"</p>}
        </div>

        {errorMessage && (
          <div className="bg-rose-950/80 border border-rose-700 text-rose-300 text-xs p-3 rounded-lg mb-4">
            {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2">
              <label className="block text-xs font-medium text-emerald-300 mb-1">
                Counter Quantity <span className="text-rose-400">*</span>
              </label>
              <input
                type="number"
                step="any"
                min="0.1"
                value={counterQuantity}
                onChange={(e) => setCounterQuantity(parseFloat(e.target.value) || 0)}
                className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-3 py-2 text-sm text-emerald-100 focus:outline-none focus:ring-2 focus:ring-amber-500"
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-emerald-300 mb-1">Unit</label>
              <select
                value={quantityUnit}
                onChange={(e) => setQuantityUnit(e.target.value)}
                className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-2 py-2 text-sm text-emerald-100 focus:outline-none focus:ring-2 focus:ring-amber-500"
              >
                <option value="KG">KG</option>
                <option value="QUINTAL">QUINTAL</option>
                <option value="TON">TON</option>
                <option value="BAG">BAG</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2">
              <label className="block text-xs font-medium text-emerald-300 mb-1">
                Counter Price (₹) <span className="text-rose-400">*</span>
              </label>
              <input
                type="number"
                step="any"
                min="1"
                value={counterPrice}
                onChange={(e) => setCounterPrice(parseFloat(e.target.value) || 0)}
                className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-3 py-2 text-sm text-emerald-100 focus:outline-none focus:ring-2 focus:ring-amber-500 font-semibold"
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-emerald-300 mb-1">Price Unit</label>
              <select
                value={priceUnit}
                onChange={(e) => setPriceUnit(e.target.value)}
                className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-2 py-2 text-sm text-emerald-100 focus:outline-none focus:ring-2 focus:ring-amber-500"
              >
                <option value="QUINTAL">/ QUINTAL</option>
                <option value="KG">/ KG</option>
                <option value="TON">/ TON</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-emerald-300 mb-1">
              Counter Message <span className="text-slate-400 font-normal">(Optional)</span>
            </label>
            <textarea
              rows={3}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="e.g. Revised price proposal based on crop quality standard..."
              className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-3 py-2 text-xs text-emerald-100 placeholder-emerald-600 focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-slate-300 hover:text-white transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={counterMutation.isPending}
              className="px-5 py-2.5 bg-gradient-to-r from-amber-600 to-amber-700 hover:from-amber-500 hover:to-amber-600 text-white font-medium text-xs rounded-xl shadow-lg hover:shadow-amber-500/20 transition-all disabled:opacity-50"
            >
              {counterMutation.isPending ? 'Submitting...' : 'Submit Counter-Offer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
