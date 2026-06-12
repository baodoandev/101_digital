package com.coffee.platform.shop;

import jakarta.persistence.*;

@Entity
@Table(name = "queue")
public class ShopQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(nullable = false)
    private String label;

    @Column(name = "max_size", nullable = false)
    private int maxSize;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @Column(nullable = false)
    private boolean active = true;

    protected ShopQueue() {}

    public Long getId() { return id; }

    public Long getShopId() { return shopId; }

    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getLabel() { return label; }

    public void setLabel(String label) { this.label = label; }

    public int getMaxSize() { return maxSize; }

    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public int getPositionIndex() { return positionIndex; }

    public void setPositionIndex(int positionIndex) { this.positionIndex = positionIndex; }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }
}
