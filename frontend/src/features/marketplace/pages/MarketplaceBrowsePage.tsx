import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ShoppingBag,
  Filter,
  PlusCircle,
  MapPin,
  Calendar,
  Layers,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  Store,
} from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { useAuth } from '../../auth/hooks/useAuth';
import { useCrops } from '../../prices/hooks/useCrops';
import { useLocationCascade } from '../../markets/hooks/useLocationCascade';
import { useMarketplaceBrowseQuery } from '../api/marketplaceApi';
import type { ListingFilterParams, ProductListing } from '../../../types/marketplace';

export const MarketplaceBrowsePage: React.FC = () => {
  const { userDocument } = useAuth();
  const isFarmer = userDocument?.role === 'FARMER';

  const [filterState, setFilterState] = useState<ListingFilterParams>({
    page: 0,
    size: 12,
    sortBy: 'newest',
  });

  const { data: cropList = [] } = useCrops();
  const { states = [], districts = [] } = useLocationCascade(filterState.state, filterState.district);

  const { data: listingPage, isLoading, isError, refetch } = useMarketplaceBrowseQuery(filterState);

  const handleFilterChange = (key: keyof ListingFilterParams, value: string | number | undefined) => {
    setFilterState((prev) => ({
      ...prev,
      [key]: value === '' ? undefined : value,
      page: 0,
    }));
  };


  const handlePageChange = (newPage: number) => {
    setFilterState((prev) => ({ ...prev, page: newPage }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans antialiased select-none">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <div className="flex items-center space-x-3 mb-2">
              <span className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <ShoppingBag className="w-6 h-6" />
              </span>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
                Produce Marketplace
              </h1>
            </div>
            <p className="text-slate-400 text-sm sm:text-base">
              Browse available produce listings directly from verified farmers across regions.
            </p>
          </div>

          {isFarmer && (
            <div className="flex items-center space-x-3">
              <Link
                to="/marketplace/my-listings"
                className="px-4 py-2.5 rounded-xl border border-slate-800 bg-slate-900/80 hover:bg-slate-800 text-slate-200 text-sm font-semibold transition-all shadow-sm flex items-center space-x-2"
              >
                <Store className="w-4 h-4 text-emerald-400" />
                <span>My Listings</span>
              </Link>
              <Link
                to="/marketplace/listings/new"
                className="px-4 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm transition-all shadow-lg shadow-emerald-500/20 flex items-center space-x-2"
              >
                <PlusCircle className="w-4 h-4" />
                <span>+ Create Listing</span>
              </Link>
            </div>
          )}
        </div>

        {/* Filter Bar Card */}
        <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl p-4 sm:p-6 mb-8 backdrop-blur-md shadow-xl space-y-4">
          <div className="flex items-center space-x-2 text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-2">
            <Filter className="w-3.5 h-3.5" />
            <span>Search & Filter Listings</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            {/* Crop Select */}
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1.5">Crop / Commodity</label>
              <select
                value={filterState.cropId || ''}
                onChange={(e) => handleFilterChange('cropId', e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500/50"
              >
                <option value="">All Crops</option>
                {cropList.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            {/* State Select */}
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1.5">State</label>
              <select
                value={filterState.state || ''}
                onChange={(e) => {
                  handleFilterChange('state', e.target.value);
                  handleFilterChange('district', undefined);
                }}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500/50"
              >
                <option value="">All States</option>
                {states.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>

            {/* District Select */}
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1.5">District</label>
              <select
                value={filterState.district || ''}
                onChange={(e) => handleFilterChange('district', e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500/50"
              >
                <option value="">All Districts</option>
                {districts.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </div>

            {/* Sort Selector */}
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1.5">Sort By</label>
              <select
                value={filterState.sortBy || 'newest'}
                onChange={(e) => handleFilterChange('sortBy', e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500/50"
              >
                <option value="newest">Newest First</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="quantity_desc">Available Quantity</option>
              </select>
            </div>

            {/* Reset / Actions */}
            <div className="flex items-end space-x-2">
              <button
                onClick={() =>
                  setFilterState({ page: 0, size: 12, sortBy: 'newest' })
                }
                className="w-full px-4 py-2 bg-slate-800/80 hover:bg-slate-800 text-slate-300 text-xs font-semibold rounded-xl transition-all"
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {/* Listings Grid */}
        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="h-64 bg-slate-900/40 rounded-2xl animate-pulse border border-slate-800/60" />
            ))}
          </div>
        ) : isError ? (
          <div className="text-center py-16 bg-slate-900/30 rounded-2xl border border-red-500/20 p-6">
            <p className="text-red-400 text-sm font-semibold mb-3">Failed to load marketplace listings.</p>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-xl"
            >
              Retry
            </button>
          </div>
        ) : listingPage?.items.length === 0 ? (
          <div className="text-center py-16 bg-slate-900/30 rounded-2xl border border-slate-800 p-8 space-y-4">
            <ShoppingBag className="w-12 h-12 text-slate-600 mx-auto" />
            <h3 className="text-lg font-bold text-slate-200">No produce listings found</h3>
            <p className="text-slate-400 text-xs sm:text-sm max-w-md mx-auto">
              There are currently no active produce listings matching your criteria. Try adjusting your filters or check back soon.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {listingPage?.items.map((listing: ProductListing) => (
              <motion.div
                key={listing.listingId}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-slate-900/70 border border-slate-800 hover:border-emerald-500/40 rounded-2xl p-5 shadow-lg flex flex-col justify-between transition-all hover:shadow-emerald-500/5 group"
              >
                <div>
                  {/* Top Badge Row */}
                  <div className="flex items-center justify-between mb-3">
                    <span className="px-2.5 py-1 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-bold uppercase tracking-wider">
                      {listing.cropName}
                    </span>

                    {listing.qualityGrade && listing.qualityGrade !== 'UNSPECIFIED' && (
                      <span className="px-2 py-0.5 rounded-md bg-slate-800 text-slate-300 text-[11px] font-medium">
                        {listing.qualityGrade.replace('_', ' ')}
                      </span>
                    )}
                  </div>

                  {/* Pricing Box */}
                  <div className="bg-slate-950/80 rounded-xl p-3.5 border border-slate-800/80 mb-4">
                    <div className="text-[11px] font-medium text-slate-400 uppercase tracking-wider mb-0.5">
                      Seller Asking Price
                    </div>
                    <div className="text-xl font-extrabold text-emerald-400">
                      ₹{listing.askingPrice?.toLocaleString('en-IN')}{' '}
                      <span className="text-xs font-normal text-slate-400">/ {listing.priceUnit || listing.unit}</span>
                    </div>

                    {/* Reference Market Price badge if available */}
                    {listing.referenceMarketPrice?.verifiedModalPrice && (
                      <div className="mt-2.5 pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400">
                        <span className="flex items-center space-x-1">
                          <TrendingUp className="w-3 h-3 text-cyan-400" />
                          <span>Market Reference:</span>
                        </span>
                        <span className="font-semibold text-slate-300">
                          ₹{listing.referenceMarketPrice.verifiedModalPrice?.toLocaleString('en-IN')} / {listing.referenceMarketPrice.priceUnit}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Details List */}
                  <div className="space-y-2 text-xs text-slate-300 mb-4">
                    <div className="flex items-center space-x-2">
                      <Layers className="w-3.5 h-3.5 text-slate-400" />
                      <span>
                        Available:{' '}
                        <strong className="text-slate-100 font-semibold">
                          {listing.availableQuantity?.toLocaleString('en-IN')} {listing.unit}
                        </strong>
                      </span>
                    </div>

                    {listing.location && (
                      <div className="flex items-center space-x-2">
                        <MapPin className="w-3.5 h-3.5 text-slate-400" />
                        <span className="truncate">
                          {[listing.location.district, listing.location.state].filter(Boolean).join(', ') || 'Region Specified'}
                        </span>
                      </div>
                    )}

                    {listing.availableFrom && (
                      <div className="flex items-center space-x-2">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        <span>Available From: {listing.availableFrom}</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Footer Action */}
                <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between">
                  <span className="text-[11px] text-slate-500">
                    Direct Farmer Produce
                  </span>
                  <Link
                    to={`/marketplace/listings/${listing.listingId}`}
                    className="px-3.5 py-1.5 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 text-xs font-semibold border border-emerald-500/30 transition-all"
                  >
                    View Details
                  </Link>
                </div>
              </motion.div>
            ))}
          </div>
        )}

        {/* Pagination Controls */}
        {listingPage && listingPage.totalPages > 1 && (
          <div className="mt-10 flex items-center justify-between border-t border-slate-800 pt-6">
            <div className="text-xs text-slate-400">
              Showing page <strong className="text-slate-200">{listingPage.page + 1}</strong> of{' '}
              <strong className="text-slate-200">{listingPage.totalPages}</strong> ({listingPage.totalElements} total listings)
            </div>

            <div className="flex items-center space-x-2">
              <button
                disabled={listingPage.page === 0}
                onClick={() => handlePageChange(listingPage.page - 1)}
                className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed text-xs flex items-center space-x-1"
              >
                <ChevronLeft className="w-4 h-4" />
                <span>Previous</span>
              </button>

              <button
                disabled={!listingPage.hasNext}
                onClick={() => handlePageChange(listingPage.page + 1)}
                className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed text-xs flex items-center space-x-1"
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
