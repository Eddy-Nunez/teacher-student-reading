package com.scholastic.portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scholastic.portal.model.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
}