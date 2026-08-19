package com.scholastic.portal.dto;

import java.time.LocalDate;

import com.scholastic.portal.model.AssignmentStatus;

public record StudentAssignmentResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        LocalDate dueDate,
        AssignmentStatus status,
        long elapsedMinutes) {
}