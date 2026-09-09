import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowLeft,
  ShoppingBag,
  MapPin,
  Calendar,
  Layers,
  TrendingUp,
  ShieldCheck,
  Tag,
  Edit,
  PauseCircle,
  PlayCircle,
  Info,
  AlertCircle,
} from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { useAuth } from '../../auth/hooks/useAuth';
import {
  useListingDetailQuery,
  useUpdateListingStatusMutation,
} from '../api/marketplaceApi';
import { MakeOfferModal } from '../components/MakeOfferModal';

export const MarketplaceDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { userDocument } = useAuth();
  const [isMakeOfferOpen, setIsMakeOfferOpen] = React.useState<boolean>(false);

  const { data: listing, isLoading, isError } = useListingDetailQuery(id || '');
  const statusMutation = useUpdateListingStatusMutation();

  const isOwner = userDocument?.uid && listing?.ownerUid === userDocument.uid;

  const handleStatusChange = (newStatus: string) => {
    if (!id) return;
    statusMutation.mutate({ listingId: id, status: newStatus });
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans">
        <Navbar />
        <main className="flex-1 max-w-4xl w-full mx-auto px-4 py-12 flex justify-center items-center">
          <div className="flex flex-col items-center space-y-4">
            <div className="w-10 h-10 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin" />
            <p className="text-slate-400 text-sm">Loading produce listing details...</p>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  if (isError || !listing) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans">
        <Navbar />
        <main className="flex-1 max-w-4xl w-full mx-auto px-4 py-12 text-center space-y-4">
          <AlertCircle className="w-12 h-12 text-red-400 mx-auto" />
          <h2 className="text-xl font-bold text-slate-200">Listing Not Found</h2>
          <p className="text-slate-400 text-sm">
            The requested produce listing may have been removed or is no longer available.
          </p>
          <Link
            to="/marketplace"
            className="inline-flex items-center space-x-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Marketplace</span>
          </Link>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans antialiased select-none">
      <Navbar />

      <main className="flex-1 max-w-4xl w-full mx-auto px-4 sm:px-6 py-8">
        {/* Back Link */}
        <div className="mb-6">
          <Link
            to="/marketplace"
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Marketplace</span>
          </Link>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          {/* Main Hero Card */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 backdrop-blur-md shadow-2xl space-y-6">
            {/* Header badges */}
            <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-slate-800/80">
              <div className="flex items-center space-x-3">
                <span className="p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                  <ShoppingBag className="w-7 h-7" />
                </span>
                <div>
                  <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
                    {listing.cropName}
                  </h1>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Farmer Produce Listing • ID: {listing.listingId}
                  </p>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <span
                  className={`px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${
                    listing.status === 'ACTIVE'
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                      : listing.status === 'PAUSED'
                      ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                      : 'bg-slate-800 text-slate-400 border border-slate-700'
                  }`}
                >
                  {listing.status}
                </span>

                {isOwner && (
                  <Link
                    to={`/marketplace/listings/${listing.listingId}/edit`}
                    className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-all flex items-center space-x-1 text-xs font-semibold"
                  >
                    <Edit className="w-3.5 h-3.5" />
                    <span>Edit</span>
                  </Link>
                )}
              </div>
            </div>

            {/* Price Showcase Box */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Seller Asking Price */}
              <div className="bg-slate-950 border border-emerald-500/30 rounded-2xl p-5 shadow-lg relative overflow-hidden">
                <div className="absolute -top-10 -right-10 w-32 h-32 bg-emerald-500/5 rounded-full blur-2xl pointer-events-none" />
                <div className="text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-1 flex items-center space-x-1.5">
                  <Tag className="w-3.5 h-3.5" />
                  <span>Seller Asking Price</span>
                </div>
                <div className="text-3xl font-extrabold text-white my-1">
                  ₹{listing.askingPrice?.toLocaleString('en-IN')}{' '}
                  <span className="text-sm font-normal text-slate-400">
                    / {listing.priceUnit || listing.unit}
                  </span>
                </div>
                <p className="text-[11px] text-slate-400">
                  Seller-entered asking price set directly by the farmer.
                </p>
              </div>

              {/* Reference Market Price (if available) */}
              {listing.referenceMarketPrice?.verifiedModalPrice ? (
                <div className="bg-slate-950 border border-slate-800 rounded-2xl p-5 shadow-lg">
                  <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-1 flex items-center space-x-1.5">
                    <TrendingUp className="w-3.5 h-3.5" />
                    <span>Verified Market Reference</span>
                  </div>
                  <div className="text-3xl font-extrabold text-slate-200 my-1">
                    ₹{listing.referenceMarketPrice.verifiedModalPrice?.toLocaleString('en-IN')}{' '}
                    <span className="text-sm font-normal text-slate-400">
                      / {listing.referenceMarketPrice.priceUnit}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400">
                    {listing.referenceMarketPrice.marketName
                      ? `Market: ${listing.referenceMarketPrice.marketName}`
                      : 'Verified market intelligence reference.'}
                  </p>
                </div>
              ) : (
                <div className="bg-slate-950 border border-slate-800/80 rounded-2xl p-5 flex items-center space-x-3 text-slate-400 text-xs">
                  <Info className="w-5 h-5 text-slate-500 flex-shrink-0" />
                  <span>
                    Verified market price reference is provided for informational comparison. Asking price is farmer-specified.
                  </span>
                </div>
              )}
            </div>

            {/* Produce Specs Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
              <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-4">
                <div className="text-slate-400 text-xs font-medium mb-1 flex items-center space-x-1.5">
                  <Layers className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Available Quantity</span>
                </div>
                <div className="text-lg font-bold text-white">
                  {listing.availableQuantity?.toLocaleString('en-IN')} {listing.unit}
                </div>
                <div className="text-[11px] text-slate-500 mt-0.5">
                  Total listed: {listing.quantity?.toLocaleString('en-IN')} {listing.unit}
                </div>
              </div>

              <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-4">
                <div className="text-slate-400 text-xs font-medium mb-1 flex items-center space-x-1.5">
                  <MapPin className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Region / Location</span>
                </div>
                <div className="text-sm font-bold text-white truncate">
                  {[listing.location?.district, listing.location?.state].filter(Boolean).join(', ') || 'Regional Produce'}
                </div>
                <div className="text-[11px] text-slate-500 mt-0.5">
                  {[listing.location?.village, listing.location?.mandal].filter(Boolean).join(', ')}
                </div>
              </div>

              <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-4">
                <div className="text-slate-400 text-xs font-medium mb-1 flex items-center space-x-1.5">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Quality Grade</span>
                </div>
                <div className="text-sm font-bold text-white">
                  {listing.qualityGrade ? listing.qualityGrade.replace('_', ' ') : 'UNSPECIFIED'}
                </div>
                <div className="text-[11px] text-slate-500 mt-0.5">
                  Seller-declared grade
                </div>
              </div>
            </div>

            {/* Dates & Availability info */}
            {(listing.harvestDate || listing.availableFrom) && (
              <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-4 flex flex-wrap items-center justify-between gap-4 text-xs text-slate-300">
                {listing.harvestDate && (
                  <div className="flex items-center space-x-2">
                    <Calendar className="w-4 h-4 text-slate-400" />
                    <span>Harvest Date: <strong className="text-slate-100">{listing.harvestDate}</strong></span>
                  </div>
                )}
                {listing.availableFrom && (
                  <div className="flex items-center space-x-2">
                    <Calendar className="w-4 h-4 text-slate-400" />
                    <span>Available From: <strong className="text-slate-100">{listing.availableFrom}</strong></span>
                  </div>
                )}
              </div>
            )}

            {/* Description */}
            {listing.description && (
              <div className="space-y-2">
                <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                  Product Description
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed bg-slate-950/60 border border-slate-800/80 rounded-xl p-4">
                  {listing.description}
                </p>
              </div>
            )}

            {/* Offer Action Section */}
            {!isOwner && listing.status === 'ACTIVE' && (userDocument?.role === 'MEDIATOR_BUYER' || userDocument?.role === 'CUSTOMER' || !userDocument?.role || userDocument?.role === 'USER') && (
              <div className="pt-4 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-4">
                <div>
                  <h3 className="text-sm font-bold text-emerald-300">Commercial Offer & Negotiation</h3>
                  <p className="text-xs text-slate-400">Propose your price and quantity directly to the farmer.</p>
                </div>
                <button
                  onClick={() => setIsMakeOfferOpen(true)}
                  className="px-6 py-3 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-xs rounded-xl shadow-lg hover:shadow-emerald-500/20 transition-all flex items-center space-x-2"
                >
                  <span>🌾 Make Offer</span>
                </button>
              </div>
            )}

            {isOwner && (
              <div className="pt-4 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-3">
                <div className="text-xs text-slate-400">
                  You are the owner of this listing.
                </div>

                <div className="flex items-center space-x-3">
                  <Link
                    to="/marketplace/received-offers"
                    className="px-4 py-2 rounded-xl bg-emerald-950 hover:bg-emerald-900 text-emerald-300 border border-emerald-700/60 text-xs font-semibold flex items-center space-x-1.5 transition-all"
                  >
                    <span>📥 View Offers</span>
                  </Link>

                  {listing.status === 'ACTIVE' ? (
                    <button
                      onClick={() => handleStatusChange('PAUSED')}
                      disabled={statusMutation.isPending}
                      className="px-4 py-2 rounded-xl bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 text-xs font-semibold flex items-center space-x-1.5 transition-all"
                    >
                      <PauseCircle className="w-4 h-4" />
                      <span>Pause Listing</span>
                    </button>
                  ) : listing.status === 'PAUSED' ? (
                    <button
                      onClick={() => handleStatusChange('ACTIVE')}
                      disabled={statusMutation.isPending}
                      className="px-4 py-2 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-semibold flex items-center space-x-1.5 transition-all"
                    >
                      <PlayCircle className="w-4 h-4" />
                      <span>Reactivate Listing</span>
                    </button>
                  ) : null}

                  <Link
                    to={`/marketplace/listings/${listing.listingId}/edit`}
                    className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition-all"
                  >
                    Edit Details
                  </Link>
                </div>
              </div>
            )}
          </div>
        </motion.div>
      </main>

      {listing && isMakeOfferOpen && (
        <MakeOfferModal
          listing={listing}
          isOpen={isMakeOfferOpen}
          onClose={() => setIsMakeOfferOpen(false)}
        />
      )}

      <Footer />
    </div>
  );
};
