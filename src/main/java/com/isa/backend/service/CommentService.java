package com.isa.backend.service;

import com.isa.backend.dto.CommentDTO;
import com.isa.backend.exception.RateLimitExceededException;
import com.isa.backend.model.Comment;
import com.isa.backend.model.User;
import com.isa.backend.model.VideoPost;
import com.isa.backend.repository.CommentRepository;
import com.isa.backend.repository.VideoPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentRateLimitService commentRateLimitService;

    private static final int MAX_COMMENT_LENGTH = 2000;

    @Transactional
    @CacheEvict(value = "videoComments", allEntries = true)
    public CommentDTO addComment(Long videoId, Long userId, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Komentar ne može biti prazan");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Komentar je predug. Maksimalna dužina je " + MAX_COMMENT_LENGTH + " karaktera.");
        }

        // Enforce per-user hourly limit
        boolean allowed = commentRateLimitService.tryConsume(userId);
        if (!allowed) {
            throw new RateLimitExceededException("Prekoračen broj komentara (maks. 60 po satu)");
        }

        VideoPost vp = videoPostRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena!"));

        Comment c = new Comment();
        c.setText(trimmed);
        c.setUserId(userId);
        c.setVideoPostId(videoId);

        Comment saved = commentRepository.save(c);

        // increment comment count on video
        vp.setCommentCount(vp.getCommentCount() + 1);
        videoPostRepository.save(vp);

        return toDTO(saved);
    }

    /**
     * Returns paginated comments for a video, newest first.
     * Results are cached per videoId for faster repeated reads.
     */
    @Cacheable(value = "videoComments", key = "#videoId + '_' + #page + '_' + #size")
    public List<CommentDTO> getCommentsForVideo(Long videoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentsPage = commentRepository.findByVideoPostIdOrderByCreatedAtDesc(videoId, pageable);
        return commentsPage.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Backwards-compatible method used by controllers that don't pass pagination
    public List<CommentDTO> getCommentsForVideo(Long videoId) {
        return commentRepository.findByVideoPostIdOrderByCreatedAtDesc(videoId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private CommentDTO toDTO(Comment c) {
        CommentDTO dto = new CommentDTO();
        dto.setId(c.getId());
        dto.setText(c.getText());
        dto.setUserId(c.getUserId());
        try {
            User u = userService.findById(c.getUserId());
            dto.setUsername(u.getUsername());
        } catch (RuntimeException e) {
            dto.setUsername(null);
        }
        dto.setVideoPostId(c.getVideoPostId());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}
