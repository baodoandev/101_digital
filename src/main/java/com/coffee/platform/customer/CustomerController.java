package com.coffee.platform.customer;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.customer.dto.CustomerResponse;
import com.coffee.platform.customer.dto.UpdateProfileRequest;
import com.coffee.platform.order.OrderService;
import com.coffee.platform.order.dto.OrderResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final OrderService orderService;
    private final CustomerService customerService;

    public CustomerController(OrderService orderService, CustomerService customerService) {
        this.orderService = orderService;
        this.customerService = customerService;
    }

    @GetMapping("/me")
    public CustomerResponse getProfile(@AuthenticationPrincipal AuthPrincipal principal) {
        return customerService.getProfile(principal);
    }

    @PatchMapping("/me")
    public CustomerResponse updateProfile(@AuthenticationPrincipal AuthPrincipal principal,
                                          @RequestBody UpdateProfileRequest req) {
        return customerService.updateProfile(principal, req);
    }

    @GetMapping("/me/orders")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        return orderService.myOrders(principal);
    }
}
