package com.coffee.platform.order;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cust_order")
public class CustOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_minor", nullable = false)
    private long totalMinor;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "placed_at", nullable = false)
    private OffsetDateTime placedAt;

    @Column(name = "fulfilled_at")
    private OffsetDateTime fulfilledAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    protected CustOrder() {}

    public CustOrder(Long customerId, Long shopId, OrderStatus status, long totalMinor,
                     String idempotencyKey, OffsetDateTime placedAt) {
        this.customerId = customerId;
        this.shopId = shopId;
        this.status = status;
        this.totalMinor = totalMinor;
        this.idempotencyKey = idempotencyKey;
        this.placedAt = placedAt;
    }

    public Long getId() { return id; }

    public Long getCustomerId() { return customerId; }

    public Long getShopId() { return shopId; }

    public OrderStatus getStatus() { return status; }

    public void setStatus(OrderStatus status) { this.status = status; }

    public long getTotalMinor() { return totalMinor; }

    public String getIdempotencyKey() { return idempotencyKey; }

    public OffsetDateTime getPlacedAt() { return placedAt; }

    public OffsetDateTime getFulfilledAt() { return fulfilledAt; }

    public void setFulfilledAt(OffsetDateTime fulfilledAt) { this.fulfilledAt = fulfilledAt; }

    public OffsetDateTime getCancelledAt() { return cancelledAt; }

    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
