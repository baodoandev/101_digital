package com.coffee.platform.shop;

import jakarta.persistence.*;

@Entity
@Table(name = "operator_assignment")
public class OperatorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    protected OperatorAssignment() {}

    public OperatorAssignment(Long userId, Long shopId) {
        this.userId = userId;
        this.shopId = shopId;
    }

    public Long getId() { return id; }

    public Long getUserId() { return userId; }

    public Long getShopId() { return shopId; }
}
