import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  useDeliveryDetailQuery,
  useAssignPartnerMutation,
  useUpdateDeliveryStatusMutation,
  useCancelDeliveryMutation,
} from '../api/marketplaceDeliveriesApi';
import { DeliveryTimelineComponent } from '../components/DeliveryTimelineComponent';

export const DeliveryDetailPage: React.FC = () => {
  const { deliveryId } = useParams<{ deliveryId: string }>();

  const { data: delivery, isLoading, isError, refetch } = useDeliveryDetailQuery(deliveryId || '');

  const assignMutation = useAssignPartnerMutation();
  const updateStatusMutation = useUpdateDeliveryStatusMutation();
  const cancelMutation = useCancelDeliveryMutation();

  const [partnerName, setPartnerName] = useState('');
  const [partnerPhone, setPartnerPhone] = useState('');
  const [vehicleType, setVehicleType] = useState('TRUCK');
  const [vehicleNumber, setVehicleNumber] = useState('');
  const [showAssignModal, setShowAssignModal] = useState(false);

  if (isLoading) {
    return <div className="min-h-screen bg-slate-950 text-emerald-400 text-center py-16 text-sm">Loading delivery details...</div>;
  }

  if (isError || !delivery) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
        <div className="max-w-xl mx-auto bg-rose-950/60 border border-rose-800 p-6 rounded-2xl text-center space-y-4">
          <h2 className="text-lg font-bold text-rose-300">Delivery Not Found</h2>
          <p className="text-xs text-rose-200">The delivery record could not be loaded or you are not authorized to view it.</p>
          <Link to="/marketplace/deliveries" className="inline-block px-4 py-2 bg-rose-900 text-xs rounded-xl text-white font-medium">
            Return to My Deliveries
          </Link>
        </div>
      </div>
    );
  }

  const handleAssignPartner = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!partnerName.trim()) {
      alert('Partner name is required.');
      return;
    }

    await assignMutation.mutateAsync({
      deliveryId: delivery.deliveryId,
      request: {
        name: partnerName,
        phone: partnerPhone,
        vehicleType,
        vehicleNumber,
      },
    });

    setShowAssignModal(false);
    refetch();
  };

  const handleStatusUpdate = async (action: 'ready' | 'pickup' | 'in-transit' | 'out-for-delivery' | 'delivered') => {
    if (window.confirm(`Are you sure you want to update status to ${action.replace(/-/g, ' ').toUpperCase()}?`)) {
      await updateStatusMutation.mutateAsync({ deliveryId: delivery.deliveryId, action });
      refetch();
    }
  };

  const handleCancel = async () => {
    const reason = window.prompt('Enter cancellation reason:');
    if (reason !== null) {
      await cancelMutation.mutateAsync({ deliveryId: delivery.deliveryId, reason });
      refetch();
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center justify-between">
          <Link
            to="/marketplace/deliveries"
            className="text-xs font-medium text-emerald-400 hover:text-emerald-300 transition-colors flex items-center gap-1"
          >
            ← Back to Deliveries
          </Link>
          <span className="text-xs font-mono text-slate-400">Delivery ID: {delivery.deliveryId}</span>
        </div>

        {/* Delivery Header Card */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <span className="text-xs text-emerald-400 font-mono font-bold block">
                Tracking Reference: {delivery.trackingReference || 'TRK-PENDING'}
              </span>
              <h1 className="text-2xl font-bold text-slate-100 mt-1 flex items-center gap-2">
                <span>🚚 Shipment Tracking</span>
              </h1>
              <p className="text-xs text-slate-400 mt-1">
                Order ID:{' '}
                <Link to={`/marketplace/orders/${delivery.orderId}`} className="text-emerald-400 font-mono underline hover:text-emerald-300">
                  {delivery.orderId}
                </Link>
              </p>
            </div>

            <div className="text-right">
              <span className="text-xs text-slate-400 block">Created On</span>
              <span className="text-xs font-mono text-slate-200">
                {new Date(delivery.createdAt).toLocaleDateString()}
              </span>
            </div>
          </div>

          {/* Actions Toolbar */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-slate-800">
            <div className="text-xs text-slate-400">
              Current Status: <strong className="text-emerald-300">{delivery.status.replace(/_/g, ' ')}</strong>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {delivery.canAssign && (
                <button
                  onClick={() => setShowAssignModal(true)}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Assign Delivery Partner
                </button>
              )}

              {delivery.canMarkReady && (
                <button
                  onClick={() => handleStatusUpdate('ready')}
                  disabled={updateStatusMutation.isPending}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Mark Ready for Pickup
                </button>
              )}

              {delivery.canPickup && (
                <button
                  onClick={() => handleStatusUpdate('pickup')}
                  disabled={updateStatusMutation.isPending}
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Mark Picked Up
                </button>
              )}

              {delivery.canInTransit && (
                <button
                  onClick={() => handleStatusUpdate('in-transit')}
                  disabled={updateStatusMutation.isPending}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Mark In Transit
                </button>
              )}

              {delivery.canOutForDelivery && (
                <button
                  onClick={() => handleStatusUpdate('out-for-delivery')}
                  disabled={updateStatusMutation.isPending}
                  className="px-4 py-2 bg-teal-600 hover:bg-teal-500 text-white font-medium text-xs rounded-xl shadow-lg transition-colors"
                >
                  Mark Out for Delivery
                </button>
              )}

              {delivery.canDeliver && (
                <button
                  onClick={() => handleStatusUpdate('delivered')}
                  disabled={updateStatusMutation.isPending}
                  className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-extrabold text-xs rounded-xl shadow-lg transition-colors"
                >
                  Confirm Delivered & Complete
                </button>
              )}

              {delivery.canCancel && (
                <button
                  onClick={handleCancel}
                  disabled={cancelMutation.isPending}
                  className="px-3 py-2 bg-rose-950/80 border border-rose-800 text-rose-300 hover:bg-rose-900 text-xs font-medium rounded-xl transition-colors"
                >
                  Cancel Delivery
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Timeline Stepper Component */}
        <DeliveryTimelineComponent
          status={delivery.status}
          createdAt={delivery.createdAt}
          assignedAt={delivery.assignedAt}
          readyForPickupAt={delivery.readyForPickupAt}
          pickedUpAt={delivery.pickedUpAt}
          inTransitAt={delivery.inTransitAt}
          outForDeliveryAt={delivery.outForDeliveryAt}
          deliveredAt={delivery.deliveredAt}
          cancelledAt={delivery.cancelledAt}
        />

        {/* Addresses & Partner Details Cards */}
        <div className="grid md:grid-cols-2 gap-6">
          {/* Pickup Address */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 space-y-2">
            <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
              <span>🌾</span> Pickup Location (Farm)
            </h3>
            {delivery.pickupAddress ? (
              <div className="text-xs text-slate-300 space-y-0.5 font-sans">
                <p>{delivery.pickupAddress.village || 'Village Center'}</p>
                <p>{delivery.pickupAddress.mandal || ''}, {delivery.pickupAddress.district || ''}</p>
                <p>{delivery.pickupAddress.state || ''} - {delivery.pickupAddress.pincode || ''}</p>
              </div>
            ) : (
              <p className="text-xs text-slate-500">Farm Location on record</p>
            )}
          </div>

          {/* Delivery Destination Address */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 space-y-2">
            <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
              <span>📍</span> Delivery Destination
            </h3>
            {delivery.deliveryAddress ? (
              <div className="text-xs text-slate-300 space-y-0.5 font-sans">
                <p>{delivery.deliveryAddress.village || 'Buyer Destination'}</p>
                <p>{delivery.deliveryAddress.mandal || ''}, {delivery.deliveryAddress.district || ''}</p>
                <p>{delivery.deliveryAddress.state || ''} - {delivery.deliveryAddress.pincode || ''}</p>
              </div>
            ) : (
              <p className="text-xs text-slate-500">Destination Location on record</p>
            )}
          </div>
        </div>

        {/* Partner Details Card */}
        {delivery.partner && (
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 space-y-3">
            <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
              <span>🚚</span> Assigned Logistics Partner
            </h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs">
              <div>
                <span className="text-slate-500 block text-[11px]">Partner Name</span>
                <strong className="text-slate-200">{delivery.partner.name}</strong>
              </div>
              <div>
                <span className="text-slate-500 block text-[11px]">Phone</span>
                <strong className="text-slate-200">{delivery.partner.phone || 'N/A'}</strong>
              </div>
              <div>
                <span className="text-slate-500 block text-[11px]">Vehicle Type</span>
                <strong className="text-slate-200">{delivery.partner.vehicleType}</strong>
              </div>
              <div>
                <span className="text-slate-500 block text-[11px]">Vehicle Number</span>
                <strong className="text-slate-200 font-mono">{delivery.partner.vehicleNumber || 'N/A'}</strong>
              </div>
            </div>
          </div>
        )}

        {/* Modal for Assigning Partner */}
        {showAssignModal && (
          <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <div className="bg-slate-900 border border-slate-800 max-w-md w-full rounded-2xl p-6 space-y-4 shadow-2xl">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-slate-100">Assign Delivery Partner</h3>
                <button onClick={() => setShowAssignModal(false)} className="text-slate-400 hover:text-slate-200 text-xs">
                  ✕
                </button>
              </div>

              <form onSubmit={handleAssignPartner} className="space-y-4">
                <div>
                  <label className="text-xs font-medium text-slate-300 block mb-1">Partner / Driver Name *</label>
                  <input
                    type="text"
                    required
                    value={partnerName}
                    onChange={(e) => setPartnerName(e.target.value)}
                    placeholder="e.g. Ramesh Transport"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-emerald-500"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-slate-300 block mb-1">Phone Number</label>
                  <input
                    type="text"
                    value={partnerPhone}
                    onChange={(e) => setPartnerPhone(e.target.value)}
                    placeholder="e.g. +91 9876543210"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-emerald-500"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-300 block mb-1">Vehicle Type</label>
                    <select
                      value={vehicleType}
                      onChange={(e) => setVehicleType(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-emerald-500"
                    >
                      <option value="TRUCK">Truck</option>
                      <option value="VAN">Mini Van</option>
                      <option value="THREE_WHEELER">Auto / Three Wheeler</option>
                      <option value="TRACTOR">Tractor</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-xs font-medium text-slate-300 block mb-1">Vehicle Number</label>
                    <input
                      type="text"
                      value={vehicleNumber}
                      onChange={(e) => setVehicleNumber(e.target.value)}
                      placeholder="e.g. TS07AB1234"
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-emerald-500"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowAssignModal(false)}
                    className="px-4 py-2 bg-slate-800 text-slate-300 text-xs font-medium rounded-xl hover:bg-slate-700"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={assignMutation.isPending}
                    className="px-4 py-2 bg-emerald-600 text-white text-xs font-medium rounded-xl hover:bg-emerald-500 shadow-lg"
                  >
                    {assignMutation.isPending ? 'Assigning...' : 'Assign Partner'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
