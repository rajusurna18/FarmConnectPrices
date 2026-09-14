import React from 'react';
import type { DeliveryStatus } from '../../../types/delivery';

interface Props {
  status: DeliveryStatus;
  createdAt?: string;
  assignedAt?: string;
  readyForPickupAt?: string;
  pickedUpAt?: string;
  inTransitAt?: string;
  outForDeliveryAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;
}

interface Step {
  key: DeliveryStatus;
  label: string;
  description: string;
  timestamp?: string;
  icon: string;
}

export const DeliveryTimelineComponent: React.FC<Props> = ({
  status,
  createdAt,
  assignedAt,
  readyForPickupAt,
  pickedUpAt,
  inTransitAt,
  outForDeliveryAt,
  deliveredAt,
  cancelledAt,
}) => {
  if (status === 'CANCELLED') {
    return (
      <div className="bg-rose-950/60 border border-rose-800/60 p-5 rounded-2xl text-center space-y-2">
        <div className="text-2xl">🚫</div>
        <h3 className="text-sm font-bold text-rose-300">Delivery Cancelled</h3>
        <p className="text-xs text-rose-200/80">
          This delivery request was cancelled on{' '}
          {cancelledAt ? new Date(cancelledAt).toLocaleString() : 'N/A'}.
        </p>
      </div>
    );
  }

  const steps: Step[] = [
    {
      key: 'CREATED',
      label: 'Request Created',
      description: 'Order paid & delivery requested',
      timestamp: createdAt,
      icon: '📝',
    },
    {
      key: 'ASSIGNED',
      label: 'Partner Assigned',
      description: 'Driver / vehicle allocated',
      timestamp: assignedAt,
      icon: '🚚',
    },
    {
      key: 'READY_FOR_PICKUP',
      label: 'Ready for Pickup',
      description: 'Produce packed at farm',
      timestamp: readyForPickupAt,
      icon: '📦',
    },
    {
      key: 'PICKED_UP',
      label: 'Picked Up',
      description: 'Driver picked up shipment',
      timestamp: pickedUpAt,
      icon: '🌾',
    },
    {
      key: 'IN_TRANSIT',
      label: 'In Transit',
      description: 'En route to destination',
      timestamp: inTransitAt,
      icon: '🚛',
    },
    {
      key: 'OUT_FOR_DELIVERY',
      label: 'Out for Delivery',
      description: 'Arriving today',
      timestamp: outForDeliveryAt,
      icon: '📍',
    },
    {
      key: 'DELIVERED',
      label: 'Delivered',
      description: 'Fulfillment completed',
      timestamp: deliveredAt,
      icon: '✅',
    },
  ];

  const statusOrder: DeliveryStatus[] = [
    'CREATED',
    'ASSIGNED',
    'READY_FOR_PICKUP',
    'PICKED_UP',
    'IN_TRANSIT',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
  ];

  const currentIndex = statusOrder.indexOf(status);

  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold text-slate-200 uppercase tracking-wider flex items-center gap-2">
          <span>📦</span> Delivery Progress Timeline
        </h3>
        <span className="px-3 py-1 bg-emerald-950/80 border border-emerald-700/60 text-emerald-300 text-xs font-semibold rounded-full">
          Status: {status.replace(/_/g, ' ')}
        </span>
      </div>

      <div className="relative">
        {/* Connecting line */}
        <div className="absolute left-4 top-4 bottom-4 w-0.5 bg-slate-800" />

        <div className="space-y-6 relative">
          {steps.map((step, idx) => {
            const isCompleted = idx <= currentIndex;
            const isCurrent = idx === currentIndex;

            return (
              <div key={step.key} className="flex items-start gap-4">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all z-10 ${
                    isCurrent
                      ? 'bg-emerald-500 text-slate-950 ring-4 ring-emerald-500/20 shadow-lg scale-110'
                      : isCompleted
                      ? 'bg-emerald-950 border border-emerald-600 text-emerald-400'
                      : 'bg-slate-950 border border-slate-800 text-slate-600'
                  }`}
                >
                  {step.icon}
                </div>

                <div className="flex-1 pt-1">
                  <div className="flex items-center justify-between">
                    <h4
                      className={`text-xs font-semibold ${
                        isCurrent
                          ? 'text-emerald-300 font-extrabold'
                          : isCompleted
                          ? 'text-slate-200'
                          : 'text-slate-500'
                      }`}
                    >
                      {step.label}
                    </h4>
                    {step.timestamp && (
                      <span className="text-[10px] font-mono text-slate-400">
                        {new Date(step.timestamp).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    )}
                  </div>
                  <p className="text-[11px] text-slate-400 mt-0.5">{step.description}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
