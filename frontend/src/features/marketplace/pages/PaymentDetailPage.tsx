import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { usePaymentDetailQuery } from '../api/marketplacePaymentsApi';

export const PaymentDetailPage: React.FC = () => {
  const { paymentId } = useParams<{ paymentId: string }>();

  const { data: payment, isLoading, isError } = usePaymentDetailQuery(paymentId || '');

  if (isLoading) {
    return <div className="min-h-screen bg-slate-950 text-emerald-400 text-center py-16 text-sm">Loading payment details...</div>;
  }

  if (isError || !payment) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
        <div className="max-w-xl mx-auto bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center space-y-4">
          <h2 className="text-lg font-bold text-rose-300">Payment Record Not Found</h2>
          <p className="text-xs text-rose-200">The requested payment detail could not be loaded or authorized.</p>
          <Link to="/marketplace/payments" className="inline-block px-4 py-2 bg-rose-900 text-xs rounded-xl text-white">
            Return to My Payments
          </Link>
        </div>
      </div>
    );
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <span className="px-3 py-1 bg-emerald-950/80 border border-emerald-700/60 text-emerald-300 text-xs font-semibold rounded-full">SUCCESS</span>;
      case 'PENDING':
        return <span className="px-3 py-1 bg-amber-950/80 border border-amber-700/60 text-amber-300 text-xs font-semibold rounded-full">PENDING</span>;
      case 'PROCESSING':
        return <span className="px-3 py-1 bg-sky-950/80 border border-sky-700/60 text-sky-300 text-xs font-semibold rounded-full">PROCESSING</span>;
      case 'FAILED':
        return <span className="px-3 py-1 bg-rose-950/80 border border-rose-700/60 text-rose-300 text-xs font-semibold rounded-full">FAILED</span>;
      default:
        return <span className="px-3 py-1 bg-slate-800 text-slate-300 text-xs font-semibold rounded-full">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-3xl mx-auto space-y-6">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center justify-between">
          <Link
            to="/marketplace/payments"
            className="text-xs font-medium text-emerald-400 hover:text-emerald-300 transition-colors"
          >
            ← Back to My Payments
          </Link>
          <span className="text-xs font-mono text-slate-400">Payment ID: {payment.paymentId}</span>
        </div>

        {/* Overview Header Card */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-2xl font-bold text-slate-100">Payment Details</h1>
                {getStatusBadge(payment.status)}
              </div>
              <p className="text-xs text-slate-400 mt-1 font-mono">
                Order Reference: <Link to={`/marketplace/orders/${payment.orderId}`} className="text-emerald-400 hover:underline">{payment.orderId}</Link>
              </p>
            </div>

            <div className="text-right">
              <span className="text-xs text-slate-400 block">Verified Amount</span>
              <span className="text-3xl font-extrabold text-emerald-300 font-mono">
                ₹{payment.amount.toLocaleString('en-IN')} {payment.currency}
              </span>
            </div>
          </div>
        </div>

        {/* Detail Breakdown Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 text-xs space-y-3">
            <h2 className="font-bold text-slate-300 border-b border-slate-800 pb-2">Provider & Transaction Info</h2>
            <div className="flex justify-between">
              <span className="text-slate-400">Provider:</span>
              <span className="font-mono text-slate-200">{payment.provider}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Payment Method:</span>
              <span className="font-mono text-slate-200">{payment.paymentMethod || 'N/A'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Provider Payment ID:</span>
              <span className="font-mono text-slate-200">{payment.providerPaymentId || 'N/A'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Transaction Reference:</span>
              <span className="font-mono text-slate-200">{payment.transactionReference || 'N/A'}</span>
            </div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 text-xs space-y-3">
            <h2 className="font-bold text-slate-300 border-b border-slate-800 pb-2">Audit & Timestamps</h2>
            <div className="flex justify-between">
              <span className="text-slate-400">Created At:</span>
              <span className="text-slate-300">{new Date(payment.createdAt).toLocaleString('en-IN')}</span>
            </div>
            {payment.initiatedAt && (
              <div className="flex justify-between">
                <span className="text-slate-400">Initiated At:</span>
                <span className="text-slate-300">{new Date(payment.initiatedAt).toLocaleString('en-IN')}</span>
              </div>
            )}
            {payment.verifiedAt && (
              <div className="flex justify-between text-emerald-300 font-semibold">
                <span>Verified At:</span>
                <span>{new Date(payment.verifiedAt).toLocaleString('en-IN')}</span>
              </div>
            )}
            {payment.failedAt && (
              <div className="flex justify-between text-rose-400 font-semibold">
                <span>Failed At:</span>
                <span>{new Date(payment.failedAt).toLocaleString('en-IN')}</span>
              </div>
            )}
            {payment.failureCode && (
              <div className="pt-2 text-rose-400 border-t border-slate-800">
                Error ({payment.failureCode}): &quot;{payment.failureMessage}&quot;
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
