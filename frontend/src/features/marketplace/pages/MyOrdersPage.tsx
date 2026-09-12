import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMyOrdersQuery } from '../api/marketplaceOrdersApi';

export const MyOrdersPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);

  const { data, isLoading, isError, refetch } = useMyOrdersQuery(statusFilter, page, 20);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <span className="px-2.5 py-1 bg-amber-950/80 border border-amber-700/60 text-amber-300 text-xs font-semibold rounded-full">PENDING CONFIRMATION</span>;
      case 'CONFIRMED':
        return <span className="px-2.5 py-1 bg-sky-950/80 border border-sky-700/60 text-sky-300 text-xs font-semibold rounded-full">CONFIRMED</span>;
      case 'PROCESSING':
        return <span className="px-2.5 py-1 bg-indigo-950/80 border border-indigo-700/60 text-indigo-300 text-xs font-semibold rounded-full">PROCESSING</span>;
      case 'COMPLETED':
        return <span className="px-2.5 py-1 bg-emerald-950/80 border border-emerald-700/60 text-emerald-300 text-xs font-semibold rounded-full">COMPLETED</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-1 bg-rose-950/80 border border-rose-700/60 text-rose-300 text-xs font-semibold rounded-full">CANCELLED</span>;
      default:
        return <span className="px-2.5 py-1 bg-slate-800 text-slate-300 text-xs font-semibold rounded-full">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
          <div>
            <h1 className="text-2xl font-bold text-emerald-400 flex items-center gap-2">
              <span>🛍️ My Orders</span>
            </h1>
            <p className="text-xs text-slate-400 mt-1">
              Track and manage marketplace purchases initiated from accepted offer proposals.
            </p>
          </div>

          <Link
            to="/marketplace"
            className="self-start md:self-auto px-4 py-2 bg-emerald-900/60 hover:bg-emerald-800/80 border border-emerald-700/50 text-emerald-200 text-xs font-medium rounded-xl transition-colors"
          >
            ← Marketplace
          </Link>
        </div>

        {/* Filter Bar */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2">
          {['ALL', 'PENDING', 'CONFIRMED', 'PROCESSING', 'COMPLETED', 'CANCELLED'].map((st) => (
            <button
              key={st}
              onClick={() => {
                setStatusFilter(st);
                setPage(0);
              }}
              className={`px-3 py-1.5 text-xs font-medium rounded-xl transition-colors ${
                statusFilter === st
                  ? 'bg-emerald-600 text-white font-semibold shadow-md'
                  : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-slate-200'
              }`}
            >
              {st}
            </button>
          ))}
        </div>

        {/* Content Section */}
        {isLoading ? (
          <div className="text-center py-16 text-emerald-400 text-xs font-medium">
            Loading your orders...
          </div>
        ) : isError ? (
          <div className="bg-rose-950/60 border border-rose-800 text-rose-200 p-6 rounded-2xl text-center space-y-3">
            <p className="text-xs">Failed to load order history. Please try again.</p>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-rose-900 text-white text-xs font-medium rounded-xl hover:bg-rose-800 transition-colors"
            >
              Retry
            </button>
          </div>
        ) : !data?.items || data.items.length === 0 ? (
          <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl p-12 text-center space-y-3">
            <span className="text-4xl block">📦</span>
            <h3 className="text-sm font-semibold text-slate-300">No Orders Found</h3>
            <p className="text-xs text-slate-400 max-w-md mx-auto">
              You have not placed any marketplace orders matching status &quot;{statusFilter}&quot;. Orders are created after an offer proposal is accepted.
            </p>
            <Link
              to="/marketplace/offers"
              className="inline-block mt-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium rounded-xl transition-colors shadow-lg"
            >
              View My Offers
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {data.items.map((order) => (
              <div
                key={order.orderId}
                className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 hover:border-emerald-700/50 transition-all space-y-4 shadow-lg"
              >
                <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-800/60 pb-3">
                  <div>
                    <span className="text-[10px] font-mono text-slate-500 block">ID: {order.orderId}</span>
                    <h2 className="text-base font-bold text-slate-100 flex items-center gap-2 mt-0.5">
                      <span>🌾 {order.cropName}</span>
                    </h2>
                  </div>

                  <div className="flex items-center gap-3">
                    {getStatusBadge(order.status)}
                  </div>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs">
                  <div>
                    <span className="text-slate-400 block text-[11px]">Agreed Quantity</span>
                    <span className="font-semibold text-slate-200">
                      {order.totalQuantity} {order.quantityUnit}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-400 block text-[11px]">Agreed Unit Price</span>
                    <span className="font-semibold text-slate-200">
                      ₹{order.agreedPrice.toLocaleString('en-IN')} / {order.priceUnit}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-400 block text-[11px]">Total Commercial Amount</span>
                    <span className="font-extrabold text-emerald-300">
                      ₹{order.totalAmount.toLocaleString('en-IN')}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-400 block text-[11px]">Order Date</span>
                    <span className="text-slate-300">
                      {new Date(order.createdAt).toLocaleDateString('en-IN', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      })}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-2">
                  <span className="text-[11px] text-slate-400">
                    Offer Reference: <span className="font-mono text-slate-300">{order.offerId}</span>
                  </span>

                  <Link
                    to={`/marketplace/orders/${order.orderId}`}
                    className="px-4 py-1.5 bg-emerald-950/80 hover:bg-emerald-900 border border-emerald-700/60 text-emerald-300 text-xs font-medium rounded-xl transition-colors flex items-center gap-1"
                  >
                    View Order Details →
                  </Link>
                </div>
              </div>
            ))}

            {/* Pagination Controls */}
            {data.totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 text-xs">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="px-3 py-1.5 bg-slate-900 border border-slate-800 disabled:opacity-40 text-slate-300 rounded-xl hover:bg-slate-800 transition-colors"
                >
                  Previous
                </button>
                <span className="text-slate-400">
                  Page {page + 1} of {data.totalPages}
                </span>
                <button
                  disabled={!data.hasNext}
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
