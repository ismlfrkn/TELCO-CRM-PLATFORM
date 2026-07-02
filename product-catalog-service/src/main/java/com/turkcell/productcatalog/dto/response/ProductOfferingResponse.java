package com.turkcell.productcatalog.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProductOfferingResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private TariffResponse tariff;
    private String status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Instant createdAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TariffResponse getTariff() { return tariff; }
    public void setTariff(TariffResponse tariff) { this.tariff = tariff; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
