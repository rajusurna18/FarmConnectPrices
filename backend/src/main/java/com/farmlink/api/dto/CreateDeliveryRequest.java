package com.farmlink.api.dto;

public class CreateDeliveryRequest {
    private String orderId;
    private LocationDto deliveryAddress;

    public CreateDeliveryRequest() {}

    public CreateDeliveryRequest(String orderId, LocationDto deliveryAddress) {
        this.orderId = orderId;
        this.deliveryAddress = deliveryAddress;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public LocationDto getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(LocationDto deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}
