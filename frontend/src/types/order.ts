export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED';

export interface OrderItem {
  orderItemId: string;
  orderId: string;
  listingId: string;
  cropId: string;
  cropName: string;
  quantity: number;
  quantityUnit: string;
  agreedUnitPrice: number;
  priceUnit: string;
  lineTotal: number;
}

export interface Order {
  orderId: string;
  offerId: string;
  listingId: string;
  farmerId: string;
  buyerUid: string;
  buyerRole: string;
  cropId: string;
  cropName: string;
  status: OrderStatus;
  totalQuantity: number;
  quantityUnit: string;
  agreedPrice: number;
  priceUnit: string;
  subtotal: number;
  shippingCost: number;
  otherCost: number;
  totalAmount: number;
  currency: string;
  item?: OrderItem | null;
  cancellationReason?: string | null;
  cancelledByUid?: string | null;
  createdAt: string;
  updatedAt: string;
  confirmedAt?: string | null;
  processedAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  canConfirm: boolean;
  canProcess: boolean;
  canComplete: boolean;
  canCancel: boolean;
}

export interface CancelOrderRequest {
  reason?: string;
}

export interface OrderPageResponse {
  items: Order[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
