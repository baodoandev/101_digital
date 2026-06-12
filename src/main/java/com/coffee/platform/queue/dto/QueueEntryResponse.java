package com.coffee.platform.queue.dto;

import com.coffee.platform.queue.QueueEntry;

import java.time.OffsetDateTime;

public record QueueEntryResponse(
        Long id, Long orderId, Long customerId, String status,
        OffsetDateTime joinedAt, OffsetDateTime servedAt
) {
    public static QueueEntryResponse from(QueueEntry e) {
        return new QueueEntryResponse(
                e.getId(), e.getOrderId(), e.getCustomerId(),
                e.getStatus().name(), e.getJoinedAt(), e.getServedAt()
        );
    }
}
