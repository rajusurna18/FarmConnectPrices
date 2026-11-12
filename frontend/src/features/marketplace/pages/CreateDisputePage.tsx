import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useOrderDetailQuery } from '../api/marketplaceOrdersApi';
import {
  useOrderDisputesQuery,
  useCreateDisputeMutation,
  extractDisputeApiErrorMessage,
} from '../api/marketplaceDisputesApi';
import type { DisputeCategory } from '../../../types/dispute';
import { DISPUTE_CATEGORY_LABELS, isDisputeActive } from '../../../types/dispute';
import { DisputeStatusBadge } from '../components/disputes/DisputeStatusBadge';

const CATEGORY_OPTIONS: DisputeCategory[] = [
  'PRODUCT_QUALITY',
  'DAMAGED_PRODUCT',
  'QUANTITY_MISMATCH',
  'WRONG_PRODUCT',
  'DELIVERY_DELAY',
  'DELIVERY_NOT_RECEIVED',
  'PAYMENT_ISSUE',
];

export const CreateDisputePage: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();

  const [issueCategory, setIssueCategory] = useState<DisputeCategory>('PRODUCT_QUALITY');
  const [subject, setSubject] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [clientError, setClientError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);

  const { data: order, isLoading: isOrderLoading, isError: isOrderError } = useOrderDetailQuery(
    orderId || ''
  );
  const { data: existingDisputes, isLoading: isDisputesLoading } = useOrderDisputesQuery(
    orderId || ''
  );

  const createDisputeMutation = useCreateDisputeMutation();

  if (isOrderLoading) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8 flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="w-8 h-8 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-xs text-emerald-400 font-medium">Loading order details...</p>
        </div>
      </div>
    );
  }

  if (isOrderError || !order) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
        <div className="max-w-xl mx-auto bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center space-y-4">
          <span className="text-3xl block">⚠️</span>
          <h2 className="text-lg font-bold text-rose-300">Order Not Found</h2>
          <p className="text-xs text-rose-200">
            The requested order could not be found or you are not authorized to view it.
          </p>
          <div className="pt-2">
            <Link
              to="/marketplace/orders"
              className="inline-block px-4 py-2 bg-rose-900 hover:bg-rose-800 text-white text-xs font-semibold rounded-xl transition-colors"
            >
              ← Back to My Orders
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const activeDispute = existingDisputes?.find((d) => isDisputeActive(d.status));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setClientError(null);
    setServerError(null);

    const trimmedSubject = subject.trim();
    const trimmedDesc = description.trim();

    if (!trimmedSubject) {
      setClientError('Please enter a brief subject for this dispute.');
      return;
    }
    if (trimmedSubject.length < 3) {
      setClientError('Subject must be at least 3 characters long.');
      return;
    }
    if (trimmedSubject.length > 100) {
      setClientError('Subject cannot exceed 100 characters.');
      return;
    }

    if (!trimmedDesc) {
      setClientError('Please provide a detailed description of the issue.');
      return;
    }
    if (trimmedDesc.length < 10) {
      setClientError('Description must be at least 10 characters long to provide sufficient detail.');
      return;
    }
    if (trimmedDesc.length > 2000) {
      setClientError('Description cannot exceed 2000 characters.');
      return;
    }

    try {
      const result = await createDisputeMutation.mutateAsync({
        orderId: order.orderId,
        issueCategory,
        subject: trimmedSubject,
        description: trimmedDesc,
      });
      navigate(`/marketplace/disputes/${result.disputeId}`);
    } catch (err) {
      const msg = extractDisputeApiErrorMessage(err);
      setServerError(msg);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-3xl mx-auto space-y-6">
        {/* Header Breadcrumbs */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div className="space-y-1">
            <h1 className="text-2xl font-bold text-amber-400 flex items-center gap-2">
              <span>⚠️</span> Report Order Issue
            </h1>
            <p className="text-xs text-slate-400">
              Open a structured dispute resolution case for order{' '}
              <span className="font-mono text-slate-300 font-semibold">{order.orderId}</span>
            </p>
          </div>
          <Link
            to={`/marketplace/orders/${order.orderId}`}
            className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-medium rounded-xl transition-colors"
          >
            ← Back to Order
          </Link>
        </div>

        {/* Order Reference Card */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 shadow-lg space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
            <div>
              <span className="text-xs text-slate-400 block font-mono">Order Reference</span>
              <span className="text-sm font-semibold text-slate-200">{order.cropName || 'Produce'}</span>
            </div>
            <div className="text-right">
              <span className="text-xs text-slate-400 block">Status</span>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-semibold bg-emerald-950/80 border border-emerald-800 text-emerald-300">
                {order.status}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
            <div>
              <span className="text-slate-400 block">Quantity</span>
              <span className="font-semibold text-slate-200">
                {order.totalQuantity} {order.quantityUnit}
              </span>
            </div>
            <div>
              <span className="text-slate-400 block">Agreed Price</span>
              <span className="font-semibold text-slate-200">
                ₹{order.agreedPrice} / {order.priceUnit}
              </span>
            </div>
            <div>
              <span className="text-slate-400 block">Total Amount</span>
              <span className="font-semibold text-emerald-400">
                ₹{order.totalAmount} {order.currency}
              </span>
            </div>
            <div>
              <span className="text-slate-400 block">Order Date</span>
              <span className="font-mono text-slate-300">
                {order.createdAt ? new Date(order.createdAt).toLocaleDateString() : 'N/A'}
              </span>
            </div>
          </div>
        </div>

        {/* Existing Active Dispute Notice */}
        {activeDispute && (
          <div className="bg-amber-950/50 border border-amber-800/80 text-amber-200 p-5 rounded-2xl space-y-3">
            <div className="flex items-start gap-3">
              <span className="text-2xl">⚠️</span>
              <div className="space-y-1">
                <h4 className="text-sm font-semibold text-amber-300">
                  Active Dispute Already Exists for this Order
                </h4>
                <p className="text-xs text-amber-200/90 leading-relaxed">
                  Dispute <strong className="font-mono">{activeDispute.disputeId}</strong> is currently{' '}
                  <strong className="font-semibold">{activeDispute.status}</strong>. Only one active
                  dispute can exist on an order at any given time.
                </p>
                <div className="pt-2 flex items-center gap-3">
                  <DisputeStatusBadge status={activeDispute.status} />
                  <Link
                    to={`/marketplace/disputes/${activeDispute.disputeId}`}
                    className="px-3 py-1 bg-amber-900/80 hover:bg-amber-800 text-amber-100 text-xs font-semibold rounded-lg border border-amber-700/60 transition-colors"
                  >
                    View Active Dispute →
                  </Link>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Form Container */}
        <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl p-6 shadow-xl">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Category selection */}
            <div>
              <label htmlFor="issueCategory" className="block text-xs font-semibold text-slate-300 mb-1.5">
                Issue Category <span className="text-rose-400">*</span>
              </label>
              <select
                id="issueCategory"
                value={issueCategory}
                onChange={(e) => setIssueCategory(e.target.value as DisputeCategory)}
                disabled={Boolean(activeDispute) || createDisputeMutation.isPending}
                className="w-full bg-slate-950 border border-slate-800 text-slate-100 text-xs rounded-xl px-3.5 py-2.5 focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500 transition-colors disabled:opacity-50"
              >
                {CATEGORY_OPTIONS.map((cat) => (
                  <option key={cat} value={cat}>
                    {DISPUTE_CATEGORY_LABELS[cat] || cat}
                  </option>
                ))}
              </select>
              <p className="text-[11px] text-slate-400 mt-1">
                Select the category that best describes the problem with this shipment or delivery.
              </p>
            </div>

            {/* Subject input */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label htmlFor="disputeSubject" className="block text-xs font-semibold text-slate-300">
                  Subject / Summary <span className="text-rose-400">*</span>
                </label>
                <span className="text-[11px] font-mono text-slate-400">
                  {subject.length} / 100
                </span>
              </div>
              <input
                id="disputeSubject"
                type="text"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                maxLength={100}
                placeholder="e.g., 20 crates arrived damaged due to moisture during transport"
                disabled={Boolean(activeDispute) || createDisputeMutation.isPending}
                className="w-full bg-slate-950 border border-slate-800 text-slate-100 text-xs rounded-xl px-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500 transition-colors disabled:opacity-50"
              />
            </div>

            {/* Description textarea */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label htmlFor="disputeDescription" className="block text-xs font-semibold text-slate-300">
                  Detailed Description <span className="text-rose-400">*</span>
                </label>
                <span className="text-[11px] font-mono text-slate-400">
                  {description.length} / 2000
                </span>
              </div>
              <textarea
                id="disputeDescription"
                rows={6}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={2000}
                placeholder="Describe what occurred, any inspection findings, timestamps, and your proposed resolution..."
                disabled={Boolean(activeDispute) || createDisputeMutation.isPending}
                className="w-full bg-slate-950 border border-slate-800 text-slate-100 text-xs rounded-xl px-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500 transition-colors resize-y disabled:opacity-50"
              />
              <p className="text-[11px] text-slate-400 mt-1">
                Please provide factual details. After creating the dispute, you will be able to register supporting evidence metadata and communicate with the counterparty.
              </p>
            </div>

            {/* Error alerts */}
            {clientError && (
              <div className="p-3 bg-amber-950/60 border border-amber-800 text-amber-200 text-xs rounded-xl">
                {clientError}
              </div>
            )}
            {serverError && (
              <div className="p-3 bg-rose-950/60 border border-rose-800 text-rose-200 text-xs rounded-xl">
                {serverError}
              </div>
            )}

            {/* Actions */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800/80">
              <Link
                to={`/marketplace/orders/${order.orderId}`}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-slate-300 text-xs font-medium rounded-xl border border-slate-800 transition-colors"
              >
                Cancel
              </Link>
              <button
                type="submit"
                disabled={Boolean(activeDispute) || createDisputeMutation.isPending || isDisputesLoading}
                className="px-5 py-2 bg-amber-600 hover:bg-amber-500 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-semibold rounded-xl shadow-lg transition-all focus:outline-none focus:ring-2 focus:ring-amber-400 flex items-center gap-2"
              >
                {createDisputeMutation.isPending && (
                  <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                )}
                <span>Submit Dispute</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
