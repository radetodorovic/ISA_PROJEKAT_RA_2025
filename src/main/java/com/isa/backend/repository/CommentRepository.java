package com.isa.backend.repository;

import com.isa.backend.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideoPostIdOrderByCreatedAtDesc(Long videoPostId);

    // Pageable variant to support pagination for large comment sets
    Page<Comment> findByVideoPostIdOrderByCreatedAtDesc(Long videoPostId, Pageable pageable);
}
