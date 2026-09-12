import { describe, it, expect } from 'vitest';
import type { Order, OrderItem } from '../../../types/order';

describe('Marketplace Orders Domain', () => {
  it('correctly constructs an order object with embedded order item', () => {
    const item: OrderItem = {
      orderItemId: 'item_100',
      orderId: 'order_100',
      listingId: 'listing_abc',
      cropId: 'crop_paddy',
      cropName: 'Paddy / Rice',
      quantity: 40,
      quantityUnit: 'QUINTAL',
      agreedUnitPrice: 2300,
      priceUnit: 'QUINTAL',
      lineTotal: 92000,
    };

    const order: Order = {
      orderId: 'order_100',
      offerId: 'offer_accepted_1',
      listingId: 'listing_abc',
      farmerId: 'farmer_789',
      buyerUid: 'buyer_123',
      buyerRole: 'MEDIATOR_BUYER',
      cropId: 'crop_paddy',
      cropName: 'Paddy / Rice',
      status: 'PENDING',
      totalQuantity: 40,
      quantityUnit: 'QUINTAL',
      agreedPrice: 2300,
      priceUnit: 'QUINTAL',
      subtotal: 92000,
      shippingCost: 0,
      otherCost: 0,
      totalAmount: 92000,
      currency: 'INR',
      item,
      createdAt: '2026-09-12T10:00:00Z',
      updatedAt: '2026-09-12T10:00:00Z',
      canConfirm: true,
      canProcess: false,
      canComplete: false,
      canCancel: true,
    };

    expect(order.orderId).toBe('order_100');
    expect(order.offerId).toBe('offer_accepted_1');
    expect(order.status).toBe('PENDING');
    expect(order.totalAmount).toBe(92000);
    expect(order.subtotal).toBe(92000);
    expect(order.shippingCost).toBe(0);
    expect(order.item?.lineTotal).toBe(92000);
    expect(order.canConfirm).toBe(true);
    expect(order.canCancel).toBe(true);
  });

  it('handles state transition flags correctly for farmer and buyer roles', () => {
    const orderConfirmed: Order = {
      orderId: 'order_200',
      offerId: 'offer_accepted_2',
      listingId: 'listing_xyz',
      farmerId: 'farmer_789',
      buyerUid: 'buyer_123',
      buyerRole: 'CUSTOMER',
      cropId: 'crop_paddy',
      cropName: 'Paddy',
      status: 'CONFIRMED',
      totalQuantity: 20,
      quantityUnit: 'QUINTAL',
      agreedPrice: 2500,
      priceUnit: 'QUINTAL',
      subtotal: 50000,
      shippingCost: 0,
      otherCost: 0,
      totalAmount: 50000,
      currency: 'INR',
      createdAt: '2026-09-12T11:00:00Z',
      updatedAt: '2026-09-12T11:05:00Z',
      confirmedAt: '2026-09-12T11:05:00Z',
      canConfirm: false,
      canProcess: true,
      canComplete: false,
      canCancel: true,
    };

    expect(orderConfirmed.status).toBe('CONFIRMED');
    expect(orderConfirmed.canProcess).toBe(true);
    expect(orderConfirmed.canConfirm).toBe(false);
  });
});
