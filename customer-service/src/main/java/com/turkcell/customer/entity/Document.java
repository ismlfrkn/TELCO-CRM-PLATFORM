package com.turkcell.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String type; // ID_CARD, PASSPORT

    @Column(name = "file_ref", nullable = false)
    private String fileRef;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public Document() {
    }

    public Document(UUID id, Customer customer, String type, String fileRef, Instant verifiedAt) {
        this.id = id;
        this.customer = customer;
        this.type = type;
        this.fileRef = fileRef;
        this.verifiedAt = verifiedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFileRef() { return fileRef; }
    public void setFileRef(String fileRef) { this.fileRef = fileRef; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}
