package com.turkcell.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class OrderItemRequest {

    @NotBlank(message = "Product code cannot be blank")
    private String productCode;

    @NotBlank(message = "Product type cannot be blank")
    private String productType; // TARIFF, ADDON

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
