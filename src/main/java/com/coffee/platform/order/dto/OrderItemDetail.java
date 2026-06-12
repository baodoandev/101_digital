package com.coffee.platform.order.dto;

import com.coffee.platform.order.OrderItem;

public record OrderItemDetail(
        Long menuItemId,
        String itemName,
        long unitPriceMinor,
        int quantity
) {
    public static OrderItemDetail from(OrderItem item) {
        return new OrderItemDetail(
                item.getMenuItemId(),
                item.getItemName(),
                item.getUnitPriceMinor(),
                item.getQuantity()
        );
    }
}
