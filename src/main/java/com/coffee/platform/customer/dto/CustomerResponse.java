package com.coffee.platform.customer.dto;

import com.coffee.platform.customer.Customer;

public record CustomerResponse(
        Long id,
        String mobileNumber,
        String name,
        String addressLabel,
        String addressLine,
        Double latitude,
        Double longitude,
        int loyaltyScore
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getMobileNumber(), c.getName(),
                c.getAddressLabel(), c.getAddressLine(),
                c.getLatitude(), c.getLongitude(),
                c.getLoyaltyScore());
    }
}
