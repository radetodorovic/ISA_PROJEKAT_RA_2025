package com.isa.backend.repository;

import com.isa.backend.model.VideoLikeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoLikeEventRepository extends JpaRepository<VideoLikeEvent, Long> {
    List<VideoLikeEvent> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
