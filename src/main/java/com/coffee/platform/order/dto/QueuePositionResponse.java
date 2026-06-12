package com.coffee.platform.order.dto;

public record QueuePositionResponse(
        Long orderId,
        Long queueId,
        int position,
        long etaSeconds
) {}
