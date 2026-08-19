package com.scholastic.portal.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.scholastic.portal.dto.AssignmentSummaryResponse;
import com.scholastic.portal.dto.BookResponse;
import com.scholastic.portal.dto.CreateAssignmentRequest;
import com.scholastic.portal.dto.StudentProgressResponse;
import com.scholastic.portal.model.Assignment;
import com.scholastic.portal.model.Book;
import com.scholastic.portal.model.Role;
import com.scholastic.portal.model.StudentAssignment;
import com.scholastic.portal.model.User;
import com.scholastic.portal.repository.AssignmentRepository;
import com.scholastic.portal.repository.BookRepository;
import com.scholastic.portal.repository.StudentAssignmentRepository;
import com.scholastic.portal.repository.UserRepository;
import com.scholastic.portal.security.AppPrincipal;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final BookRepository bookRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentAssignmentRepository progressRepository;
    private final UserRepository userRepository;

    public TeacherController(BookRepository bookRepository,
                             AssignmentRepository assignmentRepository,
                             StudentAssignmentRepository progressRepository,
                             UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.assignmentRepository = assignmentRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    /** Catalog of available books to assign. */
    @GetMapping("/books")
    public List<BookResponse> books() {
        return bookRepository.findAll().stream()
                .map(b -> new BookResponse(b.getId(), b.getTitle(), b.getAuthor(), b.getDescription(), b.getReferenceUrl()))
                .toList();
    }

    /** Roster of students (used by the teacher UI when creating/assigning). */
    @GetMapping("/students")
    public List<StudentProgressResponse.StudentInfo> students() {
        return userRepository.findByRole(Role.STUDENT).stream()
                .map(u -> new StudentProgressResponse.StudentInfo(u.getId(), u.getDisplayName()))
                .toList();
    }

    /** Create an assignment for a book + due date, auto-assigned to all students. */
    @PostMapping("/assignments")
    public AssignmentSummaryResponse createAssignment(@AuthenticationPrincipal AppPrincipal principal,
                                                      @RequestBody CreateAssignmentRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        if (request.dueDate() == null || request.dueDate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueDate is required");
        }
        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(request.dueDate());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueDate must be ISO-8601 (YYYY-MM-DD)");
        }

        User teacher = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Teacher not found"));

        Assignment assignment = new Assignment(book, teacher, dueDate);
        assignment = assignmentRepository.save(assignment);

        // Auto-assign to every student, initialized to NOT_STARTED.
        for (User student : userRepository.findByRole(Role.STUDENT)) {
            progressRepository.save(new StudentAssignment(student, assignment));
        }

        return buildSummary(assignment);
    }

    /** All assignments created by the authenticated teacher, with per-student progress. */
    @GetMapping("/assignments")
    public List<AssignmentSummaryResponse> assignments(@AuthenticationPrincipal AppPrincipal principal) {
        return assignmentRepository.findByTeacherId(principal.id()).stream()
                .map(this::buildSummary)
                .toList();
    }

    /** Per-student progress for a single assignment. */
    @GetMapping("/assignments/{id}/progress")
    public AssignmentSummaryResponse progress(@AuthenticationPrincipal AppPrincipal principal,
                                              @PathVariable Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .filter(a -> a.getTeacher().getId().equals(principal.id()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        return buildSummary(assignment);
    }

    private AssignmentSummaryResponse buildSummary(Assignment assignment) {
        List<StudentAssignment> rows = progressRepository.findByAssignmentId(assignment.getId());
        List<StudentProgressResponse> progress = rows.stream()
                .map(r -> new StudentProgressResponse(
                        r.getStudent().getId(),
                        r.getStudent().getDisplayName(),
                        r.getStatus(),
                        r.getElapsedMinutes()))
                .toList();
        return AssignmentSummaryResponse.from(assignment, progress);
    }
}