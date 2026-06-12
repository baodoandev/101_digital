package com.coffee.platform.shop.dto;

import com.coffee.platform.shop.Shop;

import java.time.format.DateTimeFormatter;

public record ShopResponse(
        Long id,
        String name,
        String contactPhone,
        String contactEmail,
        String addressLine,
        String city,
        String country,
        Double latitude,
        Double longitude,
        String timezone,
        String currency,
        String openingTime,
        String closingTime,
        int avgServiceSeconds,
        String status
) {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getContactPhone(),
                shop.getContactEmail(),
                shop.getAddressLine(),
                shop.getCity(),
                shop.getCountry(),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getTimezone(),
                shop.getCurrency(),
                shop.getOpeningTime().format(TIME_FMT),
                shop.getClosingTime().format(TIME_FMT),
                shop.getAvgServiceSeconds(),
                shop.getStatus()
        );
    }
}
