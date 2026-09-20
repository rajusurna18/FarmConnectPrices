export type NotificationType =
  | 'NEW_OFFER'
  | 'COUNTER_OFFER_RECEIVED'
  | 'OFFER_ACCEPTED'
  | 'OFFER_REJECTED'
  | 'OFFER_CANCELLED'
  | 'ORDER_CREATED'
  | 'ORDER_CONFIRMED'
  | 'ORDER_PROCESSING'
  | 'ORDER_CANCELLED'
  | 'ORDER_COMPLETED'
  | 'PAYMENT_CREATED'
  | 'PAYMENT_SUCCESS'
  | 'PAYMENT_FAILED'
  | 'DELIVERY_ASSIGNED'
  | 'DELIVERY_READY_FOR_PICKUP'
  | 'DELIVERY_PICKED_UP'
  | 'DELIVERY_IN_TRANSIT'
  | 'DELIVERY_OUT_FOR_DELIVERY'
  | 'DELIVERY_DELIVERED'
  | 'DELIVERY_CANCELLED'
  | 'NEW_REVIEW';

export interface NotificationItem {
  notificationId: string;
  recipientUid: string;
  recipientRole: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  relatedEntityType?: string;
  relatedEntityId?: string;
  relatedRoute?: string;
  metadata?: Record<string, unknown>;
  sourceEventId?: string;
  createdAt: string;
  readAt?: string | null;
}

export interface NotificationListResponse {
  notifications: NotificationItem[];
  totalCount: number;
  unreadCount: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

export interface UnreadCountResponse {
  count: number;
}
