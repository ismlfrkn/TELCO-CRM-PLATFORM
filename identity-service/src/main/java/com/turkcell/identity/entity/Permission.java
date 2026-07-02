package com.turkcell.identity.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "permissions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(length = 1000)
    private String description;

    public Permission() {
    }

    public Permission(UUID id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
