package com.coffee.platform.order;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.order.dto.OrderResponse;
import com.coffee.platform.order.dto.PlaceOrderRequest;
import com.coffee.platform.order.dto.QueuePositionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest req,
                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                    @AuthenticationPrincipal AuthPrincipal principal) {
        return orderService.placeOrder(req, idempotencyKey, principal);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id,
                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return orderService.getOrder(id, principal);
    }

    @GetMapping("/{id}/queue-position")
    public QueuePositionResponse getQueuePosition(@PathVariable Long id,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return orderService.getPosition(id, principal);
    }

    @DeleteMapping("/{id}")
    public void cancelOrder(@PathVariable Long id,
                            @AuthenticationPrincipal AuthPrincipal principal) {
        orderService.cancelOrder(id, principal);
    }
}
