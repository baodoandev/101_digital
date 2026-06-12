package com.coffee.platform.shop.dto;

import com.coffee.platform.shop.ShopQueue;

public record QueueResponse(
        Long id,
        String label,
        int maxSize,
        int positionIndex,
        boolean active
) {

    public static QueueResponse from(ShopQueue q) {
        return new QueueResponse(
                q.getId(),
                q.getLabel(),
                q.getMaxSize(),
                q.getPositionIndex(),
                q.isActive()
        );
    }
}
