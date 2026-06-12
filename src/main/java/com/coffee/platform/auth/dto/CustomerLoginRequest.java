package com.coffee.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerLoginRequest(
        @NotBlank String mobileNumber,
        @NotBlank String otp
) {}
