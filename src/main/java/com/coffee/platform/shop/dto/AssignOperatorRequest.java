package com.coffee.platform.shop.dto;

import jakarta.validation.constraints.NotNull;

public record AssignOperatorRequest(
        @NotNull Long userId
) {}
