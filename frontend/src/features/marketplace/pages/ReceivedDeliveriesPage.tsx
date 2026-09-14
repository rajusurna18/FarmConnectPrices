import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useReceivedDeliveriesQuery } from '../api/marketplaceDeliveriesApi';

export const ReceivedDeliveriesPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);

  const { data, isLoading, isError, refetch } = useReceivedDeliveriesQuery(statusFilter, page, 20);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'CREATED':
        return <span className="px-2.5 py-0.5 bg-amber-950/80 border border-amber-700/60 text-amber-300 text-[11px] font-semibold rounded-full">ACTION NEEDED: ASSIGN PARTNER</span>;
      case 'ASSIGNED':
        return <span className="px-2.5 py-0.5 bg-sky-950/80 border border-sky-700/60 text-sky-300 text-[11px] font-semibold rounded-full">PARTNER ASSIGNED</span>;
      case 'READY_FOR_PICKUP':
        return <span className="px-2.5 py-0.5 bg-indigo-950/80 border border-indigo-700/60 text-indigo-300 text-[11px] font-semibold rounded-full">READY FOR PICKUP</span>;
      case 'PICKED_UP':
        return <span className="px-2.5 py-0.5 bg-purple-950/80 border border-purple-700/60 text-purple-300 text-[11px] font-semibold rounded-full">PICKED UP</span>;
      case 'IN_TRANSIT':
        return <span className="px-2.5 py-0.5 bg-blue-950/80 border border-blue-700/60 text-blue-300 text-[11px] font-semibold rounded-full">IN TRANSIT</span>;
      case 'OUT_FOR_DELIVERY':
        return <span className="px-2.5 py-0.5 bg-teal-950/80 border border-teal-700/60 text-teal-300 text-[11px] font-semibold rounded-full font-bold">OUT FOR DELIVERY</span>;
      case 'DELIVERED':
        return <span className="px-2.5 py-0.5 bg-emerald-950/80 border border-emerald-700/60 text-emerald-300 text-[11px] font-semibold rounded-full font-bold">DELIVERED & COMPLETED</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-0.5 bg-rose-950/80 border border-rose-700/60 text-rose-300 text-[11px] font-semibold rounded-full">CANCELLED</span>;
      default:
        return <span className="px-2.5 py-0.5 bg-slate-800 text-slate-300 text-[11px] font-semibold rounded-full">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Navigation & Header */}
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <Link to="/marketplace" className="text-xs font-medium text-emerald-400 hover:text-emerald-300 transition-colors flex items-center gap-1 mb-1">
              ← Back to Marketplace
            </Link>
            <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
              <span>🌾 Dispatch & Logistics Management</span>
            </h1>
            <p className="text-xs text-slate-400 mt-1">Manage delivery partner assignment and pickup/dispatch for received orders.</p>
          </div>

          <Link
            to="/marketplace/received-orders"
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
          >
            Received Orders
          </Link>
        </div>

        {/* Status Filter Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-800">
          {['ALL', 'CREATED', 'ASSIGNED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'].map((filter) => (
            <button
              key={filter}
              onClick={() => {
                setStatusFilter(filter);
                setPage(0);
              }}
              className={`px-3 py-1.5 text-xs font-medium rounded-xl whitespace-nowrap transition-colors ${
                statusFilter === filter
                  ? 'bg-emerald-500/20 border border-emerald-500/60 text-emerald-300'
                  : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              {filter.replace(/_/g, ' ')}
            </button>
          ))}
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="text-center py-16 text-xs text-emerald-400">Loading received deliveries...</div>
        )}

        {/* Error State */}
        {isError && (
          <div className="bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center space-y-3">
            <p className="text-xs text-rose-300 font-medium">Failed to load received delivery records.</p>
            <button onClick={() => refetch()} className="px-4 py-2 bg-rose-900 text-xs text-white rounded-xl">
              Retry
            </button>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !isError && data?.items.length === 0 && (
          <div className="bg-slate-900/60 border border-slate-800 p-12 rounded-2xl text-center space-y-3">
            <div className="text-3xl">🚜</div>
            <h3 className="text-sm font-bold text-slate-300">No Delivery Dispatches Found</h3>
            <p className="text-xs text-slate-400">You have no active or completed dispatches under this status.</p>
          </div>
        )}

        {/* Deliveries List */}
        {!isLoading && !isError && data && data.items.length > 0 && (
          <div className="grid gap-4">
            {data.items.map((delivery) => (
              <div
                key={delivery.deliveryId}
                className="bg-slate-900/80 border border-slate-800 hover:border-slate-700 rounded-2xl p-5 shadow-lg transition-all flex flex-wrap items-center justify-between gap-4"
              >
                <div className="space-y-1.5">
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-mono text-emerald-400 font-bold">
                      ID: {delivery.deliveryId}
                    </span>
                    {getStatusBadge(delivery.status)}
                  </div>

                  <div className="text-xs text-slate-300">
                    Order ID:{' '}
                    <Link to={`/marketplace/orders/${delivery.orderId}`} className="text-emerald-400 font-mono underline hover:text-emerald-300">
                      {delivery.orderId}
                    </Link>
                    {delivery.partner && (
                      <span className="ml-3 text-slate-400">
                        Partner: <strong className="text-slate-200">{delivery.partner.name}</strong> ({delivery.partner.vehicleType})
                      </span>
                    )}
                  </div>

                  <div className="text-[11px] text-slate-400">
                    Pickup Location:{' '}
                    {delivery.pickupAddress
                      ? `${delivery.pickupAddress.village || ''}, ${delivery.pickupAddress.mandal || ''}, ${delivery.pickupAddress.district || ''}`
                      : 'Farm Location'}
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <Link
                    to={`/marketplace/deliveries/${delivery.deliveryId}`}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-xl shadow-lg transition-colors"
                  >
                    Manage Dispatch & Status →
                  </Link>
                </div>
              </div>
            ))}

            {/* Pagination Controls */}
            {data.totalPages > 1 && (
              <div className="flex items-center justify-between pt-4">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="px-3 py-1.5 bg-slate-900 border border-slate-800 text-xs font-medium text-slate-300 rounded-xl disabled:opacity-50"
                >
                  Previous
                </button>
                <span className="text-xs text-slate-400 font-mono">
                  Page {page + 1} of {data.totalPages}
                </span>
                <button
                  disabled={!data.hasNext}
                  onClick={() => setPage((p) => p + 1)}
                  className="px-3 py-1.5 bg-slate-900 border border-slate-800 text-xs font-medium text-slate-300 rounded-xl disabled:opacity-50"
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
