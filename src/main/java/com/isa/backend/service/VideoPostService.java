package com.isa.backend.service;

import com.isa.backend.dto.VideoDTO;
import com.isa.backend.model.VideoPost;
import com.isa.backend.repository.VideoPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoPostService {

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public List<VideoDTO> getAllVideoPosts() {
        return videoPostRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public VideoDTO createVideoPost(String title, String description, MultipartFile video, MultipartFile thumbnail, Long userId, String uploaderEmail) throws IOException {
        if (video == null || video.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }

        // Validate content type (simple check)
        String contentType = video.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("Video must be a video file");
        }

        // Prepare directories
        Path videosPath = Paths.get(uploadDir, "videos");
        Path thumbsPath = Paths.get(uploadDir, "thumbnails");
        Files.createDirectories(videosPath);
        Files.createDirectories(thumbsPath);

        String originalVideoName = video.getOriginalFilename() == null ? "video.mp4" : video.getOriginalFilename();
        String videoFilename = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(originalVideoName);
        Path targetVideo = videosPath.resolve(videoFilename);
        Files.copy(video.getInputStream(), targetVideo);

        String thumbnailFilename = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String originalThumbName = thumbnail.getOriginalFilename() == null ? "thumb.jpg" : thumbnail.getOriginalFilename();
            thumbnailFilename = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(originalThumbName);
            Path targetThumb = thumbsPath.resolve(thumbnailFilename);
            Files.copy(thumbnail.getInputStream(), targetThumb);
        }

        VideoPost vp = new VideoPost();
        vp.setTitle(title);
        vp.setDescription(description);
        vp.setVideoPath(Paths.get("videos").resolve(videoFilename).toString());
        vp.setThumbnailPath(thumbnailFilename != null ? Paths.get("thumbnails").resolve(thumbnailFilename).toString() : null);
        vp.setUserId(userId);
        vp.setUploaderEmail(uploaderEmail);
        vp.setCreatedAt(LocalDateTime.now());

        VideoPost saved = videoPostRepository.save(vp);
        return toDto(saved);
    }

    private VideoDTO toDto(VideoPost vp) {
        String videoUrl = "/api/videos/stream/" + Paths.get(vp.getVideoPath()).getFileName().toString();
        String thumbnailUrl = vp.getThumbnailPath() != null ? "/api/videos/thumbnail/" + Paths.get(vp.getThumbnailPath()).getFileName().toString() : null;
        return new VideoDTO(vp.getId(), vp.getTitle(), vp.getDescription(), videoUrl, thumbnailUrl, vp.getUserId(), vp.getUploaderEmail(), vp.getCreatedAt());
    }
}
