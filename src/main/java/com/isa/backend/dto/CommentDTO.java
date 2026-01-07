package com.isa.backend.dto;

import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private String text;
    private Long userId;
    private String username;
    private Long videoPostId;
    private LocalDateTime createdAt;

    public CommentDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getVideoPostId() {
        return videoPostId;
    }

    public void setVideoPostId(Long videoPostId) {
        this.videoPostId = videoPostId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
