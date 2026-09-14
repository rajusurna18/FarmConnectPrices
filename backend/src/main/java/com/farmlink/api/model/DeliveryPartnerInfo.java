package com.farmlink.api.model;

/**
 * Domain entity representing internal delivery partner / driver details assigned to a delivery.
 */
public class DeliveryPartnerInfo {
    private String partnerId;
    private String name;
    private String phone;
    private String vehicleType;  // e.g., "TRUCK", "VAN", "TRACTOR", "THREE_WHEELER"
    private String vehicleNumber;
    private String status;       // "ASSIGNED", "AVAILABLE", "INACTIVE"

    public DeliveryPartnerInfo() {}

    public DeliveryPartnerInfo(String partnerId, String name, String phone, String vehicleType, String vehicleNumber, String status) {
        this.partnerId = partnerId;
        this.name = name;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.status = status;
    }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
