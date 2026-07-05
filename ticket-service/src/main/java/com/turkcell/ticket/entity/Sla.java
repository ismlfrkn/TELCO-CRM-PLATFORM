package com.turkcell.ticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "slas")
public class Sla {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String priority;

    @Column(name = "response_time", nullable = false)
    private int responseTime; // dakika

    @Column(name = "resolution_time", nullable = false)
    private int resolutionTime; // dakika

    public Sla() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getResponseTime() { return responseTime; }
    public void setResponseTime(int responseTime) { this.responseTime = responseTime; }

    public int getResolutionTime() { return resolutionTime; }
    public void setResolutionTime(int resolutionTime) { this.resolutionTime = resolutionTime; }
}
