package com.coffee.platform.auth;

import com.coffee.platform.auth.dto.*;
import com.coffee.platform.common.ConflictException;
import com.coffee.platform.common.UnauthorizedException;
import com.coffee.platform.customer.Customer;
import com.coffee.platform.customer.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepo, CustomerRepository customerRepo,
                       PasswordEncoder encoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registerOwner(RegisterOwnerRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new ConflictException("EMAIL_TAKEN", "Email already registered");
        }
        AppUser user = new AppUser(req.email(), encoder.encode(req.password()), req.displayName(), Role.OWNER);
        user = userRepo.save(user);
        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), PrincipalType.USER, user.getRole().name());
        return new AuthResponse(token, "USER", user.getId());
    }

    public AuthResponse login(LoginRequest req) {
        AppUser user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), PrincipalType.USER, user.getRole().name());
        return new AuthResponse(token, "USER", user.getId());
    }

    @Transactional
    public AuthResponse registerCustomer(RegisterCustomerRequest req) {
        if (customerRepo.existsByMobileNumber(req.mobileNumber())) {
            throw new ConflictException("MOBILE_TAKEN", "Mobile number already registered");
        }
        Customer c = new Customer(req.mobileNumber(), req.name());
        c = customerRepo.save(c);
        String token = jwtService.generateToken(
                c.getId(), c.getMobileNumber(), PrincipalType.CUSTOMER, "CUSTOMER");
        return new AuthResponse(token, "CUSTOMER", c.getId());
    }

    public AuthResponse loginCustomer(CustomerLoginRequest req) {
        if (!"000000".equals(req.otp())) {
            throw new UnauthorizedException("Invalid OTP");
        }
        Customer c = customerRepo.findByMobileNumber(req.mobileNumber())
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));
        String token = jwtService.generateToken(
                c.getId(), c.getMobileNumber(), PrincipalType.CUSTOMER, "CUSTOMER");
        return new AuthResponse(token, "CUSTOMER", c.getId());
    }
}
