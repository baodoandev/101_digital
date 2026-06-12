package com.coffee.platform.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    List<QueueEntry> findByQueueIdAndStatus(Long queueId, QueueEntryStatus status);

    @Query("SELECT COUNT(e) FROM QueueEntry e WHERE e.queueId = :queueId AND e.status = 'WAITING'")
    int countWaiting(Long queueId);

    @Query("SELECT COUNT(e) FROM QueueEntry e WHERE e.queueId = :queueId AND e.status = 'WAITING' AND e.joinedAt < :joinedAt")
    int countAhead(Long queueId, OffsetDateTime joinedAt);

    @Query(value = """
            SELECT * FROM queue_entry
            WHERE queue_id = :queueId AND status = 'WAITING'
            ORDER BY joined_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<QueueEntry> lockNextWaiting(Long queueId);

    @Query(value = """
            SELECT * FROM queue_entry
            WHERE id = :entryId AND status = 'WAITING'
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<QueueEntry> lockWaitingById(Long entryId);

    List<QueueEntry> findByOrderId(Long orderId);

    @Query(value = """
            SELECT qe.* FROM queue_entry qe
            JOIN queue q ON qe.queue_id = q.id
            WHERE q.shop_id = :shopId AND qe.status = 'WAITING'
            ORDER BY qe.joined_at
            """, nativeQuery = true)
    List<QueueEntry> findWaitingByShopId(Long shopId);
}
