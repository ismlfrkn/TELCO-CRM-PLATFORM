package com.turkcell.customer.dto.response;

import java.util.UUID;

public class AddressResponse {

    private UUID id;
    private String line1;
    private String city;
    private String district;
    private String postalCode;
    private boolean isDefault;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}
