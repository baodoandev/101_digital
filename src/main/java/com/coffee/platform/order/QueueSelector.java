package com.coffee.platform.order;

import com.coffee.platform.queue.QueueEntryRepository;
import com.coffee.platform.shop.ShopQueue;
import com.coffee.platform.shop.ShopQueueRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class QueueSelector {

    private final ShopQueueRepository queueRepo;
    private final QueueEntryRepository entryRepo;

    public QueueSelector(ShopQueueRepository queueRepo, QueueEntryRepository entryRepo) {
        this.queueRepo = queueRepo;
        this.entryRepo = entryRepo;
    }

    public Optional<ShopQueue> selectQueue(Long shopId) {
        List<ShopQueue> queues = queueRepo.findByShopId(shopId).stream()
                .filter(ShopQueue::isActive)
                .toList();

        ShopQueue best = null;
        int bestCount = Integer.MAX_VALUE;

        for (ShopQueue q : queues) {
            int waiting = entryRepo.countWaiting(q.getId());
            if (waiting < q.getMaxSize() && waiting < bestCount) {
                bestCount = waiting;
                best = q;
            }
        }
        return Optional.ofNullable(best);
    }
}
