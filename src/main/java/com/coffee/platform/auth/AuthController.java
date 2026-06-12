package com.coffee.platform.auth;

import com.coffee.platform.auth.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/owner/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerOwner(@Valid @RequestBody RegisterOwnerRequest req) {
        return authService.registerOwner(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/customer/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerCustomer(@Valid @RequestBody RegisterCustomerRequest req) {
        return authService.registerCustomer(req);
    }

    @PostMapping("/customer/login")
    public AuthResponse loginCustomer(@Valid @RequestBody CustomerLoginRequest req) {
        return authService.loginCustomer(req);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return new MeResponse(principal.id(), principal.subject(), principal.type().name(), principal.role());
    }
}
