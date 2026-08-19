package com.scholastic.portal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(length = 2000)
    private String description;

    /** Embedded reading content rendered in the reader view. */
    @Column(length = 60000)
    private String content;

    /** Optional external source link. */
    @Column(name = "reference_url")
    private String referenceUrl;

    protected Book() {
        // JPA
    }

    public Book(String title, String author, String description, String content, String referenceUrl) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.content = content;
        this.referenceUrl = referenceUrl;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getContent() { return content; }
    public String getReferenceUrl() { return referenceUrl; }
}