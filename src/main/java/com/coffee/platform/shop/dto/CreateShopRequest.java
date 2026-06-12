package com.coffee.platform.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateShopRequest(
        @NotBlank String name,
        String contactPhone,
        String contactEmail,
        String addressLine,
        String city,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String timezone,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull String openingTime,
        @NotNull String closingTime,
        Integer avgServiceSeconds
) {}
