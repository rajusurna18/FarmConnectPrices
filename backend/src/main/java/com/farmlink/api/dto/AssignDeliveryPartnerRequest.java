package com.farmlink.api.dto;

public class AssignDeliveryPartnerRequest {
    private String name;
    private String phone;
    private String vehicleType;
    private String vehicleNumber;

    public AssignDeliveryPartnerRequest() {}

    public AssignDeliveryPartnerRequest(String name, String phone, String vehicleType, String vehicleNumber) {
        this.name = name;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
}
