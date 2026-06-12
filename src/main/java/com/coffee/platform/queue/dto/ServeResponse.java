package com.coffee.platform.queue.dto;

public record ServeResponse(
        Long entryId, Long orderId, Long customerId, String status
) {}
