package com.coffee.platform.auth;

public record AuthPrincipal(Long id, String subject, PrincipalType type, String role) {}
