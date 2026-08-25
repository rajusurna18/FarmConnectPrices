package com.farmlink.api.dto;

import java.util.Objects;

public class MarketSummaryResponse {
    private String id;
    private String name;
    private String code;
    private String type;
    private String state;
    private String district;
    private String mandal;
    private String status;
    private int supportedCropCount;

    public MarketSummaryResponse() {
    }

    public MarketSummaryResponse(String id, String name, String code, String type,
                                 String state, String district, String mandal,
                                 String status, int supportedCropCount) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
        this.state = state;
        this.district = district;
        this.mandal = mandal;
        this.status = status;
        this.supportedCropCount = supportedCropCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getMandal() {
        return mandal;
    }

    public void setMandal(String mandal) {
        this.mandal = mandal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSupportedCropCount() {
        return supportedCropCount;
    }

    public void setSupportedCropCount(int supportedCropCount) {
        this.supportedCropCount = supportedCropCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketSummaryResponse that = (MarketSummaryResponse) o;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
