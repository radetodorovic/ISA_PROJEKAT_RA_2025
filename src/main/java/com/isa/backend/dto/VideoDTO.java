package com.isa.backend.dto;

import java.time.LocalDateTime;

public class VideoDTO {
    private Long id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Long userId;
    private String uploaderEmail;
    private LocalDateTime createdAt;

    public VideoDTO() {}

    public VideoDTO(Long id, String title, String description, String videoUrl, String thumbnailUrl, Long userId, String uploaderEmail, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.userId = userId;
        this.uploaderEmail = uploaderEmail;
        this.createdAt = createdAt;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUploaderEmail() { return uploaderEmail; }
    public void setUploaderEmail(String uploaderEmail) { this.uploaderEmail = uploaderEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

