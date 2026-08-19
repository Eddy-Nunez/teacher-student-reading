package com.scholastic.portal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scholastic.portal.model.StudentAssignment;

import jakarta.persistence.LockModeType;

public interface StudentAssignmentRepository extends JpaRepository<StudentAssignment, Long> {

    /** All progress rows for a given assignment (used by teacher to view statuses). */
    List<StudentAssignment> findByAssignmentId(Long assignmentId);

    /** All assignments assigned to a student, newest first. */
    @Query("select s from StudentAssignment s where s.student.id = :studentId order by s.assignment.createdAt desc")
    List<StudentAssignment> findByStudentId(@Param("studentId") Long studentId);

    Optional<StudentAssignment> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);

    /**
     * Pessimistic write-lock for the read-modify-write status update, so concurrent writers
     * serialize on the row | the in-memory monotonic max cannot lose an update (see StudentController).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StudentAssignment s where s.student.id = :studentId and s.assignment.id = :assignmentId")
    Optional<StudentAssignment> findLockedByStudentIdAndAssignmentId(@Param("studentId") Long studentId,
                                                                     @Param("assignmentId") Long assignmentId);
}