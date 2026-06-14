package com.coffee.platform.customer;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "address_label")
    private String addressLabel;

    @Column(name = "address_line")
    private String addressLine;

    private Double latitude;

    private Double longitude;

    // location column is DB-generated (GEOGRAPHY), not mapped

    @Column(name = "loyalty_score", nullable = false)
    private int loyaltyScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected Customer() {}

    public Customer(String mobileNumber, String name) {
        this.mobileNumber = mobileNumber;
        this.name = name;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }

    public String getMobileNumber() { return mobileNumber; }

    public String getName() { return name; }

    public String getAddressLabel() { return addressLabel; }

    public String getAddressLine() { return addressLine; }

    public Double getLatitude() { return latitude; }

    public Double getLongitude() { return longitude; }

    public int getLoyaltyScore() { return loyaltyScore; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }

    public void setAddressLabel(String addressLabel) { this.addressLabel = addressLabel; }

    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
