package com.scholastic.portal.dto;

import com.scholastic.portal.model.AssignmentStatus;

public record StudentProgressResponse(
        Long studentId,
        String studentName,
        AssignmentStatus status,
        long elapsedMinutes) {

    public record StudentInfo(Long id, String name) {}
}