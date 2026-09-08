import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Store,
  PlusCircle,
  Eye,
  Edit,
  PauseCircle,
  PlayCircle,
  Trash2,
  MapPin,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import {
  useMyListingsQuery,
  useUpdateListingStatusMutation,
  useDeleteListingMutation,
} from '../api/marketplaceApi';
import type { ProductListing } from '../../../types/marketplace';

export const MyListingsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);

  const { data: pageData, isLoading, isError, refetch } = useMyListingsQuery(
    statusFilter,
    page,
    10
  );

  const statusMutation = useUpdateListingStatusMutation();
  const deleteMutation = useDeleteListingMutation();

  const handleStatusToggle = (listingId: string, currentStatus: string) => {
    const nextStatus = currentStatus === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    statusMutation.mutate({ listingId, status: nextStatus });
  };

  const handleDelete = (listingId: string) => {
    if (window.confirm('Are you sure you want to delete this listing?')) {
      deleteMutation.mutate(listingId);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans antialiased select-none">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <div className="flex items-center space-x-3 mb-2">
              <span className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <Store className="w-6 h-6" />
              </span>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
                My Marketplace Listings
              </h1>
            </div>
            <p className="text-slate-400 text-sm">
              Manage your produce offerings, edit prices, pause availability, or create new listings.
            </p>
          </div>

          <Link
            to="/marketplace/listings/new"
            className="px-4 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm transition-all shadow-lg shadow-emerald-500/20 flex items-center space-x-2 self-start md:self-auto"
          >
            <PlusCircle className="w-4 h-4" />
            <span>+ Create New Listing</span>
          </Link>
        </div>

        {/* Status Filter Tabs */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-3 mb-6 scrollbar-none border-b border-slate-800">
          {['ALL', 'ACTIVE', 'PAUSED', 'DRAFT', 'SOLD_OUT', 'EXPIRED'].map((tab) => (
            <button
              key={tab}
              onClick={() => {
                setStatusFilter(tab);
                setPage(0);
              }}
              className={`px-4 py-2 rounded-xl text-xs font-semibold uppercase tracking-wider transition-all flex-shrink-0 ${
                statusFilter === tab
                  ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                  : 'bg-slate-900/60 text-slate-400 hover:bg-slate-800 hover:text-slate-200 border border-slate-800/80'
              }`}
            >
              {tab.replace('_', ' ')}
            </button>
          ))}
        </div>

        {/* Content */}
        {isLoading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-28 bg-slate-900/40 rounded-2xl animate-pulse border border-slate-800/60" />
            ))}
          </div>
        ) : isError ? (
          <div className="text-center py-16 bg-slate-900/30 rounded-2xl border border-red-500/20 p-6">
            <p className="text-red-400 text-sm font-semibold mb-3">Failed to load your listings.</p>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-xl"
            >
              Retry
            </button>
          </div>
        ) : pageData?.items.length === 0 ? (
          <div className="text-center py-16 bg-slate-900/30 rounded-2xl border border-slate-800 p-8 space-y-4">
            <Store className="w-12 h-12 text-slate-600 mx-auto" />
            <h3 className="text-lg font-bold text-slate-200">No listings found</h3>
            <p className="text-slate-400 text-xs sm:text-sm max-w-md mx-auto">
              You haven't created any produce listings matching status "{statusFilter}".
            </p>
            <Link
              to="/marketplace/listings/new"
              className="inline-flex items-center space-x-2 px-4 py-2 bg-emerald-500 text-slate-950 font-bold text-xs rounded-xl shadow-md"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Create Your First Listing</span>
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {pageData?.items.map((listing: ProductListing) => (
              <motion.div
                key={listing.listingId}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-slate-900/70 border border-slate-800 rounded-2xl p-5 shadow-md hover:border-slate-700 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                {/* Info block */}
                <div className="space-y-1.5 flex-1">
                  <div className="flex items-center space-x-3">
                    <span className="text-base font-bold text-white">
                      {listing.cropName}
                    </span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider ${
                        listing.status === 'ACTIVE'
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : listing.status === 'PAUSED'
                          ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                          : 'bg-slate-800 text-slate-400 border border-slate-700'
                      }`}
                    >
                      {listing.status}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-400">
                    <div>
                      Asking Price:{' '}
                      <strong className="text-emerald-400 font-semibold">
                        ₹{listing.askingPrice?.toLocaleString('en-IN')} / {listing.priceUnit || listing.unit}
                      </strong>
                    </div>
                    <div>
                      Available:{' '}
                      <strong className="text-slate-200 font-semibold">
                        {listing.availableQuantity} / {listing.quantity} {listing.unit}
                      </strong>
                    </div>
                    {listing.location && (
                      <div className="flex items-center space-x-1">
                        <MapPin className="w-3 h-3 text-slate-500" />
                        <span>{[listing.location.district, listing.location.state].filter(Boolean).join(', ')}</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Actions Block */}
                <div className="flex items-center space-x-2 pt-3 md:pt-0 border-t md:border-t-0 border-slate-800">
                  <Link
                    to={`/marketplace/listings/${listing.listingId}`}
                    className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-all text-xs font-semibold flex items-center space-x-1"
                    title="View Details"
                  >
                    <Eye className="w-4 h-4" />
                    <span className="hidden sm:inline">View</span>
                  </Link>

                  <Link
                    to={`/marketplace/listings/${listing.listingId}/edit`}
                    className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-all text-xs font-semibold flex items-center space-x-1"
                    title="Edit Listing"
                  >
                    <Edit className="w-4 h-4" />
                    <span className="hidden sm:inline">Edit</span>
                  </Link>

                  {listing.status === 'ACTIVE' ? (
                    <button
                      onClick={() => handleStatusToggle(listing.listingId, listing.status)}
                      className="p-2 rounded-xl bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 transition-all text-xs font-semibold flex items-center space-x-1"
                      title="Pause Listing"
                    >
                      <PauseCircle className="w-4 h-4" />
                      <span className="hidden sm:inline">Pause</span>
                    </button>
                  ) : listing.status === 'PAUSED' ? (
                    <button
                      onClick={() => handleStatusToggle(listing.listingId, listing.status)}
                      className="p-2 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 transition-all text-xs font-semibold flex items-center space-x-1"
                      title="Activate Listing"
                    >
                      <PlayCircle className="w-4 h-4" />
                      <span className="hidden sm:inline">Activate</span>
                    </button>
                  ) : null}

                  {listing.status === 'DRAFT' && (
                    <button
                      onClick={() => handleDelete(listing.listingId)}
                      className="p-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 transition-all text-xs font-semibold flex items-center space-x-1"
                      title="Delete Draft"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {pageData && pageData.totalPages > 1 && (
          <div className="mt-8 flex items-center justify-between border-t border-slate-800 pt-6">
            <div className="text-xs text-slate-400">
              Page {pageData.page + 1} of {pageData.totalPages}
            </div>

            <div className="flex items-center space-x-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:bg-slate-800 disabled:opacity-40 text-xs flex items-center space-x-1"
              >
                <ChevronLeft className="w-4 h-4" />
                <span>Previous</span>
              </button>

              <button
                disabled={!pageData.hasNext}
                onClick={() => setPage((p) => p + 1)}
                className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:bg-slate-800 disabled:opacity-40 text-xs flex items-center space-x-1"
              >
                <span>Next</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
};
