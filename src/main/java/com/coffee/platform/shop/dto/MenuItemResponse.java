package com.coffee.platform.shop.dto;

import com.coffee.platform.shop.MenuItem;

public record MenuItemResponse(
        Long id,
        String name,
        String description,
        long priceMinor,
        boolean available
) {

    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPriceMinor(),
                item.isAvailable()
        );
    }
}
