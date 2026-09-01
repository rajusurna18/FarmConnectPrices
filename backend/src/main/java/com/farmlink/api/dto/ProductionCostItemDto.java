package com.farmlink.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductionCostItemDto {
    private String id;
    private String category;
    private String description;
    private BigDecimal amount;
    private String currency = "INR";

    public ProductionCostItemDto() {
        this.id = UUID.randomUUID().toString();
    }

    public ProductionCostItemDto(String id, String category, String description, BigDecimal amount, String currency) {
        this.id = id != null && !id.trim().isEmpty() ? id : UUID.randomUUID().toString();
        this.category = category != null ? category.trim().toUpperCase() : null;
        this.description = description != null ? description.trim() : null;
        this.amount = amount;
        this.currency = currency != null && !currency.trim().isEmpty() ? currency.trim().toUpperCase() : "INR";
    }

    public ProductionCostItemDto(String category, String description, double amount) {
        this(UUID.randomUUID().toString(), category, description, BigDecimal.valueOf(amount), "INR");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null ? category.trim().toUpperCase() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency != null && !currency.trim().isEmpty() ? currency.trim().toUpperCase() : "INR";
    }
}
