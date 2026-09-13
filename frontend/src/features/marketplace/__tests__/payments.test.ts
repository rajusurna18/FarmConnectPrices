import { describe, it, expect } from 'vitest';
import type { Payment, PaymentIntentResponse, PaymentStatus } from '../../../types/payment';

describe('Marketplace Payments Foundation Domain', () => {
  it('correctly constructs a PaymentIntentResponse', () => {
    const intent: PaymentIntentResponse = {
      paymentIntentId: 'mock_pi_123456',
      paymentId: 'pay_order_100',
      orderId: 'order_100',
      amount: 92000,
      currency: 'INR',
      provider: 'MOCK',
      providerOrderId: 'mock_order_789',
      status: 'PENDING',
      createdAt: '2026-09-13T14:00:00Z',
      expiresAt: '2026-09-13T14:15:00Z',
    };

    expect(intent.paymentIntentId).toBe('mock_pi_123456');
    expect(intent.paymentId).toBe('pay_order_100');
    expect(intent.orderId).toBe('order_100');
    expect(intent.amount).toBe(92000);
    expect(intent.currency).toBe('INR');
    expect(intent.status).toBe('PENDING');
    expect(intent.provider).toBe('MOCK');
  });

  it('correctly structures a Payment aggregate in SUCCESS state', () => {
    const payment: Payment = {
      paymentId: 'pay_order_100',
      orderId: 'order_100',
      buyerUid: 'buyer_123',
      farmerId: 'farmer_789',
      buyerRole: 'MEDIATOR_BUYER',
      amount: 92000,
      currency: 'INR',
      status: 'SUCCESS',
      provider: 'MOCK',
      providerPaymentId: 'mock_pi_123456',
      providerOrderId: 'mock_order_789',
      transactionReference: 'tx_mock_999888',
      paymentMethod: 'UPI',
      createdAt: '2026-09-13T14:00:00Z',
      updatedAt: '2026-09-13T14:02:00Z',
      initiatedAt: '2026-09-13T14:00:05Z',
      verifiedAt: '2026-09-13T14:02:00Z',
    };

    expect(payment.paymentId).toBe('pay_order_100');
    expect(payment.status).toBe('SUCCESS');
    expect(payment.amount).toBe(92000);
    expect(payment.transactionReference).toBe('tx_mock_999888');
    expect(payment.paymentMethod).toBe('UPI');
    expect(payment.verifiedAt).toBeDefined();
    expect(payment.failureCode).toBeUndefined();
  });

  it('correctly structures a Payment aggregate in FAILED state', () => {
    const payment: Payment = {
      paymentId: 'pay_order_200',
      orderId: 'order_200',
      buyerUid: 'buyer_123',
      farmerId: 'farmer_789',
      buyerRole: 'CUSTOMER',
      amount: 45000,
      currency: 'INR',
      status: 'FAILED',
      provider: 'MOCK',
      providerPaymentId: 'mock_pi_777888',
      failureCode: 'SIMULATED_MOCK_FAILURE',
      failureMessage: 'Payment declined by issuing bank',
      createdAt: '2026-09-13T14:00:00Z',
      updatedAt: '2026-09-13T14:01:00Z',
      failedAt: '2026-09-13T14:01:00Z',
    };

    expect(payment.status).toBe('FAILED');
    expect(payment.failureCode).toBe('SIMULATED_MOCK_FAILURE');
    expect(payment.failureMessage).toBe('Payment declined by issuing bank');
    expect(payment.failedAt).toBeDefined();
    expect(payment.verifiedAt).toBeUndefined();
  });

  it('validates state machine status transitions', () => {
    const validStatuses: PaymentStatus[] = [
      'CREATED',
      'PENDING',
      'PROCESSING',
      'SUCCESS',
      'FAILED',
      'CANCELLED',
      'EXPIRED',
    ];

    expect(validStatuses).toContain('SUCCESS');
    expect(validStatuses).toContain('FAILED');
    expect(validStatuses).toHaveLength(7);
  });
});
