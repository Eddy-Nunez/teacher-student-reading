package com.scholastic.portal.model;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "assignment")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The book being assigned. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    /** Due date of the reading assignment. */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Teacher who created the assignment. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    protected Assignment() {
        // JPA
    }

    public Assignment(Book book, User teacher, LocalDate dueDate) {
        this.book = book;
        this.teacher = teacher;
        this.dueDate = dueDate;
        this.createdAt = ZonedDateTime.now();
    }

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public LocalDate getDueDate() { return dueDate; }
    public User getTeacher() { return teacher; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
}