import React from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  useOrderDetailQuery,
  useConfirmOrderMutation,
  useProcessOrderMutation,
  useCompleteOrderMutation,
  useCancelOrderMutation,
} from '../api/marketplaceOrdersApi';

export const OrderDetailPage: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();

  const { data: order, isLoading, isError, refetch } = useOrderDetailQuery(orderId || '');

  const confirmMutation = useConfirmOrderMutation();
  const processMutation = useProcessOrderMutation();
  const completeMutation = useCompleteOrderMutation();
  const cancelMutation = useCancelOrderMutation();

  if (isLoading) {
    return <div className="min-h-screen bg-slate-950 text-emerald-400 text-center py-16 text-sm">Loading order details...</div>;
  }

  if (isError || !order) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
        <div className="max-w-xl mx-auto bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center space-y-4">
          <h2 className="text-lg font-bold text-rose-300">Order Not Found</h2>
          <p className="text-xs text-rose-200">The order could not be loaded or you are not authorized to view it.</p>
          <Link to="/marketplace" className="inline-block px-4 py-2 bg-rose-900 text-xs rounded-xl text-white font-medium">
            Return to Marketplace
          </Link>
        </div>
      </div>
    );
  }

  const handleConfirm = async () => {
    if (window.confirm('Are you sure you want to confirm this order?')) {
      await confirmMutation.mutateAsync(order.orderId);
      refetch();
    }
  };

  const handleProcess = async () => {
    if (window.confirm('Are you sure you want to move this order to processing?')) {
      await processMutation.mutateAsync(order.orderId);
      refetch();
    }
  };

  const handleComplete = async () => {
    if (window.confirm('Are you sure you want to mark this order as completed?')) {
      await completeMutation.mutateAsync(order.orderId);
      refetch();
    }
  };

  const handleCancel = async () => {
    const reason = window.prompt('Enter cancellation reason:');
    if (reason !== null) {
      await cancelMutation.mutateAsync({ orderId: order.orderId, reason });
      refetch();
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <span className="px-3 py-1 bg-amber-950/80 border border-amber-700/60 text-amber-300 text-xs font-semibold rounded-full">PENDING CONFIRMATION</span>;
      case 'CONFIRMED':
        return <span className="px-3 py-1 bg-sky-950/80 border border-sky-700/60 text-sky-300 text-xs font-semibold rounded-full">CONFIRMED</span>;
      case 'PROCESSING':
        return <span className="px-3 py-1 bg-indigo-950/80 border border-indigo-700/60 text-indigo-300 text-xs font-semibold rounded-full">PROCESSING</span>;
      case 'COMPLETED':
        return <span className="px-3 py-1 bg-emerald-950/80 border border-emerald-700/60 text-emerald-300 text-xs font-semibold rounded-full">COMPLETED</span>;
      case 'CANCELLED':
        return <span className="px-3 py-1 bg-rose-950/80 border border-rose-700/60 text-rose-300 text-xs font-semibold rounded-full">CANCELLED</span>;
      default:
        return <span className="px-3 py-1 bg-slate-800 text-slate-300 text-xs font-semibold rounded-full">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center justify-between">
          <Link
            to="/marketplace/orders"
            className="text-xs font-medium text-emerald-400 hover:text-emerald-300 transition-colors flex items-center gap-1"
          >
            ← Back to My Orders
          </Link>
          <span className="text-xs font-mono text-slate-400">Order ID: {order.orderId}</span>
        </div>

        {/* Overview Header Card */}
        <div className="bg-emerald-950/60 border border-emerald-800/50 rounded-2xl p-6 shadow-xl space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-emerald-200 flex items-center gap-2">
                <span>🌾 {order.cropName}</span>
              </h1>
              <p className="text-xs text-emerald-400 mt-1">
                Agreed Commercial Price: ₹{order.agreedPrice.toLocaleString('en-IN')} / {order.priceUnit}
              </p>
            </div>

            <div className="text-right">
              <span className="text-xs text-slate-400 block">Total Order Amount</span>
              <span className="text-3xl font-extrabold text-emerald-300">
                ₹{order.totalAmount.toLocaleString('en-IN')}
              </span>
              <span className="text-xs text-slate-300 block">
                Agreed Quantity: <strong>{order.totalQuantity} {order.quantityUnit}</strong>
              </span>
            </div>
          </div>

          {/* Action Bar */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-emerald-900/40">
            <div className="flex items-center gap-3">
              <span className="text-xs text-slate-400">Current Status:</span>
              {getStatusBadge(order.status)}
            </div>

            <div className="flex items-center gap-2">
              {order.canConfirm && (
                <button
                  onClick={handleConfirm}
                  disabled={confirmMutation.isPending}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Confirm Order
                </button>
              )}

              {order.canProcess && (
                <button
                  onClick={handleProcess}
                  disabled={processMutation.isPending}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Move to Processing
                </button>
              )}

              {order.canComplete && (
                <button
                  onClick={handleComplete}
                  disabled={completeMutation.isPending}
                  className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs rounded-xl shadow-lg transition-colors"
                >
                  Mark Completed
                </button>
              )}

              {order.canCancel && (
                <button
                  onClick={handleCancel}
                  disabled={cancelMutation.isPending}
                  className="px-4 py-2 bg-rose-950/80 hover:bg-rose-900 border border-rose-700/60 text-rose-300 font-medium text-xs rounded-xl transition-colors"
                >
                  Cancel Order
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Future Module Placeholder Notice */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 text-xs text-slate-400 flex items-center gap-3">
          <span className="text-xl">💳</span>
          <div>
            <span className="font-semibold text-slate-300 block">Payment & Logistics Notice</span>
            <span>Payment integration and shipment tracking will be available in future modules. Commercial agreement and order inventory allocation are locked.</span>
          </div>
        </div>

        {/* Detailed Financial & Item Summary */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-lg">
          <h2 className="text-base font-bold text-slate-200 flex items-center gap-2 border-b border-slate-800 pb-3">
            <span>📋 Order Items & Commercial Breakdown</span>
          </h2>

          {order.item && (
            <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-4 space-y-2 text-xs">
              <div className="flex justify-between font-semibold text-slate-200">
                <span>{order.item.cropName} ({order.item.quantity} {order.item.quantityUnit})</span>
                <span>₹{order.item.lineTotal.toLocaleString('en-IN')}</span>
              </div>
              <div className="text-[11px] text-slate-400 flex justify-between">
                <span>Agreed Price per Unit: ₹{order.item.agreedUnitPrice.toLocaleString('en-IN')} / {order.item.priceUnit}</span>
                <span>Line Item ID: {order.item.orderItemId}</span>
              </div>
            </div>
          )}

          <div className="space-y-2 text-xs pt-2 border-t border-slate-800/60">
            <div className="flex justify-between text-slate-400">
              <span>Subtotal:</span>
              <span className="font-mono text-slate-200">₹{order.subtotal.toLocaleString('en-IN')}</span>
            </div>
            <div className="flex justify-between text-slate-400">
              <span>Shipping & Delivery Cost:</span>
              <span className="font-mono text-slate-200">₹{order.shippingCost.toLocaleString('en-IN')} (N/A)</span>
            </div>
            <div className="flex justify-between text-slate-400">
              <span>Other Costs:</span>
              <span className="font-mono text-slate-200">₹{order.otherCost.toLocaleString('en-IN')} (N/A)</span>
            </div>
            <div className="flex justify-between font-bold text-sm text-emerald-300 pt-2 border-t border-slate-800">
              <span>Total Commercial Amount:</span>
              <span className="font-mono">₹{order.totalAmount.toLocaleString('en-IN')} {order.currency}</span>
            </div>
          </div>
        </div>

        {/* Source References & Audit */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 text-xs space-y-2">
            <h3 className="font-bold text-slate-300 border-b border-slate-800 pb-2">Source References</h3>
            <div className="flex justify-between">
              <span className="text-slate-400">Accepted Offer ID:</span>
              <Link to={`/marketplace/offers/${order.offerId}`} className="text-emerald-400 hover:underline font-mono">
                {order.offerId}
              </Link>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Listing ID:</span>
              <Link to={`/marketplace/${order.listingId}`} className="text-emerald-400 hover:underline font-mono">
                {order.listingId}
              </Link>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Buyer Role:</span>
              <span className="font-mono text-slate-200">{order.buyerRole}</span>
            </div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 text-xs space-y-2">
            <h3 className="font-bold text-slate-300 border-b border-slate-800 pb-2">Lifecycle Timeline</h3>
            <div className="flex justify-between">
              <span className="text-slate-400">Created At:</span>
              <span className="text-slate-300">{new Date(order.createdAt).toLocaleString('en-IN')}</span>
            </div>
            {order.confirmedAt && (
              <div className="flex justify-between">
                <span className="text-slate-400">Confirmed At:</span>
                <span className="text-slate-300">{new Date(order.confirmedAt).toLocaleString('en-IN')}</span>
              </div>
            )}
            {order.processedAt && (
              <div className="flex justify-between">
                <span className="text-slate-400">Processed At:</span>
                <span className="text-slate-300">{new Date(order.processedAt).toLocaleString('en-IN')}</span>
              </div>
            )}
            {order.completedAt && (
              <div className="flex justify-between">
                <span className="text-slate-400">Completed At:</span>
                <span className="text-slate-300">{new Date(order.completedAt).toLocaleString('en-IN')}</span>
              </div>
            )}
            {order.cancelledAt && (
              <div className="flex justify-between text-rose-300">
                <span>Cancelled At:</span>
                <span>{new Date(order.cancelledAt).toLocaleString('en-IN')}</span>
              </div>
            )}
            {order.cancellationReason && (
              <div className="pt-2 text-[11px] text-rose-400 border-t border-slate-800">
                Reason: &quot;{order.cancellationReason}&quot;
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
