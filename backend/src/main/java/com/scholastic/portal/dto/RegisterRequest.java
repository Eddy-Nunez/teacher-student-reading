package com.scholastic.portal.dto;

public record RegisterRequest(
        String username,
        String password,
        String displayName) {
}