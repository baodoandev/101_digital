package com.coffee.platform.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMenuItemRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(0) Long priceMinor
) {}
