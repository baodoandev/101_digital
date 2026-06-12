package com.coffee.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterCustomerRequest(
        @NotBlank String mobileNumber,
        @NotBlank String name
) {}
