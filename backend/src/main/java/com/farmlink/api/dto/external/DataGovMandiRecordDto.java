package com.farmlink.api.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataGovMandiRecordDto {

    @JsonProperty("state")
    @JsonAlias({"State", "state"})
    private String state;

    @JsonProperty("district")
    @JsonAlias({"District", "district"})
    private String district;

    @JsonProperty("market")
    @JsonAlias({"Market", "market"})
    private String market;

    @JsonProperty("commodity")
    @JsonAlias({"Commodity", "commodity"})
    private String commodity;

    @JsonProperty("variety")
    @JsonAlias({"Variety", "variety"})
    private String variety;

    @JsonProperty("arrival_date")
    @JsonAlias({"Arrival_Date", "arrival_date", "arrivalDate", "Date"})
    private String arrivalDate;

    @JsonProperty("min_price")
    @JsonAlias({"Min_Price", "min_price", "minPrice"})
    private String minPrice;

    @JsonProperty("max_price")
    @JsonAlias({"Max_Price", "max_price", "maxPrice"})
    private String maxPrice;

    @JsonProperty("modal_price")
    @JsonAlias({"Modal_Price", "modal_price", "modalPrice"})
    private String modalPrice;

    public DataGovMandiRecordDto() {
    }

    public DataGovMandiRecordDto(String state, String district, String market, String commodity,
                               String variety, String arrivalDate, String minPrice,
                               String maxPrice, String modalPrice) {
        this.state = state;
        this.district = district;
        this.market = market;
        this.commodity = commodity;
        this.variety = variety;
        this.arrivalDate = arrivalDate;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.modalPrice = modalPrice;
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

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(String arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public String getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(String minPrice) {
        this.minPrice = minPrice;
    }

    public String getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(String maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getModalPrice() {
        return modalPrice;
    }

    public void setModalPrice(String modalPrice) {
        this.modalPrice = modalPrice;
    }
}
