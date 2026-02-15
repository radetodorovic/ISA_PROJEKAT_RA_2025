package com.isa.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * DTO za UploadEvent - koristi se za JSON serijalizaciju
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadEventDTO implements Serializable {

    @JsonProperty("video_id")
    private String videoId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("author_email")
    private String authorEmail;

    @JsonProperty("author_name")
    private String authorName;

    @JsonProperty("video_size_bytes")
    private Long videoSizeBytes;

    @JsonProperty("thumbnail_size_bytes")
    private Long thumbnailSizeBytes;

    @JsonProperty("video_path")
    private String videoPath;

    @JsonProperty("thumbnail_path")
    private String thumbnailPath;

    @JsonProperty("upload_timestamp")
    private Long uploadTimestamp;

    @JsonProperty("location")
    private String location;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("transcode_profiles")
    private List<String> transcodeProfiles;
}

