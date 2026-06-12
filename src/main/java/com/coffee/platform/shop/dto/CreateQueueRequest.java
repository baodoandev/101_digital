package com.coffee.platform.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQueueRequest(
        @NotBlank String label,
        @NotNull @Min(1) Integer maxSize
) {}
