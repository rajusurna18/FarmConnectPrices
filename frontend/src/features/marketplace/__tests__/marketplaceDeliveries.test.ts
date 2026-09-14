import { describe, it, expect } from 'vitest';
import type { Delivery, CreateDeliveryRequest, DeliveryStatus } from '../../../types/delivery';

describe('Marketplace Delivery Foundation Unit Tests', () => {
  it('validates Delivery data model structure & state machine values', () => {
    const validStatuses: DeliveryStatus[] = [
      'CREATED',
      'ASSIGNED',
      'READY_FOR_PICKUP',
      'PICKED_UP',
      'IN_TRANSIT',
      'OUT_FOR_DELIVERY',
      'DELIVERED',
      'CANCELLED',
    ];

    const delivery: Delivery = {
      deliveryId: 'del_order_100',
      orderId: 'order_100',
      farmerId: 'farmer_uid_1',
      buyerUid: 'buyer_uid_1',
      buyerRole: 'CUSTOMER',
      status: 'CREATED',
      pickupAddress: {
        state: 'Telangana',
        district: 'Warangal',
        mandal: 'Enumamula',
        village: 'Farm Village',
        pincode: '506001',
      },
      deliveryAddress: {
        state: 'Telangana',
        district: 'Hyderabad',
        mandal: 'Kukatpally',
        village: 'Destination Village',
        pincode: '500072',
      },
      trackingReference: 'TRK-99887766',
      createdAt: '2026-09-14T10:00:00Z',
      updatedAt: '2026-09-14T10:00:00Z',
      canAssign: true,
      canCancel: true,
    };

    expect(delivery.deliveryId).toBe('del_order_100');
    expect(validStatuses).toContain(delivery.status);
    expect(delivery.pickupAddress?.district).toBe('Warangal');
    expect(delivery.deliveryAddress?.district).toBe('Hyderabad');
    expect(delivery.canAssign).toBe(true);
  });

  it('validates CreateDeliveryRequest payload construction', () => {
    const request: CreateDeliveryRequest = {
      orderId: 'order_200',
      deliveryAddress: {
        state: 'Telangana',
        district: 'Rangareddy',
        mandal: 'Rajendranagar',
        village: 'Buyer Hub',
        pincode: '500030',
      },
    };

    expect(request.orderId).toBe('order_200');
    expect(request.deliveryAddress?.pincode).toBe('500030');
  });

  it('validates state machine progression order', () => {
    const statusOrder: DeliveryStatus[] = [
      'CREATED',
      'ASSIGNED',
      'READY_FOR_PICKUP',
      'PICKED_UP',
      'IN_TRANSIT',
      'OUT_FOR_DELIVERY',
      'DELIVERED',
    ];

    expect(statusOrder.indexOf('CREATED')).toBe(0);
    expect(statusOrder.indexOf('DELIVERED')).toBe(6);
    expect(statusOrder.indexOf('PICKED_UP')).toBeLessThan(statusOrder.indexOf('IN_TRANSIT'));
    expect(statusOrder.indexOf('IN_TRANSIT')).toBeLessThan(statusOrder.indexOf('OUT_FOR_DELIVERY'));
    expect(statusOrder.indexOf('OUT_FOR_DELIVERY')).toBeLessThan(statusOrder.indexOf('DELIVERED'));
  });
});
