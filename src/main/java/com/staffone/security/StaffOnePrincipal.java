package com.staffone.security;

import java.util.UUID;

public record StaffOnePrincipal(
    UUID   userId,
    UUID   tenantId,
    String email,
    String role,
    String tenantMode) {}
