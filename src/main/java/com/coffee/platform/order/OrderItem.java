package com.coffee.platform.order;

import jakarta.persistence.*;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {}

    public OrderItem(Long orderId, Long menuItemId, String itemName, long unitPriceMinor, int quantity) {
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.unitPriceMinor = unitPriceMinor;
        this.quantity = quantity;
    }

    public Long getId() { return id; }

    public Long getOrderId() { return orderId; }

    public Long getMenuItemId() { return menuItemId; }

    public String getItemName() { return itemName; }

    public long getUnitPriceMinor() { return unitPriceMinor; }

    public int getQuantity() { return quantity; }
}
