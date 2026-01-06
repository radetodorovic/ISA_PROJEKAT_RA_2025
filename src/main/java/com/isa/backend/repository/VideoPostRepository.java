package com.isa.backend.repository;

import com.isa.backend.model.VideoPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoPostRepository extends JpaRepository<VideoPost, Long> {
}

