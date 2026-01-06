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
import com.isa.backend.dto.VideoPostDTO;
import com.isa.backend.model.VideoPost;
import com.isa.backend.repository.VideoPostRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
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
    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Kreira novu video objavu (transakciono)
     * Ako bilo šta ne uspe, sve se rollback-uje
     */
    @Transactional
    public VideoPostDTO createVideoPost(
            String title,
            String description,
            Set<String> tags,
            MultipartFile thumbnail,
            MultipartFile video,
            String location,
            Long userId
    ) throws IOException {

        // Validacija
        if (video.getSize() > 200 * 1024 * 1024) { // 200MB
            throw new IllegalArgumentException("Video fajl je prevelik! Maksimalna veličina je 200MB.");
        }

        String videoFilename = null;
        String thumbnailFilename = null;

        try {
            // 1. Sačuvaj thumbnail
            thumbnailFilename = fileStorageService.saveThumbnailFile(thumbnail);

            // 2. Sačuvaj video (ovo može da traje dugo)
            videoFilename = fileStorageService.saveVideoFile(video);

            // 3. Kreiraj VideoPost entitet
            VideoPost videoPost = new VideoPost();
            videoPost.setTitle(title);
            videoPost.setDescription(description);
            videoPost.setTags(tags);
            videoPost.setThumbnailPath(thumbnailFilename);
            videoPost.setVideoPath(videoFilename);
            videoPost.setVideoSize(video.getSize());
            videoPost.setLocation(location);
            videoPost.setUserId(userId);

            // 4. Sačuvaj u bazu
            VideoPost savedPost = videoPostRepository.save(videoPost);

            // 5. Vrati DTO
            return convertToDTO(savedPost, true);

        } catch (Exception e) {
            // Rollback: obriši fajlove ako nešto pođe po zlu
            if (videoFilename != null) {
                fileStorageService.deleteVideoFile(videoFilename);
            }
            if (thumbnailFilename != null) {
                fileStorageService.deleteThumbnailFile(thumbnailFilename);
            }
            throw new RuntimeException("Kreiranje video objave nije uspelo: " + e.getMessage(), e);
        }
    }

    /**
     * Vraća sve video objave
     */
    public List<VideoPostDTO> getAllVideoPosts() {
        return getAllVideoPosts(false);
    }

    public List<VideoPostDTO> getAllVideoPosts(boolean authenticated) {
        return videoPostRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(vp -> convertToDTO(vp, authenticated))
                .collect(Collectors.toList());
    }

    /**
     * Vraća video objavu po ID-u
     */
    public VideoPostDTO getVideoPostById(Long id) {
        return getVideoPostById(id, false);
    }

    public VideoPostDTO getVideoPostById(Long id, boolean authenticated) {
        VideoPost videoPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena!"));
        return convertToDTO(videoPost, authenticated);
    }

    /**
     * Povećava broj lajkova (jednostavno increment)
     */
    @Transactional
    public void incrementLikeCount(Long id) {
        VideoPost videoPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena!"));
        videoPost.setLikeCount(videoPost.getLikeCount() + 1);
        videoPostRepository.save(videoPost);
    }

    /**
     * Povećava broj pregleda za data videoPath (koristi se u stream endpoint-u)
     */
    @Transactional
    public void incrementViewCountByPath(String videoPath) {
        videoPostRepository.findByVideoPath(videoPath).ifPresent(videoPost -> {
            videoPost.setViewCount(videoPost.getViewCount() + 1);
            videoPostRepository.save(videoPost);
        });
    }

    /**
     * Vraća VideoPost entitet na osnovu sačuvanog videoPath (koristi se za mapiranje filename -> videoId)
     */
    public VideoPost getVideoPostByVideoPath(String videoPath) {
        return videoPostRepository.findByVideoPath(videoPath)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena za dati filename: " + videoPath));
    }

    /**
     * Konvertuje VideoPost entitet u DTO
     */
    private VideoPostDTO convertToDTO(VideoPost videoPost, boolean authenticated) {
        VideoPostDTO dto = new VideoPostDTO();
        dto.setId(videoPost.getId());
        dto.setTitle(videoPost.getTitle());
        dto.setDescription(videoPost.getDescription());
        dto.setTags(videoPost.getTags());
        dto.setThumbnailUrl("/api/videos/thumbnail/" + videoPost.getThumbnailPath());
        dto.setVideoUrl("/api/videos/stream/" + videoPost.getVideoPath());
        dto.setVideoSize(videoPost.getVideoSize());
        dto.setCreatedAt(videoPost.getCreatedAt());
        dto.setLocation(videoPost.getLocation());
        dto.setUserId(videoPost.getUserId());
        dto.setViewCount(videoPost.getViewCount());
        dto.setLikeCount(videoPost.getLikeCount());
        dto.setCommentCount(videoPost.getCommentCount());

        // If the caller is authenticated, enable like/comment actions on the DTO
        dto.setCanLike(authenticated);
        dto.setCanComment(authenticated);

        return dto;
    }
}
