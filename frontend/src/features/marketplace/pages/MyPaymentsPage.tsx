import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMyPaymentsQuery } from '../api/marketplacePaymentsApi';

export const MyPaymentsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);
  const size = 20;

  const { data: pageData, isLoading, isError } = useMyPaymentsQuery(statusFilter, page, size);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <span className="px-2.5 py-0.5 bg-emerald-950/80 border border-emerald-700/60 text-emerald-300 text-xs font-semibold rounded-full">SUCCESS</span>;
      case 'PENDING':
        return <span className="px-2.5 py-0.5 bg-amber-950/80 border border-amber-700/60 text-amber-300 text-xs font-semibold rounded-full">PENDING</span>;
      case 'PROCESSING':
        return <span className="px-2.5 py-0.5 bg-sky-950/80 border border-sky-700/60 text-sky-300 text-xs font-semibold rounded-full">PROCESSING</span>;
      case 'FAILED':
        return <span className="px-2.5 py-0.5 bg-rose-950/80 border border-rose-700/60 text-rose-300 text-xs font-semibold rounded-full">FAILED</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-0.5 bg-slate-800 border border-slate-700 text-slate-400 text-xs font-semibold rounded-full">CANCELLED</span>;
      case 'EXPIRED':
        return <span className="px-2.5 py-0.5 bg-slate-800 border border-slate-700 text-slate-400 text-xs font-semibold rounded-full">EXPIRED</span>;
      default:
        return <span className="px-2.5 py-0.5 bg-slate-800 text-slate-300 text-xs font-semibold rounded-full">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
              <span>💳 My Payments</span>
            </h1>
            <p className="text-xs text-slate-400 mt-1">
              View authoritative marketplace payment history and verification status.
            </p>
          </div>

          <Link
            to="/marketplace/orders"
            className="px-4 py-2 bg-slate-900 border border-slate-800 text-slate-300 hover:text-white rounded-xl text-xs font-medium transition-colors"
          >
            View My Orders →
          </Link>
        </div>

        {/* Status Filter Bar */}
        <div className="flex flex-wrap items-center gap-2 bg-slate-900/60 border border-slate-800 p-2 rounded-2xl">
          {['ALL', 'SUCCESS', 'PENDING', 'FAILED', 'CANCELLED'].map((st) => (
            <button
              key={st}
              onClick={() => {
                setStatusFilter(st);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded-xl text-xs font-medium transition-colors ${
                statusFilter === st
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              {st}
            </button>
          ))}
        </div>

        {/* Content Body */}
        {isLoading ? (
          <div className="text-center py-16 text-emerald-400 text-sm">Loading payment records...</div>
        ) : isError ? (
          <div className="bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center text-xs text-rose-300">
            Failed to load payment history. Please verify your authentication.
          </div>
        ) : !pageData || pageData.items.length === 0 ? (
          <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-12 text-center text-slate-400 space-y-3">
            <span className="text-3xl block">💳</span>
            <p className="text-sm font-medium text-slate-300">No payment records found.</p>
            <p className="text-xs text-slate-400">Payments created for confirmed marketplace orders will appear here.</p>
          </div>
        ) : (
          <div className="space-y-3">
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/80 text-slate-400 uppercase text-[10px] tracking-wider border-b border-slate-800">
                  <tr>
                    <th className="p-4">Payment ID</th>
                    <th className="p-4">Order ID</th>
                    <th className="p-4">Amount</th>
                    <th className="p-4">Provider</th>
                    <th className="p-4">Status</th>
                    <th className="p-4">Date</th>
                    <th className="p-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {pageData.items.map((payment) => (
                    <tr key={payment.paymentId} className="hover:bg-slate-800/40 transition-colors">
                      <td className="p-4 font-mono font-semibold text-emerald-400">{payment.paymentId}</td>
                      <td className="p-4 font-mono text-slate-400">
                        <Link to={`/marketplace/orders/${payment.orderId}`} className="hover:underline text-emerald-400">
                          {payment.orderId}
                        </Link>
                      </td>
                      <td className="p-4 font-bold text-slate-100 font-mono">
                        ₹{payment.amount.toLocaleString('en-IN')} {payment.currency}
                      </td>
                      <td className="p-4 text-slate-400 font-mono">{payment.provider}</td>
                      <td className="p-4">{getStatusBadge(payment.status)}</td>
                      <td className="p-4 text-slate-400">{new Date(payment.createdAt).toLocaleString('en-IN')}</td>
                      <td className="p-4 text-right">
                        <Link
                          to={`/marketplace/payments/${payment.paymentId}`}
                          className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-medium transition-colors"
                        >
                          Details →
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {pageData.totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 text-xs text-slate-400">
                <span>
                  Page {pageData.page + 1} of {pageData.totalPages} ({pageData.totalElements} records)
                </span>
                <div className="flex items-center gap-2">
                  <button
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                    className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg disabled:opacity-40"
                  >
                    Previous
                  </button>
                  <button
                    disabled={!pageData.hasNext}
                    onClick={() => setPage((p) => p + 1)}
                    className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg disabled:opacity-40"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
