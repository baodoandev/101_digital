package com.coffee.platform.customer;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.order.OrderService;
import com.coffee.platform.order.dto.OrderResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final OrderService orderService;

    public CustomerController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/me/orders")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        return orderService.myOrders(principal);
    }
}
