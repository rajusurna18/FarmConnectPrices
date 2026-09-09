import { describe, it, expect } from 'vitest';
import type { Offer, OfferRound } from '../../../types/offer';

describe('Marketplace Offers Domain', () => {
  it('correctly constructs an offer object with rounds', () => {
    const round1: OfferRound = {
      roundNumber: 1,
      senderUid: 'buyer_123',
      senderRole: 'CUSTOMER',
      action: 'OFFER',
      quantity: 500,
      quantityUnit: 'KG',
      price: 2200,
      priceUnit: 'QUINTAL',
      message: 'Initial buyer proposal',
      timestamp: '2026-09-08T00:00:00Z',
    };

    const offer: Offer = {
      offerId: 'offer_1',
      listingId: 'listing_1',
      farmerId: 'farmer_456',
      buyerUid: 'buyer_123',
      buyerRole: 'CUSTOMER',
      cropId: 'crop_tomato',
      cropName: 'Tomato',
      offeredQuantity: 500,
      quantityUnit: 'KG',
      offeredPrice: 2200,
      priceUnit: 'QUINTAL',
      askingPriceReference: 2500,
      status: 'PENDING',
      roundNumber: 1,
      currentResponderUid: 'farmer_456',
      rounds: [round1],
      createdAt: '2026-09-08T00:00:00Z',
      updatedAt: '2026-09-08T00:00:00Z',
      canAccept: false,
      canReject: false,
      canCounter: false,
      canCancel: true,
    };

    expect(offer.offerId).toBe('offer_1');
    expect(offer.status).toBe('PENDING');
    expect(offer.rounds.length).toBe(1);
    expect(offer.rounds[0].action).toBe('OFFER');
    expect(offer.askingPriceReference).toBe(2500);
    expect(offer.offeredPrice).toBe(2200);
  });

  it('handles accepted offer state with agreed values', () => {
    const roundAccept: OfferRound = {
      roundNumber: 2,
      senderUid: 'farmer_456',
      senderRole: 'FARMER',
      action: 'ACCEPT',
      quantity: 500,
      quantityUnit: 'KG',
      price: 2200,
      priceUnit: 'QUINTAL',
      message: 'Accepted',
      timestamp: '2026-09-08T01:00:00Z',
    };

    const offer: Offer = {
      offerId: 'offer_1',
      listingId: 'listing_1',
      farmerId: 'farmer_456',
      buyerUid: 'buyer_123',
      buyerRole: 'CUSTOMER',
      cropId: 'crop_tomato',
      cropName: 'Tomato',
      offeredQuantity: 500,
      quantityUnit: 'KG',
      offeredPrice: 2200,
      priceUnit: 'QUINTAL',
      askingPriceReference: 2500,
      status: 'ACCEPTED',
      roundNumber: 2,
      currentResponderUid: 'farmer_456',
      agreedQuantity: 500,
      agreedPrice: 2200,
      agreedPriceUnit: 'QUINTAL',
      rounds: [roundAccept],
      createdAt: '2026-09-08T00:00:00Z',
      updatedAt: '2026-09-08T01:00:00Z',
      canAccept: false,
      canReject: false,
      canCounter: false,
      canCancel: false,
    };

    expect(offer.status).toBe('ACCEPTED');
    expect(offer.agreedPrice).toBe(2200);
    expect(offer.canAccept).toBe(false);
    expect(offer.canCounter).toBe(false);
  });
});
