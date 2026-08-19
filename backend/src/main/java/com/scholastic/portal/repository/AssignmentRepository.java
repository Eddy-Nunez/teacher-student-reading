package com.scholastic.portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scholastic.portal.model.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("select a from Assignment a where a.teacher.id = :teacherId order by a.createdAt desc")
    java.util.List<Assignment> findByTeacherId(@Param("teacherId") Long teacherId);
}