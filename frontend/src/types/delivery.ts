export type DeliveryStatus =
  | 'CREATED'
  | 'ASSIGNED'
  | 'READY_FOR_PICKUP'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

export interface LocationDto {
  state?: string;
  district?: string;
  mandal?: string;
  village?: string;
  pincode?: string;
}

export interface DeliveryPartnerInfo {
  partnerId?: string;
  name: string;
  phone?: string;
  vehicleType?: string;
  vehicleNumber?: string;
  status?: string;
}

export interface Delivery {
  deliveryId: string;
  orderId: string;
  farmerId: string;
  buyerUid: string;
  buyerRole: string;

  status: DeliveryStatus;

  pickupAddress?: LocationDto;
  pickupLatitude?: number;
  pickupLongitude?: number;

  deliveryAddress?: LocationDto;
  deliveryLatitude?: number;
  deliveryLongitude?: number;

  partner?: DeliveryPartnerInfo;
  trackingReference?: string;

  cancellationReason?: string;
  cancelledByUid?: string;

  createdAt: string;
  updatedAt: string;
  assignedAt?: string;
  readyForPickupAt?: string;
  pickedUpAt?: string;
  inTransitAt?: string;
  outForDeliveryAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;

  canAssign?: boolean;
  canMarkReady?: boolean;
  canPickup?: boolean;
  canInTransit?: boolean;
  canOutForDelivery?: boolean;
  canDeliver?: boolean;
  canCancel?: boolean;
}

export interface CreateDeliveryRequest {
  orderId: string;
  deliveryAddress?: LocationDto;
}

export interface AssignDeliveryPartnerRequest {
  name: string;
  phone?: string;
  vehicleType?: string;
  vehicleNumber?: string;
}

export interface CancelDeliveryRequest {
  reason?: string;
}

export interface DeliveryPageResponse {
  items: Delivery[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
