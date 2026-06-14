package com.coffee.platform.customer.dto;

public record UpdateProfileRequest(
        String name,
        String addressLabel,
        String addressLine,
        Double latitude,
        Double longitude
) {}
