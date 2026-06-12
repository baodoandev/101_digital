package com.coffee.platform.order;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.auth.PrincipalType;
import com.coffee.platform.common.BadRequestException;
import com.coffee.platform.common.ConflictException;
import com.coffee.platform.common.ForbiddenException;
import com.coffee.platform.common.NotFoundException;
import com.coffee.platform.order.dto.*;
import com.coffee.platform.queue.QueueEntry;
import com.coffee.platform.queue.QueueEntryRepository;
import com.coffee.platform.queue.QueueEntryStatus;
import com.coffee.platform.shop.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CustOrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ShopRepository shopRepo;
    private final MenuItemRepository menuItemRepo;
    private final QueueEntryRepository queueEntryRepo;
    private final QueueSelector queueSelector;
    private final Clock clock;

    public OrderService(CustOrderRepository orderRepo,
                        OrderItemRepository orderItemRepo,
                        ShopRepository shopRepo,
                        MenuItemRepository menuItemRepo,
                        QueueEntryRepository queueEntryRepo,
                        QueueSelector queueSelector,
                        Clock clock) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.shopRepo = shopRepo;
        this.menuItemRepo = menuItemRepo;
        this.queueEntryRepo = queueEntryRepo;
        this.queueSelector = queueSelector;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req, String idempotencyKey, AuthPrincipal principal) {
        requireCustomer(principal);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepo.findByCustomerIdAndIdempotencyKey(principal.id(), idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Shop shop = shopRepo.findById(req.shopId())
                .orElseThrow(() -> new NotFoundException("Shop not found"));

        if (!"ACTIVE".equals(shop.getStatus())) {
            throw new BadRequestException("Shop is not active");
        }

        boolean open = OpeningHours.isOpen(
                shop.getOpeningTime(),
                shop.getClosingTime(),
                ZoneId.of(shop.getTimezone()),
                clock.instant()
        );
        if (!open) {
            throw new ConflictException("SHOP_CLOSED", "Shop is currently closed");
        }

        Map<Long, MenuItem> menuMap = menuItemRepo.findByShopId(req.shopId()).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        long total = 0;
        List<OrderItemSnapshot> snapshots = new ArrayList<>();
        for (OrderItemRequest itemReq : req.items()) {
            MenuItem menuItem = menuMap.get(itemReq.menuItemId());
            if (menuItem == null) {
                throw new NotFoundException("Menu item not found: " + itemReq.menuItemId());
            }
            if (!menuItem.isAvailable()) {
                throw new BadRequestException("Menu item unavailable: " + menuItem.getName());
            }
            long lineTotal = menuItem.getPriceMinor() * itemReq.quantity();
            total += lineTotal;
            snapshots.add(new OrderItemSnapshot(menuItem.getId(), menuItem.getName(), menuItem.getPriceMinor(), itemReq.quantity()));
        }

        ShopQueue queue = queueSelector.selectQueue(req.shopId())
                .orElseThrow(() -> new ConflictException("QUEUE_FULL", "All queues are full"));

        OffsetDateTime now = OffsetDateTime.now(clock);
        String idemKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : null;

        CustOrder order = new CustOrder(principal.id(), req.shopId(), OrderStatus.WAITING, total, idemKey, now);
        order = orderRepo.save(order);

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemSnapshot snap : snapshots) {
            OrderItem oi = new OrderItem(order.getId(), snap.menuItemId(), snap.itemName(), snap.unitPriceMinor(), snap.quantity());
            items.add(orderItemRepo.save(oi));
        }

        QueueEntry entry = new QueueEntry(queue.getId(), order.getId(), principal.id(), now);
        queueEntryRepo.save(entry);

        String currency = shop.getCurrency();
        List<OrderItemDetail> itemDetails = items.stream().map(OrderItemDetail::from).toList();
        return OrderResponse.from(order, currency, itemDetails);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, AuthPrincipal principal) {
        CustOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        checkOrderAccess(order, principal);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public QueuePositionResponse getPosition(Long orderId, AuthPrincipal principal) {
        CustOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        checkOrderAccess(order, principal);

        if (order.getStatus() != OrderStatus.WAITING) {
            throw new ConflictException("INVALID_STATE", "Order is not in WAITING status");
        }

        List<QueueEntry> entries = queueEntryRepo.findByOrderId(orderId);
        QueueEntry entry = entries.stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.WAITING)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Queue entry not found"));

        int ahead = queueEntryRepo.countAhead(entry.getQueueId(), entry.getJoinedAt());
        int position = ahead + 1;

        Shop shop = shopRepo.findById(order.getShopId())
                .orElseThrow(() -> new NotFoundException("Shop not found"));

        long etaSeconds = (long) position * shop.getAvgServiceSeconds();

        return new QueuePositionResponse(orderId, entry.getQueueId(), position, etaSeconds);
    }

    @Transactional
    public void cancelOrder(Long orderId, AuthPrincipal principal) {
        requireCustomer(principal);

        CustOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getCustomerId().equals(principal.id())) {
            throw new ForbiddenException("Not your order");
        }

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.WAITING) {
            throw new ConflictException("INVALID_STATE", "Order cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(OffsetDateTime.now(clock));
        orderRepo.save(order);

        List<QueueEntry> entries = queueEntryRepo.findByOrderId(orderId);
        Long queueId = null;
        for (QueueEntry entry : entries) {
            if (entry.getStatus() == QueueEntryStatus.WAITING) {
                entry.setStatus(QueueEntryStatus.CANCELLED);
                queueEntryRepo.save(entry);
                queueId = entry.getQueueId();
            }
        }

        String traceId = MDC.get("traceId");
        log.info("""
                {{"event":"order.cancelled","orderId":{},"shopId":{},"queueId":{},"traceId":"{}"}}""",
                orderId, order.getShopId(), queueId, traceId != null ? traceId : "");
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(AuthPrincipal principal) {
        requireCustomer(principal);

        List<CustOrder> orders = orderRepo.findByCustomerIdOrderByPlacedAtDesc(principal.id());
        return orders.stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(CustOrder order) {
        Shop shop = shopRepo.findById(order.getShopId())
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        List<OrderItemDetail> items = orderItemRepo.findByOrderId(order.getId()).stream()
                .map(OrderItemDetail::from)
                .toList();
        return OrderResponse.from(order, shop.getCurrency(), items);
    }

    private void checkOrderAccess(CustOrder order, AuthPrincipal principal) {
        if (principal.type() == PrincipalType.CUSTOMER) {
            if (!order.getCustomerId().equals(principal.id())) {
                throw new ForbiddenException("Not your order");
            }
        } else if (principal.type() == PrincipalType.USER) {
            Shop shop = shopRepo.findById(order.getShopId())
                    .orElseThrow(() -> new NotFoundException("Shop not found"));
            if (!shop.getOwnerId().equals(principal.id())) {
                throw new ForbiddenException("Not your shop's order");
            }
        }
    }

    private void requireCustomer(AuthPrincipal principal) {
        if (principal.type() != PrincipalType.CUSTOMER) {
            throw new ForbiddenException("Only customers can perform this action");
        }
    }

    private record OrderItemSnapshot(Long menuItemId, String itemName, long unitPriceMinor, int quantity) {}
}
