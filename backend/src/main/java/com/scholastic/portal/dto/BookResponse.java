package com.scholastic.portal.dto;

public record BookResponse(
        Long id,
        String title,
        String author,
        String description,
        String referenceUrl) {
}