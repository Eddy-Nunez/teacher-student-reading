package com.scholastic.portal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.scholastic.portal.dto.AssignmentDetailResponse;
import com.scholastic.portal.dto.StudentAssignmentResponse;
import com.scholastic.portal.dto.UpdateProgressRequest;
import com.scholastic.portal.model.AssignmentStatus;
import com.scholastic.portal.model.StudentAssignment;
import com.scholastic.portal.repository.StudentAssignmentRepository;
import com.scholastic.portal.security.AppPrincipal;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentAssignmentRepository progressRepository;

    public StudentController(StudentAssignmentRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    /** All assignments assigned to the logged-in student, with the student's own status. */
    @GetMapping("/assignments")
    public List<StudentAssignmentResponse> assignments(@AuthenticationPrincipal AppPrincipal principal) {
        return progressRepository.findByStudentId(principal.id()).stream()
                .map(this::toSummary)
                .toList();
    }

    /** Detail for the reader view, including the book's embedded reading content. */
    @GetMapping("/assignments/{id}")
    public AssignmentDetailResponse detail(@AuthenticationPrincipal AppPrincipal principal,
                                           @PathVariable Long id) {
        StudentAssignment row = ownedRow(principal, id);
        var sa = row.getAssignment();
        var book = sa.getBook();
        return new AssignmentDetailResponse(
                sa.getId(),
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getContent(),
                book.getReferenceUrl(),
                sa.getDueDate(),
                row.getStatus(),
                row.getElapsedMinutes());
    }

    /**
     * Student updates their own progress: status and/or accumulated minutes read.
     * Minutes are monotonic (never regress) to guard against stale client writes.
     */
    @PutMapping("/assignments/{id}/status")
    public StudentAssignmentResponse updateStatus(@AuthenticationPrincipal AppPrincipal principal,
                                                  @PathVariable Long id,
                                                  @RequestBody UpdateProgressRequest request) {
        StudentAssignment row = ownedRow(principal, id);

        if (request.status() != null) {
            row.setStatus(request.status());
        }
        if (request.elapsedMinutes() != null && request.elapsedMinutes() >= 0) {
            // Monotonic guard: local timers can arrive out of order across devices.
            row.setElapsedMinutes(Math.max(row.getElapsedMinutes(), request.elapsedMinutes()));
        }
        progressRepository.save(row);
        return toSummary(row);
    }

    private StudentAssignment ownedRow(AppPrincipal principal, Long assignmentId) {
        return progressRepository.findByStudentIdAndAssignmentId(principal.id(), assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not assigned to you"));
    }

    private StudentAssignmentResponse toSummary(StudentAssignment row) {
        return new StudentAssignmentResponse(
                row.getAssignment().getId(),
                row.getAssignment().getBook().getId(),
                row.getAssignment().getBook().getTitle(),
                row.getAssignment().getBook().getAuthor(),
                row.getAssignment().getDueDate(),
                row.getStatus(),
                row.getElapsedMinutes());
    }
}