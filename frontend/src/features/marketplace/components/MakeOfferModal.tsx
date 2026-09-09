import React, { useState } from 'react';
import { useCreateOfferMutation } from '../api/marketplaceOffersApi';
import type { ProductListing } from '../../../types/marketplace';

interface MakeOfferModalProps {
  listing: ProductListing;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const MakeOfferModal: React.FC<MakeOfferModalProps> = ({
  listing,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const createOfferMutation = useCreateOfferMutation();

  const [offeredQuantity, setOfferedQuantity] = useState<number>(listing.availableQuantity || listing.quantity);
  const [quantityUnit, setQuantityUnit] = useState<string>(listing.unit || 'KG');
  const [offeredPrice, setOfferedPrice] = useState<number>(listing.askingPrice);
  const [priceUnit, setPriceUnit] = useState<string>(listing.priceUnit || 'QUINTAL');
  const [message, setMessage] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (offeredQuantity <= 0) {
      setErrorMessage('Offered quantity must be greater than zero.');
      return;
    }

    if (listing.availableQuantity && offeredQuantity > listing.availableQuantity) {
      setErrorMessage(`Offered quantity cannot exceed available quantity (${listing.availableQuantity} ${listing.unit}).`);
      return;
    }

    if (offeredPrice <= 0) {
      setErrorMessage('Offered price must be greater than zero.');
      return;
    }

    try {
      await createOfferMutation.mutateAsync({
        listingId: listing.listingId,
        offeredQuantity,
        quantityUnit,
        offeredPrice,
        priceUnit,
        message: message.trim() || undefined,
      });

      if (onSuccess) onSuccess();
      onClose();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } | string }; message?: string };
      const msg = errorObj.response?.data && typeof errorObj.response.data === 'object'
        ? errorObj.response.data.message
        : errorObj.response?.data || errorObj.message || 'Failed to submit offer.';
      setErrorMessage(typeof msg === 'string' ? msg : 'Failed to submit offer.');
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

        <h2 className="text-xl font-bold text-emerald-300 mb-1 flex items-center gap-2">
          <span>🌾 Make Commercial Offer</span>
        </h2>
        <p className="text-sm text-emerald-400/80 mb-4">
          Listing: <strong className="text-emerald-200">{listing.cropName}</strong> ({listing.availableQuantity} {listing.unit} available)
        </p>

        {/* Asking price reference card */}
        <div className="bg-emerald-900/40 border border-emerald-700/50 rounded-xl p-3 mb-4 text-xs">
          <div className="text-emerald-400 font-medium">Farmer Asking Price Reference</div>
          <div className="text-lg font-bold text-emerald-200">
            ₹{listing.askingPrice.toLocaleString('en-IN')} <span className="text-xs text-emerald-400 font-normal">/ {listing.priceUnit}</span>
          </div>
          <p className="text-slate-400 mt-1">Your offer will be sent directly to the farmer for review.</p>
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
                Offer Quantity <span className="text-rose-400">*</span>
              </label>
              <input
                type="number"
                step="any"
                min="0.1"
                max={listing.availableQuantity}
                value={offeredQuantity}
                onChange={(e) => setOfferedQuantity(parseFloat(e.target.value) || 0)}
                className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-3 py-2 text-sm text-emerald-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-emerald-300 mb-1">Unit</label>
              <select
                value={quantityUnit}
                onChange={(e) => setQuantityUnit(e.target.value)}
                className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-2 py-2 text-sm text-emerald-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
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
                Your Offer Price (₹) <span className="text-rose-400">*</span>
              </label>
              <input
                type="number"
                step="any"
                min="1"
                value={offeredPrice}
                onChange={(e) => setOfferedPrice(parseFloat(e.target.value) || 0)}
                className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-3 py-2 text-sm text-emerald-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-semibold"
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-emerald-300 mb-1">Price Unit</label>
              <select
                value={priceUnit}
                onChange={(e) => setPriceUnit(e.target.value)}
                className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-2 py-2 text-sm text-emerald-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              >
                <option value="QUINTAL">/ QUINTAL</option>
                <option value="KG">/ KG</option>
                <option value="TON">/ TON</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-emerald-300 mb-1">
              Message to Farmer <span className="text-slate-400 font-normal">(Optional)</span>
            </label>
            <textarea
              rows={3}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="e.g. Can pick up from farm tomorrow morning with immediate payment arrangement..."
              className="w-full bg-emerald-900/60 border border-emerald-700/60 rounded-xl px-3 py-2 text-xs text-emerald-100 placeholder-emerald-600 focus:outline-none focus:ring-2 focus:ring-emerald-500"
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
              disabled={createOfferMutation.isPending}
              className="px-5 py-2.5 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-medium text-xs rounded-xl shadow-lg hover:shadow-emerald-500/20 transition-all disabled:opacity-50"
            >
              {createOfferMutation.isPending ? 'Submitting...' : 'Submit Commercial Offer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
