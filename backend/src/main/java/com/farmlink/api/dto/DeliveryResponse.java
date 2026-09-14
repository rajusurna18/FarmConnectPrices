package com.farmlink.api.dto;

import com.farmlink.api.model.DeliveryPartnerInfo;

public class DeliveryResponse {
    private String deliveryId;
    private String orderId;
    private String farmerId;
    private String buyerUid;
    private String buyerRole;

    private String status;

    private LocationDto pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;

    private LocationDto deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;

    private DeliveryPartnerInfo partner;
    private String trackingReference;

    private String cancellationReason;
    private String cancelledByUid;

    private String createdAt;
    private String updatedAt;
    private String assignedAt;
    private String readyForPickupAt;
    private String pickedUpAt;
    private String inTransitAt;
    private String outForDeliveryAt;
    private String deliveredAt;
    private String cancelledAt;

    private boolean canAssign;
    private boolean canMarkReady;
    private boolean canPickup;
    private boolean canInTransit;
    private boolean canOutForDelivery;
    private boolean canDeliver;
    private boolean canCancel;

    public DeliveryResponse() {}

    public DeliveryResponse(String deliveryId, String orderId, String farmerId, String buyerUid, String buyerRole,
                            String status, LocationDto pickupAddress, Double pickupLatitude, Double pickupLongitude,
                            LocationDto deliveryAddress, Double deliveryLatitude, Double deliveryLongitude,
                            DeliveryPartnerInfo partner, String trackingReference, String cancellationReason,
                            String cancelledByUid, String createdAt, String updatedAt, String assignedAt,
                            String readyForPickupAt, String pickedUpAt, String inTransitAt, String outForDeliveryAt,
                            String deliveredAt, String cancelledAt, boolean canAssign, boolean canMarkReady,
                            boolean canPickup, boolean canInTransit, boolean canOutForDelivery, boolean canDeliver,
                            boolean canCancel) {
        this.deliveryId = deliveryId;
        this.orderId = orderId;
        this.farmerId = farmerId;
        this.buyerUid = buyerUid;
        this.buyerRole = buyerRole;
        this.status = status;
        this.pickupAddress = pickupAddress;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.deliveryAddress = deliveryAddress;
        this.deliveryLatitude = deliveryLatitude;
        this.deliveryLongitude = deliveryLongitude;
        this.partner = partner;
        this.trackingReference = trackingReference;
        this.cancellationReason = cancellationReason;
        this.cancelledByUid = cancelledByUid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.assignedAt = assignedAt;
        this.readyForPickupAt = readyForPickupAt;
        this.pickedUpAt = pickedUpAt;
        this.inTransitAt = inTransitAt;
        this.outForDeliveryAt = outForDeliveryAt;
        this.deliveredAt = deliveredAt;
        this.cancelledAt = cancelledAt;
        this.canAssign = canAssign;
        this.canMarkReady = canMarkReady;
        this.canPickup = canPickup;
        this.canInTransit = canInTransit;
        this.canOutForDelivery = canOutForDelivery;
        this.canDeliver = canDeliver;
        this.canCancel = canCancel;
    }

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public String getBuyerUid() { return buyerUid; }
    public void setBuyerUid(String buyerUid) { this.buyerUid = buyerUid; }

    public String getBuyerRole() { return buyerRole; }
    public void setBuyerRole(String buyerRole) { this.buyerRole = buyerRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocationDto getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(LocationDto pickupAddress) { this.pickupAddress = pickupAddress; }

    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }

    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public LocationDto getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(LocationDto deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public Double getDeliveryLatitude() { return deliveryLatitude; }
    public void setDeliveryLatitude(Double deliveryLatitude) { this.deliveryLatitude = deliveryLatitude; }

    public Double getDeliveryLongitude() { return deliveryLongitude; }
    public void setDeliveryLongitude(Double deliveryLongitude) { this.deliveryLongitude = deliveryLongitude; }

    public DeliveryPartnerInfo getPartner() { return partner; }
    public void setPartner(DeliveryPartnerInfo partner) { this.partner = partner; }

    public String getTrackingReference() { return trackingReference; }
    public void setTrackingReference(String trackingReference) { this.trackingReference = trackingReference; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getCancelledByUid() { return cancelledByUid; }
    public void setCancelledByUid(String cancelledByUid) { this.cancelledByUid = cancelledByUid; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }

    public String getReadyForPickupAt() { return readyForPickupAt; }
    public void setReadyForPickupAt(String readyForPickupAt) { this.readyForPickupAt = readyForPickupAt; }

    public String getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(String pickedUpAt) { this.pickedUpAt = pickedUpAt; }

    public String getInTransitAt() { return inTransitAt; }
    public void setInTransitAt(String inTransitAt) { this.inTransitAt = inTransitAt; }

    public String getOutForDeliveryAt() { return outForDeliveryAt; }
    public void setOutForDeliveryAt(String outForDeliveryAt) { this.outForDeliveryAt = outForDeliveryAt; }

    public String getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(String deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }

    public boolean isCanAssign() { return canAssign; }
    public void setCanAssign(boolean canAssign) { this.canAssign = canAssign; }

    public boolean isCanMarkReady() { return canMarkReady; }
    public void setCanMarkReady(boolean canMarkReady) { this.canMarkReady = canMarkReady; }

    public boolean isCanPickup() { return canPickup; }
    public void setCanPickup(boolean canPickup) { this.canPickup = canPickup; }

    public boolean isCanInTransit() { return canInTransit; }
    public void setCanInTransit(boolean canInTransit) { this.canInTransit = canInTransit; }

    public boolean isCanOutForDelivery() { return canOutForDelivery; }
    public void setCanOutForDelivery(boolean canOutForDelivery) { this.canOutForDelivery = canOutForDelivery; }

    public boolean isCanDeliver() { return canDeliver; }
    public void setCanDeliver(boolean canDeliver) { this.canDeliver = canDeliver; }

    public boolean isCanCancel() { return canCancel; }
    public void setCanCancel(boolean canCancel) { this.canCancel = canCancel; }
}
