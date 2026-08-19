package com.scholastic.portal.dto;

import com.scholastic.portal.model.AssignmentStatus;

public record UpdateProgressRequest(
        AssignmentStatus status,
        Long elapsedMinutes) {
}