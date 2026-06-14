package com.coffee.platform.customer;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.auth.PrincipalType;
import com.coffee.platform.common.ForbiddenException;
import com.coffee.platform.common.NotFoundException;
import com.coffee.platform.customer.dto.CustomerResponse;
import com.coffee.platform.customer.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepo;

    public CustomerService(CustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getProfile(AuthPrincipal principal) {
        requireCustomer(principal);
        Customer c = customerRepo.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        return CustomerResponse.from(c);
    }

    @Transactional
    public CustomerResponse updateProfile(AuthPrincipal principal, UpdateProfileRequest req) {
        requireCustomer(principal);
        Customer c = customerRepo.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        if (req.name() != null) c.setName(req.name());
        if (req.addressLabel() != null) c.setAddressLabel(req.addressLabel());
        if (req.addressLine() != null) c.setAddressLine(req.addressLine());
        if (req.latitude() != null) c.setLatitude(req.latitude());
        if (req.longitude() != null) c.setLongitude(req.longitude());
        c = customerRepo.save(c);
        return CustomerResponse.from(c);
    }

    private void requireCustomer(AuthPrincipal principal) {
        if (principal.type() != PrincipalType.CUSTOMER) {
            throw new ForbiddenException("Only customers can access this resource");
        }
    }
}
