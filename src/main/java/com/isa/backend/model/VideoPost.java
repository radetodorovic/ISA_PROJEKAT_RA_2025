package com.isa.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "video_posts")
public class VideoPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String videoPath;
    private String thumbnailPath;
    private Long userId;
    private String uploaderEmail;
    private LocalDateTime createdAt;

    public VideoPost() {}

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUploaderEmail() { return uploaderEmail; }
    public void setUploaderEmail(String uploaderEmail) { this.uploaderEmail = uploaderEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "video_tags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @Column(nullable = false)
    private String thumbnailPath;

    @Column(nullable = false)
    private String videoPath;

    @Column(nullable = false)
    private Long videoSize;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String location;

    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "integer default 0")
    private Integer viewCount = 0;

    @Column(columnDefinition = "integer default 0")
    private Integer likeCount = 0;

    @Column(columnDefinition = "integer default 0")
    private Integer commentCount = 0;

    // Constructors
    public VideoPost() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public Long getVideoSize() {
        return videoSize;
    }

    public void setVideoSize(Long videoSize) {
        this.videoSize = videoSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }
}
