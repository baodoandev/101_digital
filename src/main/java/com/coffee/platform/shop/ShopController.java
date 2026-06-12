package com.coffee.platform.shop;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.shop.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShopResponse createShop(@Valid @RequestBody CreateShopRequest req,
                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return shopService.createShop(req, principal);
    }

    @GetMapping
    public List<ShopResponse> listShops() {
        return shopService.listShops();
    }

    @GetMapping("/{id}")
    public ShopResponse getShop(@PathVariable Long id) {
        return shopService.getShop(id);
    }

    @GetMapping("/nearest")
    public List<ShopResponse> findNearest(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        return shopService.findNearest(lat, lng, radiusKm, limit);
    }

    @PostMapping("/{shopId}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse addMenuItem(@PathVariable Long shopId,
                                        @Valid @RequestBody CreateMenuItemRequest req,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        return shopService.addMenuItem(shopId, req, principal);
    }

    @GetMapping("/{shopId}/menu-items")
    public List<MenuItemResponse> listMenuItems(@PathVariable Long shopId) {
        return shopService.listMenuItems(shopId);
    }

    @PostMapping("/{shopId}/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueResponse addQueue(@PathVariable Long shopId,
                                  @Valid @RequestBody CreateQueueRequest req,
                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return shopService.addQueue(shopId, req, principal);
    }

    @GetMapping("/{shopId}/queues")
    public List<QueueResponse> listQueues(@PathVariable Long shopId) {
        return shopService.listQueues(shopId);
    }

    @PostMapping("/{shopId}/operators")
    @ResponseStatus(HttpStatus.CREATED)
    public void assignOperator(@PathVariable Long shopId,
                               @Valid @RequestBody AssignOperatorRequest req,
                               @AuthenticationPrincipal AuthPrincipal principal) {
        shopService.assignOperator(shopId, req, principal);
    }
}
