export type OfferStatus =
  | 'PENDING'
  | 'COUNTERED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'EXPIRED';

export interface OfferRound {
  roundNumber: number;
  senderUid: string;
  senderRole: string;
  action: 'OFFER' | 'COUNTER' | 'ACCEPT' | 'REJECT' | 'CANCEL';
  quantity: number;
  quantityUnit: string;
  price: number;
  priceUnit: string;
  message?: string | null;
  timestamp: string;
}

export interface Offer {
  offerId: string;
  listingId: string;
  farmerId: string;
  buyerUid: string;
  buyerRole: string;
  cropId: string;
  cropName: string;
  offeredQuantity: number;
  quantityUnit: string;
  offeredPrice: number;
  priceUnit: string;
  askingPriceReference: number;
  message?: string | null;
  status: OfferStatus;
  roundNumber: number;
  currentResponderUid: string;
  agreedQuantity?: number | null;
  agreedPrice?: number | null;
  agreedPriceUnit?: string | null;
  rounds: OfferRound[];
  createdAt: string;
  updatedAt: string;
  expiresAt?: string | null;
  respondedAt?: string | null;
  canAccept: boolean;
  canReject: boolean;
  canCounter: boolean;
  canCancel: boolean;
}

export interface CreateOfferRequest {
  listingId: string;
  offeredQuantity: number;
  quantityUnit?: string;
  offeredPrice: number;
  priceUnit?: string;
  message?: string;
}

export interface CounterOfferRequest {
  counterQuantity: number;
  quantityUnit?: string;
  counterPrice: number;
  priceUnit?: string;
  message?: string;
}

export interface OfferPageResponse {
  items: Offer[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
