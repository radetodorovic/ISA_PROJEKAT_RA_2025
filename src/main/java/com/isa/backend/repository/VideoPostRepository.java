package com.isa.backend.repository;

import com.isa.backend.model.VideoPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoPostRepository extends JpaRepository<VideoPost, Long> {

    // Pronađi sve video objave od određenog korisnika
    List<VideoPost> findByUserId(Long userId);

    // Pronađi video objave po tag-u
    List<VideoPost> findByTagsContaining(String tag);

    // Pronađi najnovije video objave (sortirane po datumu kreiranja)
    List<VideoPost> findAllByOrderByCreatedAtDesc();

    // Pronađi video post po putanji fajla (koristi se za inkrement viewCount pri stream-u)
    Optional<VideoPost> findByVideoPath(String videoPath);
}