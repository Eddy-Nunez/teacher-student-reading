package com.scholastic.portal.dto;

import java.time.LocalDate;
import java.util.List;

import com.scholastic.portal.model.Assignment;
import com.scholastic.portal.model.AssignmentStatus;

public record AssignmentSummaryResponse(
        Long id,
        String bookTitle,
        String bookAuthor,
        LocalDate dueDate,
        long assignedStudentsCount,
        long completedCount,
        long inProgressCount,
        long notStartedCount,
        List<StudentProgressResponse> studentProgress) {

    public static AssignmentSummaryResponse from(Assignment a, List<StudentProgressResponse> progress) {
        long assigned = progress.size();
        long completed = progress.stream().filter(p -> p.status() == AssignmentStatus.COMPLETED).count();
        long inProgress = progress.stream().filter(p -> p.status() == AssignmentStatus.IN_PROGRESS).count();
        long notStarted = progress.stream().filter(p -> p.status() == AssignmentStatus.NOT_STARTED).count();
        return new AssignmentSummaryResponse(
                a.getId(),
                a.getBook().getTitle(),
                a.getBook().getAuthor(),
                a.getDueDate(),
                assigned,
                completed,
                inProgress,
                notStarted,
                progress);
    }
}