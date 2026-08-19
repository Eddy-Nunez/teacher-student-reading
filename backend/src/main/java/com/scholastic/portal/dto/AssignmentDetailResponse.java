package com.scholastic.portal.dto;

import java.time.LocalDate;

import com.scholastic.portal.model.AssignmentStatus;

public record AssignmentDetailResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        String description,
        String content,
        String referenceUrl,
        LocalDate dueDate,
        AssignmentStatus status,
        long elapsedMinutes) {
}