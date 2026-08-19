package com.scholastic.portal.dto;

public record CreateAssignmentRequest(
        Long bookId,
        String dueDate) {
}