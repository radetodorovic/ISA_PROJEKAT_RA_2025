package com.isa.backend.repository;

import com.isa.backend.model.VideoCommentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoCommentEventRepository extends JpaRepository<VideoCommentEvent, Long> {
    List<VideoCommentEvent> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
