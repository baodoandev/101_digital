package com.coffee.platform.queue;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.common.NotFoundException;
import com.coffee.platform.customer.CustomerRepository;
import com.coffee.platform.order.CustOrder;
import com.coffee.platform.order.CustOrderRepository;
import com.coffee.platform.order.OrderStatus;
import com.coffee.platform.queue.dto.QueueEntryResponse;
import com.coffee.platform.queue.dto.QueueSnapshotResponse;
import com.coffee.platform.queue.dto.ServeResponse;
import com.coffee.platform.shop.ShopAccess;
import com.coffee.platform.shop.ShopQueue;
import com.coffee.platform.shop.ShopQueueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class QueueOpsService {

    private final ShopQueueRepository queueRepo;
    private final QueueEntryRepository entryRepo;
    private final CustOrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final ShopAccess shopAccess;
    private final Clock clock;

    public QueueOpsService(ShopQueueRepository queueRepo,
                           QueueEntryRepository entryRepo,
                           CustOrderRepository orderRepo,
                           CustomerRepository customerRepo,
                           ShopAccess shopAccess,
                           Clock clock) {
        this.queueRepo = queueRepo;
        this.entryRepo = entryRepo;
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.shopAccess = shopAccess;
        this.clock = clock;
    }

    public QueueSnapshotResponse getSnapshot(Long queueId, AuthPrincipal principal) {
        ShopQueue queue = queueRepo.findById(queueId)
                .orElseThrow(() -> new NotFoundException("Queue not found"));
        shopAccess.loadShopForOperator(queue.getShopId(), principal);
        int waiting = entryRepo.countWaiting(queueId);
        return new QueueSnapshotResponse(queue.getId(), queue.getLabel(), queue.getMaxSize(), waiting);
    }

    public List<QueueEntryResponse> getEntries(Long queueId, AuthPrincipal principal) {
        ShopQueue queue = queueRepo.findById(queueId)
                .orElseThrow(() -> new NotFoundException("Queue not found"));
        shopAccess.loadShopForOperator(queue.getShopId(), principal);
        return entryRepo.findByQueueIdAndStatus(queueId, QueueEntryStatus.WAITING).stream()
                .map(QueueEntryResponse::from).toList();
    }

    @Transactional
    public ServeResponse serveNext(Long queueId, AuthPrincipal principal) {
        ShopQueue queue = queueRepo.findById(queueId)
                .orElseThrow(() -> new NotFoundException("Queue not found"));
        shopAccess.loadShopForOperator(queue.getShopId(), principal);

        QueueEntry entry = entryRepo.lockNextWaiting(queueId)
                .orElseThrow(() -> new NotFoundException("No waiting entries"));

        return doServe(entry);
    }

    @Transactional
    public ServeResponse serveSpecific(Long queueId, Long entryId, AuthPrincipal principal) {
        ShopQueue queue = queueRepo.findById(queueId)
                .orElseThrow(() -> new NotFoundException("Queue not found"));
        shopAccess.loadShopForOperator(queue.getShopId(), principal);

        QueueEntry entry = entryRepo.lockWaitingById(entryId)
                .orElseThrow(() -> new NotFoundException("Entry not found or already served"));

        if (!entry.getQueueId().equals(queueId)) {
            throw new NotFoundException("Entry not in this queue");
        }

        return doServe(entry);
    }

    private ServeResponse doServe(QueueEntry entry) {
        OffsetDateTime now = OffsetDateTime.now(clock);

        entry.setStatus(QueueEntryStatus.SERVED);
        entry.setServedAt(now);
        entryRepo.save(entry);

        CustOrder order = orderRepo.findById(entry.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setStatus(OrderStatus.FULFILLED);
        order.setFulfilledAt(now);
        orderRepo.save(order);

        customerRepo.incrementLoyalty(entry.getCustomerId());

        return new ServeResponse(entry.getId(), entry.getOrderId(), entry.getCustomerId(), "SERVED");
    }
}
