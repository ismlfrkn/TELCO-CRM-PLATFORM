package com.turkcell.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String line1;

    @Column(nullable = false)
    private String city;

    @Column
    private String district;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    public Address() {
    }

    public Address(UUID id, Customer customer, String line1, String city, String district, String postalCode, boolean isDefault) {
        this.id = id;
        this.customer = customer;
        this.line1 = line1;
        this.city = city;
        this.district = district;
        this.postalCode = postalCode;
        this.isDefault = isDefault;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

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
