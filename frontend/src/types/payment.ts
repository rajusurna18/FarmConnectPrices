export type PaymentStatus =
  | 'CREATED'
  | 'PENDING'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'FAILED'
  | 'CANCELLED'
  | 'EXPIRED';

export interface PaymentAttempt {
  attemptId: string;
  providerPaymentId: string;
  transactionReference?: string | null;
  paymentMethod?: string | null;
  status: string;
  failureCode?: string | null;
  failureMessage?: string | null;
  attemptedAt: string;
  verifiedAt?: string | null;
}

export interface Payment {
  paymentId: string;
  orderId: string;
  buyerUid: string;
  farmerId: string;
  buyerRole: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  provider: string;
  providerPaymentId?: string | null;
  providerOrderId?: string | null;
  transactionReference?: string | null;
  paymentMethod?: string | null;
  failureCode?: string | null;
  failureMessage?: string | null;
  createdAt: string;
  updatedAt: string;
  initiatedAt?: string | null;
  verifiedAt?: string | null;
  failedAt?: string | null;
  expiresAt?: string | null;
}

export interface PaymentIntentResponse {
  paymentIntentId: string;
  paymentId: string;
  orderId: string;
  amount: number;
  currency: string;
  provider: string;
  providerOrderId?: string | null;
  status: PaymentStatus;
  createdAt: string;
  expiresAt?: string | null;
}

export interface VerifyPaymentRequest {
  providerPaymentId?: string;
  providerSignature?: string;
  paymentMethod?: string;
  simulatedStatus?: 'SUCCESS' | 'FAILED';
  failureReason?: string;
}

export interface PaymentPageResponse {
  items: Payment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
