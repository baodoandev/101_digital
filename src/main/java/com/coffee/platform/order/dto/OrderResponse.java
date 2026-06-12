package com.coffee.platform.order.dto;

import com.coffee.platform.order.CustOrder;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long shopId,
        String status,
        long totalMinor,
        String currency,
        OffsetDateTime placedAt,
        List<OrderItemDetail> items
) {
    public static OrderResponse from(CustOrder order, String currency, List<OrderItemDetail> items) {
        return new OrderResponse(
                order.getId(),
                order.getShopId(),
                order.getStatus().name(),
                order.getTotalMinor(),
                currency,
                order.getPlacedAt(),
                items
        );
    }
}
