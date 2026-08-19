package com.scholastic.portal.dto;

import com.scholastic.portal.model.Role;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        String displayName,
        Role role) {
}