package com.coffee.platform.queue;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "queue_entry")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queue_id", nullable = false)
    private Long queueId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueEntryStatus status;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "served_at")
    private OffsetDateTime servedAt;

    protected QueueEntry() {}

    public QueueEntry(Long queueId, Long orderId, Long customerId, OffsetDateTime joinedAt) {
        this.queueId = queueId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = QueueEntryStatus.WAITING;
        this.joinedAt = joinedAt;
    }

    public Long getId() { return id; }

    public Long getQueueId() { return queueId; }

    public Long getOrderId() { return orderId; }

    public Long getCustomerId() { return customerId; }

    public QueueEntryStatus getStatus() { return status; }

    public OffsetDateTime getJoinedAt() { return joinedAt; }

    public OffsetDateTime getServedAt() { return servedAt; }

    public void setStatus(QueueEntryStatus status) { this.status = status; }

    public void setServedAt(OffsetDateTime servedAt) { this.servedAt = servedAt; }
}
