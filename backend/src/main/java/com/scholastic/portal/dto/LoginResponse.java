package com.scholastic.portal.dto;

import com.scholastic.portal.model.Role;

/**
 * Session/user payload returned on login and session validation. The JWT itself is NOT exposed
 * in the response body — it travels only in an HttpOnly cookie.
 */
public record LoginResponse(
        Long userId,
        String username,
        String displayName,
        Role role) {
}