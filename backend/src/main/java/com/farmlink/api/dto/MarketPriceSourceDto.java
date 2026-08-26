package com.farmlink.api.dto;

public class MarketPriceSourceDto {
    private String type;
    private String name;
    private String reference;

    public MarketPriceSourceDto() {}

    public MarketPriceSourceDto(String type, String name, String reference) {
        this.type = type;
        this.name = name;
        this.reference = reference;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
