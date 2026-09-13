import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useOrderDetailQuery } from '../api/marketplaceOrdersApi';
import {
  useCreatePaymentIntentMutation,
  useVerifyPaymentMutation,
} from '../api/marketplacePaymentsApi';
import type { PaymentIntentResponse } from '../../../types/payment';

export const PaymentCheckoutPage: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();

  const { data: order, isLoading: isOrderLoading, isError: isOrderError } = useOrderDetailQuery(orderId || '');

  const createIntentMutation = useCreatePaymentIntentMutation();
  const verifyMutation = useVerifyPaymentMutation();

  const [intent, setIntent] = useState<PaymentIntentResponse | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<'MOCK_CARD' | 'UPI' | 'NET_BANKING'>('MOCK_CARD');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (order && order.status === 'CONFIRMED' && !intent && !createIntentMutation.isPending) {
      createIntentMutation
        .mutateAsync(order.orderId)
        .then((res) => {
          setIntent(res);
        })
        .catch((err) => {
          setErrorMessage(err?.response?.data || err?.message || 'Failed to initialize payment intent');
        });
    }
  }, [order, intent, createIntentMutation]);

  if (isOrderLoading || createIntentMutation.isPending) {
    return (
      <div className="min-h-screen bg-slate-950 text-emerald-400 text-center py-20 text-sm flex flex-col items-center justify-center space-y-4">
        <div className="w-10 h-10 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin" />
        <p>Initializing Secure Payment Session...</p>
      </div>
    );
  }

  if (isOrderError || !order) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
        <div className="max-w-xl mx-auto bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center space-y-4">
          <h2 className="text-lg font-bold text-rose-300">Order Not Found or Ineligible</h2>
          <p className="text-xs text-rose-200">The requested order could not be loaded for checkout.</p>
          <Link to="/marketplace/orders" className="inline-block px-4 py-2 bg-rose-900 text-xs rounded-xl text-white">
            Return to My Orders
          </Link>
        </div>
      </div>
    );
  }

  if (order.status !== 'CONFIRMED') {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
        <div className="max-w-xl mx-auto bg-amber-950/60 border border-amber-800 p-6 rounded-2xl text-center space-y-4">
          <h2 className="text-lg font-bold text-amber-300">Order Not Payable</h2>
          <p className="text-xs text-amber-200">
            Payment is only permitted for <strong>CONFIRMED</strong> orders. Current order status: <strong>{order.status}</strong>.
          </p>
          <Link to={`/marketplace/orders/${order.orderId}`} className="inline-block px-4 py-2 bg-amber-900 text-xs rounded-xl text-white">
            View Order Details
          </Link>
        </div>
      </div>
    );
  }

  const handleSimulatePayment = async (simulatedStatus: 'SUCCESS' | 'FAILED') => {
    if (!intent) return;
    setErrorMessage(null);

    try {
      const verifiedPayment = await verifyMutation.mutateAsync({
        paymentId: intent.paymentId,
        request: {
          providerPaymentId: intent.paymentIntentId,
          paymentMethod,
          simulatedStatus,
          failureReason: simulatedStatus === 'FAILED' ? 'Simulated payment failure by user' : undefined,
        },
      });

      navigate(`/marketplace/payments/${verifiedPayment.paymentId}`);
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: string }; message?: string };
      setErrorMessage(errorResponse?.response?.data || errorResponse?.message || 'Payment verification failed');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-2xl mx-auto space-y-6">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center justify-between">
          <Link
            to={`/marketplace/orders/${order.orderId}`}
            className="text-xs font-medium text-emerald-400 hover:text-emerald-300 transition-colors"
          >
            ← Back to Order
          </Link>
          <span className="text-xs font-mono text-slate-400">Checkout Session</span>
        </div>

        {/* Development / Mock Provider Banner */}
        <div className="bg-amber-950/80 border border-amber-700/80 rounded-2xl p-4 text-xs text-amber-200 flex items-start gap-3 shadow-lg">
          <span className="text-2xl">⚠️</span>
          <div>
            <span className="font-bold text-amber-100 block text-sm">DEVELOPMENT / MOCK PAYMENT ENVIRONMENT</span>
            <span>
              This application is operating with the <strong>MOCK</strong> payment provider. No real monetary transactions or credentials are used.
            </span>
          </div>
        </div>

        {/* Commercial Order Summary */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
          <h2 className="text-base font-bold text-slate-200 border-b border-slate-800 pb-3 flex items-center justify-between">
            <span>🌾 {order.cropName} Order Breakdown</span>
            <span className="text-xs font-normal text-emerald-400 font-mono">ID: {order.orderId}</span>
          </h2>

          <div className="space-y-2 text-xs">
            <div className="flex justify-between text-slate-400">
              <span>Agreed Quantity:</span>
              <span className="text-slate-200 font-semibold">
                {order.totalQuantity} {order.quantityUnit}
              </span>
            </div>
            <div className="flex justify-between text-slate-400">
              <span>Agreed Unit Price:</span>
              <span className="text-slate-200 font-mono">
                ₹{order.agreedPrice.toLocaleString('en-IN')} / {order.priceUnit}
              </span>
            </div>
            <div className="flex justify-between text-slate-400">
              <span>Subtotal:</span>
              <span className="text-slate-200 font-mono">₹{order.subtotal.toLocaleString('en-IN')}</span>
            </div>
            <div className="flex justify-between font-bold text-base text-emerald-300 pt-3 border-t border-slate-800">
              <span>Authoritative Payment Total:</span>
              <span className="font-mono">
                ₹{order.totalAmount.toLocaleString('en-IN')} {order.currency}
              </span>
            </div>
          </div>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="bg-rose-950/80 border border-rose-800 p-4 rounded-xl text-xs text-rose-200">
            <strong>Payment Error:</strong> {errorMessage}
          </div>
        )}

        {/* Payment Method Selector & Actions */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-6">
          <h3 className="text-sm font-bold text-slate-200">Select Mock Payment Method</h3>

          <div className="grid grid-cols-3 gap-3 text-xs">
            <button
              type="button"
              onClick={() => setPaymentMethod('MOCK_CARD')}
              className={`p-3 rounded-xl border font-medium text-center transition-all ${
                paymentMethod === 'MOCK_CARD'
                  ? 'bg-emerald-950/80 border-emerald-500 text-emerald-300 shadow-md'
                  : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
              }`}
            >
              💳 Mock Card
            </button>

            <button
              type="button"
              onClick={() => setPaymentMethod('UPI')}
              className={`p-3 rounded-xl border font-medium text-center transition-all ${
                paymentMethod === 'UPI'
                  ? 'bg-emerald-950/80 border-emerald-500 text-emerald-300 shadow-md'
                  : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
              }`}
            >
              📱 UPI / QR
            </button>

            <button
              type="button"
              onClick={() => setPaymentMethod('NET_BANKING')}
              className={`p-3 rounded-xl border font-medium text-center transition-all ${
                paymentMethod === 'NET_BANKING'
                  ? 'bg-emerald-950/80 border-emerald-500 text-emerald-300 shadow-md'
                  : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
              }`}
            >
              🏛️ Net Banking
            </button>
          </div>

          <div className="pt-4 border-t border-slate-800 space-y-3">
            <button
              type="button"
              onClick={() => handleSimulatePayment('SUCCESS')}
              disabled={verifyMutation.isPending}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-xl shadow-lg transition-colors flex items-center justify-center gap-2"
            >
              {verifyMutation.isPending ? 'Processing Verification...' : `Pay ₹${order.totalAmount.toLocaleString('en-IN')} (Simulate Success)`}
            </button>

            <button
              type="button"
              onClick={() => handleSimulatePayment('FAILED')}
              disabled={verifyMutation.isPending}
              className="w-full py-2 bg-rose-950/60 hover:bg-rose-900 border border-rose-800/80 text-rose-300 font-medium text-xs rounded-xl transition-colors"
            >
              Simulate Payment Failure
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
