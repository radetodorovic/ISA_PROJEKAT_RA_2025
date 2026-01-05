package com.isa.backend.service;

import com.isa.backend.dto.CommentDTO;
import com.isa.backend.model.Comment;
import com.isa.backend.model.VideoPost;
import com.isa.backend.repository.CommentRepository;
import com.isa.backend.repository.VideoPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Transactional
    public CommentDTO addComment(Long videoId, Long userId, String text) {
        VideoPost vp = videoPostRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena!"));

        Comment c = new Comment();
        c.setText(text);
        c.setUserId(userId);
        c.setVideoPostId(videoId);

        Comment saved = commentRepository.save(c);

        // increment comment count on video
        vp.setCommentCount(vp.getCommentCount() + 1);
        videoPostRepository.save(vp);

        return toDTO(saved);
    }

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
        dto.setVideoPostId(c.getVideoPostId());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}

