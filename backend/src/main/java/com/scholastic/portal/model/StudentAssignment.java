package com.scholastic.portal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Progress record for a specific (student, assignment) pair.
 * Tracked by {\@link #status} and accumulated {@link #elapsedMinutes}.
 */
@Entity
@Table(name = "student_assignment_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "assignment_id"}))
public class StudentAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.NOT_STARTED;

    @Column(name = "elapsed_minutes", nullable = false)
    private long elapsedMinutes = 0L;

    protected StudentAssignment() {
        // JPA
    }

    public StudentAssignment(User student, Assignment assignment) {
        this.student = student;
        this.assignment = assignment;
    }

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Assignment getAssignment() { return assignment; }
    public AssignmentStatus getStatus() { return status; }
    public long getElapsedMinutes() { return elapsedMinutes; }

    public void setStatus(AssignmentStatus status) { this.status = status; }
    public void setElapsedMinutes(long elapsedMinutes) { this.elapsedMinutes = elapsedMinutes; }
}