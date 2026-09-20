import React, { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useBuyerOpportunitiesQuery } from '../api/marketplaceBuyerOpportunitiesApi';
import { useMyListingsQuery } from '../api/marketplaceApi';
import type { ProductListing } from '../../../types/marketplace';
import type { MatchReasonCode, ConfidenceLevel } from '../../../types/buyerOpportunity';

export const BuyerOpportunitiesPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialListingId = searchParams.get('listingId') || '';
  const [selectedListingId, setSelectedListingId] = useState<string>(initialListingId);
  const [page, setPage] = useState<number>(0);

  const { data: listingsData, isLoading: isLoadingListings } = useMyListingsQuery();
  const { data: opportunitiesData, isLoading: isLoadingOpportunities, isError, refetch } = useBuyerOpportunitiesQuery(
    selectedListingId,
    page,
    10
  );

  const activeListings = listingsData?.items.filter((l: ProductListing) => l.status === 'ACTIVE') || [];

  const renderReasonBadge = (reason: MatchReasonCode) => {
    switch (reason) {
      case 'CROP_MATCH':
        return <span key={reason} className="px-2.5 py-1 bg-emerald-950/80 border border-emerald-700/60 text-emerald-300 text-xs font-medium rounded-lg flex items-center gap-1">🌾 Crop Match</span>;
      case 'QUANTITY_MATCH':
        return <span key={reason} className="px-2.5 py-1 bg-sky-950/80 border border-sky-700/60 text-sky-300 text-xs font-medium rounded-lg flex items-center gap-1">📦 Quantity Aligned</span>;
      case 'PARTIAL_QUANTITY':
        return <span key={reason} className="px-2.5 py-1 bg-amber-950/80 border border-amber-700/60 text-amber-300 text-xs font-medium rounded-lg flex items-center gap-1">🧩 Partial Quantity</span>;
      case 'QUALITY_MATCH':
        return <span key={reason} className="px-2.5 py-1 bg-purple-950/80 border border-purple-700/60 text-purple-300 text-xs font-medium rounded-lg flex items-center gap-1">⭐ Quality Grade Match</span>;
      case 'SAME_MARKET':
        return <span key={reason} className="px-2.5 py-1 bg-indigo-950/80 border border-indigo-700/60 text-indigo-300 text-xs font-medium rounded-lg flex items-center gap-1">📍 Same Local Market</span>;
      case 'SAME_DISTRICT':
        return <span key={reason} className="px-2.5 py-1 bg-blue-950/80 border border-blue-700/60 text-blue-300 text-xs font-medium rounded-lg flex items-center gap-1">📍 Same District</span>;
      case 'SAME_STATE':
        return <span key={reason} className="px-2.5 py-1 bg-slate-800 border border-slate-700 text-slate-300 text-xs font-medium rounded-lg flex items-center gap-1">🏛️ Same State</span>;
      default:
        return <span key={reason} className="px-2.5 py-1 bg-slate-900 text-slate-400 text-xs font-medium rounded-lg">{reason}</span>;
    }
  };

  const renderConfidenceBadge = (confidence: ConfidenceLevel) => {
    switch (confidence) {
      case 'HIGH':
        return <span className="px-2.5 py-1 bg-emerald-950 border border-emerald-600 text-emerald-300 text-xs font-bold rounded-full">CONFIDENCE: HIGH</span>;
      case 'MEDIUM':
        return <span className="px-2.5 py-1 bg-sky-950 border border-sky-600 text-sky-300 text-xs font-bold rounded-full">CONFIDENCE: MEDIUM</span>;
      case 'LOW':
        return <span className="px-2.5 py-1 bg-amber-950 border border-amber-600 text-amber-300 text-xs font-bold rounded-full">CONFIDENCE: LOW</span>;
      case 'INSUFFICIENT_DATA':
        return <span className="px-2.5 py-1 bg-slate-900 border border-slate-700 text-slate-400 text-xs font-bold rounded-full">INSUFFICIENT DATA</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
          <div>
            <h1 className="text-2xl font-bold text-emerald-400 flex items-center gap-2">
              <span>🎯 Buyer Opportunity Matching</span>
            </h1>
            <p className="text-xs text-slate-400 mt-1">
              Decision support foundation matching active farmer produce listings against verified marketplace buyer demand.
            </p>
          </div>

          <Link
            to="/marketplace/my-listings"
            className="self-start md:self-auto px-4 py-2 bg-emerald-900/60 hover:bg-emerald-800/80 border border-emerald-700/50 text-emerald-200 text-xs font-medium rounded-xl transition-colors"
          >
            ← My Produce Listings
          </Link>
        </div>

        {/* Legal & Methodology Notice */}
        <div className="bg-slate-900/80 border border-emerald-900/50 p-4 rounded-2xl text-xs text-slate-300 space-y-1">
          <span className="font-semibold text-emerald-400 block">ℹ️ Methodology & Data Privacy Notice</span>
          <p className="text-slate-400">
            Relevance scores represent deterministic multi-dimensional compatibility (Crop, Quantity, Location, Quality Grade) based strictly on verified application demand data. Scores do <strong>NOT</strong> represent purchase probability or intent prediction. Private buyer personal information is strictly protected.
          </p>
        </div>

        {/* Listing Selector */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 space-y-3">
          <label htmlFor="listing-select" className="text-xs font-semibold text-slate-300 block">Select Active Produce Listing to Analyze</label>
          {isLoadingListings ? (
            <div className="text-xs text-slate-400">Loading active produce listings...</div>
          ) : activeListings.length === 0 ? (
            <div className="text-xs text-amber-300 bg-amber-950/40 p-3 border border-amber-800/60 rounded-xl">
              You currently have no ACTIVE produce listings. Buyer opportunity analysis requires an active produce listing.
            </div>
          ) : (
            <select
              id="listing-select"
              value={selectedListingId}
              onChange={(e) => {
                setSelectedListingId(e.target.value);
                setSearchParams({ listingId: e.target.value });
                setPage(0);
              }}
              className="w-full bg-slate-950 border border-slate-700 text-slate-200 text-xs rounded-xl p-3 focus:border-emerald-500 focus:outline-none"
            >
              <option value="" disabled>-- Choose an active produce listing --</option>
              {activeListings.map((l: ProductListing) => (
                <option key={l.listingId} value={l.listingId}>
                  🌾 {l.cropName} — {l.availableQuantity} {l.unit} @ ₹{l.askingPrice}/{l.priceUnit} ({l.location?.district || 'Market'})
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Opportunities List Section */}
        {!selectedListingId ? (
          <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-12 text-center text-xs text-slate-400">
            Please select an active produce listing above to view matching buyer demand opportunities.
          </div>
        ) : isLoadingOpportunities ? (
          <div className="text-center py-16 text-emerald-400 text-xs font-medium">
            Computing buyer demand compatibility...
          </div>
        ) : isError ? (
          <div className="bg-rose-950/60 border border-rose-800 text-rose-200 p-6 rounded-2xl text-center space-y-3">
            <p className="text-xs">Failed to retrieve buyer opportunities. Please verify listing ownership or try again.</p>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-rose-900 text-white text-xs font-medium rounded-xl hover:bg-rose-800 transition-colors"
            >
              Retry
            </button>
          </div>
        ) : !opportunitiesData?.items || opportunitiesData.items.length === 0 ? (
          <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl p-12 text-center space-y-3">
            <span className="text-4xl block">🔍</span>
            <h3 className="text-sm font-semibold text-slate-300">
              {opportunitiesData?.statusState === 'INSUFFICIENT_DATA' ? 'Insufficient Activity Data' : 'No Buyer Matches Found'}
            </h3>
            <p className="text-xs text-slate-400 max-w-md mx-auto">
              {opportunitiesData?.statusState === 'INSUFFICIENT_DATA'
                ? 'Verified buyer demand records are sparse for this crop region. As buyers interact on the marketplace, opportunities will appear automatically.'
                : 'No active buyer demand records currently match this listing parameters. Try adjusting listing quantity or location coverage.'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex items-center justify-between text-xs text-slate-400 px-1">
              <span>Found <strong>{opportunitiesData.totalElements}</strong> matching buyer demand profile(s)</span>
              <span>Deterministic Sorting: Relevance Score (Highest First)</span>
            </div>

            {opportunitiesData.items.map((opp) => (
              <div
                key={opp.opportunityId}
                className="bg-slate-900/80 border border-slate-800 hover:border-emerald-700/50 rounded-2xl p-5 transition-all space-y-4 shadow-lg"
              >
                <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-800/60 pb-3">
                  <div>
                    <span className="text-[10px] font-mono text-slate-500 block">OPPORTUNITY ID: {opp.opportunityId}</span>
                    <h2 className="text-base font-bold text-slate-100 flex items-center gap-2 mt-0.5">
                      <span>👤 {opp.buyerRoleDisplay}</span>
                      <span className="text-xs font-normal text-slate-400">({opp.anonymizedLocation})</span>
                    </h2>
                  </div>

                  <div className="flex items-center gap-3">
                    {renderConfidenceBadge(opp.confidenceLevel)}
                    <div className="bg-emerald-950/80 border border-emerald-600/80 px-3 py-1.5 rounded-xl text-right">
                      <span className="text-[10px] text-emerald-400 block font-semibold uppercase">Relevance Score</span>
                      <span className="text-base font-extrabold text-emerald-300">{opp.relevanceScore.toFixed(1)}%</span>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-xs">
                  <div>
                    <span className="text-slate-400 block text-[11px]">Demanded Crop</span>
                    <span className="font-semibold text-slate-200">🌾 {opp.cropName}</span>
                  </div>

                  <div>
                    <span className="text-slate-400 block text-[11px]">Typical Requested Quantity</span>
                    <span className="font-semibold text-slate-200">
                      {opp.typicalQuantity ? `${opp.typicalQuantity} ${opp.quantityUnit || ''}` : 'Standard Volume'}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-400 block text-[11px]">Anonymized Buyer Region</span>
                    <span className="text-slate-300">📍 {opp.anonymizedLocation}</span>
                  </div>
                </div>

                <div className="space-y-1.5 pt-1 border-t border-slate-800/40">
                  <span className="text-[11px] font-semibold text-slate-400 block">Structured Compatibility Reasons:</span>
                  <div className="flex flex-wrap gap-2">
                    {opp.matchReasons.map((r) => renderReasonBadge(r))}
                  </div>
                </div>
              </div>
            ))}

            {/* Pagination Controls */}
            {opportunitiesData.totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 text-xs">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="px-3 py-1.5 bg-slate-900 border border-slate-800 disabled:opacity-40 text-slate-300 rounded-xl hover:bg-slate-800 transition-colors"
                >
                  Previous
                </button>
                <span className="text-slate-400">
                  Page {page + 1} of {opportunitiesData.totalPages}
                </span>
                <button
                  disabled={!opportunitiesData.hasNext}
                  onClick={() => setPage((p) => p + 1)}
                  className="px-3 py-1.5 bg-slate-900 border border-slate-800 disabled:opacity-40 text-slate-300 rounded-xl hover:bg-slate-800 transition-colors"
                >
                  Next
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
