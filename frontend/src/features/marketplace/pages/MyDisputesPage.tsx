import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMyDisputesQuery } from '../api/marketplaceDisputesApi';
import { DisputeCard } from '../components/disputes/DisputeCard';
import { DisputeFilters } from '../components/disputes/DisputeFilters';

export const MyDisputesPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [cursorHistory, setCursorHistory] = useState<Array<string | undefined>>([undefined]);
  const [cursorIndex, setCursorIndex] = useState<number>(0);

  const currentCursor = cursorHistory[cursorIndex];

  const { data, isLoading, isError, refetch } = useMyDisputesQuery({
    status: statusFilter,
    size: 20,
    cursor: currentCursor,
  });

  const handleStatusChange = (newStatus: string) => {
    setStatusFilter(newStatus);
    setCursorHistory([undefined]);
    setCursorIndex(0);
  };

  const handleNextPage = () => {
    if (data?.nextCursor) {
      const nextHistory = [...cursorHistory.slice(0, cursorIndex + 1), data.nextCursor];
      setCursorHistory(nextHistory);
      setCursorIndex(cursorIndex + 1);
    }
  };

  const handlePreviousPage = () => {
    if (cursorIndex > 0) {
      setCursorIndex(cursorIndex - 1);
    }
  };

  const items = data?.items || [];
  const openCount = items.filter((d) => d.status === 'OPEN').length;
  const underReviewCount = items.filter((d) => d.status === 'UNDER_REVIEW' || d.status === 'WAITING_FOR_INFORMATION').length;
  const resolvedCount = items.filter((d) => d.status === 'RESOLVED' || d.status === 'CLOSED').length;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Navigation & Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-emerald-400 flex items-center gap-2">
                <span>⚠️</span> My Disputes
              </h1>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-mono bg-slate-800 text-slate-300">
                Created by you
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Track issues you&apos;ve reported about your marketplace orders.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Link
              to="/marketplace/disputes/related"
              className="px-3.5 py-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-medium rounded-xl transition-colors"
            >
              View Related Disputes →
            </Link>
            <Link
              to="/marketplace/orders"
              className="px-3.5 py-2 bg-emerald-900/60 hover:bg-emerald-800/80 border border-emerald-700/50 text-emerald-200 text-xs font-medium rounded-xl transition-colors"
            >
              ← My Orders
            </Link>
          </div>
        </div>

        {/* Current View Metric Breakdown */}
        {items.length > 0 && (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-slate-900/60 border border-slate-800/80 rounded-xl p-3 text-center">
              <span className="text-xs text-slate-400 block">Current Page Total</span>
              <span className="text-xl font-bold text-slate-100 font-mono">{items.length}</span>
            </div>
            <div className="bg-amber-950/30 border border-amber-900/40 rounded-xl p-3 text-center">
              <span className="text-xs text-amber-300/80 block">Open in View</span>
              <span className="text-xl font-bold text-amber-300 font-mono">{openCount}</span>
            </div>
            <div className="bg-sky-950/30 border border-sky-900/40 rounded-xl p-3 text-center">
              <span className="text-xs text-sky-300/80 block">Under Review in View</span>
              <span className="text-xl font-bold text-sky-300 font-mono">{underReviewCount}</span>
            </div>
            <div className="bg-emerald-950/30 border border-emerald-900/40 rounded-xl p-3 text-center">
              <span className="text-xs text-emerald-300/80 block">Resolved / Closed in View</span>
              <span className="text-xl font-bold text-emerald-300 font-mono">{resolvedCount}</span>
            </div>
          </div>
        )}

        {/* Filter Bar */}
        <DisputeFilters
          selectedStatus={statusFilter}
          onSelectStatus={handleStatusChange}
        />

        {/* Content Section */}
        {isLoading ? (
          <div className="py-20 text-center space-y-3">
            <div className="w-8 h-8 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto" />
            <p className="text-xs text-emerald-400 font-medium">Loading your reported disputes...</p>
          </div>
        ) : isError ? (
          <div className="bg-rose-950/60 border border-rose-800 text-rose-200 p-6 rounded-2xl text-center space-y-3 shadow-xl">
            <span className="text-3xl block">⚠️</span>
            <p className="text-xs font-semibold">Failed to load dispute records. Please verify your connection.</p>
            <button
              type="button"
              onClick={() => refetch()}
              className="px-4 py-2 bg-rose-900 hover:bg-rose-800 text-white text-xs font-medium rounded-xl transition-colors"
            >
              Retry
            </button>
          </div>
        ) : items.length === 0 ? (
          <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl p-12 text-center space-y-4 shadow-xl">
            <span className="text-4xl block">⚖️</span>
            <div className="space-y-1">
              <h3 className="text-sm font-semibold text-slate-300">
                You haven&apos;t reported any issues yet.
              </h3>
              <p className="text-xs text-slate-400 max-w-md mx-auto">
                {statusFilter === 'ALL'
                  ? 'If you encounter damaged produce, delivery delays, or quality mismatches on an order, you can report an issue directly from the order page.'
                  : `No disputes found matching filter "${statusFilter}".`}
              </p>
            </div>
            <Link
              to="/marketplace/orders"
              className="inline-block px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-xl shadow-md transition-colors"
            >
              Browse Orders to Report Issue
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-4" role="feed" aria-label="My disputes list">
              {items.map((dispute) => (
                <DisputeCard
                  key={dispute.disputeId}
                  dispute={dispute}
                  viewMode="mine"
                />
              ))}
            </div>

            {/* Cursor-Based Pagination Controls */}
            <nav
              className="pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400"
              aria-label="Disputes pagination"
            >
              <div>
                <span>Page </span>
                <strong className="text-slate-200 font-mono">{cursorIndex + 1}</strong>
                {data?.totalElements && data.totalElements > 0 ? (
                  <span> of {data.totalPages}</span>
                ) : null}
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handlePreviousPage}
                  disabled={cursorIndex === 0 || isLoading}
                  className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed border border-slate-800 text-slate-300 rounded-xl transition-all font-medium focus:outline-none focus-visible:ring-1 focus-visible:ring-emerald-400"
                >
                  ← Previous
                </button>
                <button
                  type="button"
                  onClick={handleNextPage}
                  disabled={!data?.hasNext || !data?.nextCursor || isLoading}
                  className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed border border-slate-800 text-slate-300 rounded-xl transition-all font-medium focus:outline-none focus-visible:ring-1 focus-visible:ring-emerald-400"
                >
                  Next →
                </button>
              </div>
            </nav>
          </div>
        )}
      </div>
    </div>
  );
};
