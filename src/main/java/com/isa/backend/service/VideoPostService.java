package com.isa.backend.service;

import com.isa.backend.dto.VideoPostDTO;
import com.isa.backend.model.VideoPost;
import com.isa.backend.repository.VideoPostRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoPostService {

    private static final Logger logger = LoggerFactory.getLogger(VideoPostService.class);

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.upload.timeout-ms:60000}")
    private long uploadTimeoutMs;

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

        if (!fileStorageService.isMp4(video)) {
            throw new IllegalArgumentException("Video mora biti pravi MP4 fajl!");
        }

        // Generiši finalna jedinstvena imena (bez ekstenzije promenjene)
        String originalVideoName = video.getOriginalFilename();
        String videoExt = originalVideoName != null ? originalVideoName.substring(originalVideoName.lastIndexOf('.')) : "";
        String finalVideoFilename = UUID.randomUUID().toString() + videoExt;

        String originalThumbName = thumbnail.getOriginalFilename();
        String thumbExt = originalThumbName != null ? originalThumbName.substring(originalThumbName.lastIndexOf('.')) : "";
        String finalThumbFilename = UUID.randomUUID().toString() + thumbExt;

        String tempVideoName = null;
        String tempThumbName = null;

        try {
            // Sačuvaj fajlove u temp direktorijume (sa timeout za video)
            tempThumbName = fileStorageService.saveThumbnailFileToTempWithFinalName(thumbnail, finalThumbFilename);
            try {
                tempVideoName = fileStorageService.saveVideoFileToTempWithFinalName(video, finalVideoFilename, uploadTimeoutMs);
            } catch (IOException e) {
                // if video save timed out or failed, delete thumb temp and rethrow
                if (tempThumbName != null) {
                    try { fileStorageService.deleteTempThumbnail(tempThumbName); } catch (IOException ignored) {}
                }
                throw e;
            }

            // Kreiraj VideoPost entitet (postavimo finalna imena)
            VideoPost videoPost = new VideoPost();
            videoPost.setTitle(title);
            videoPost.setDescription(description);
            videoPost.setTags(tags);
            videoPost.setThumbnailPath(finalThumbFilename);
            videoPost.setVideoPath(finalVideoFilename);
            videoPost.setVideoSize(video.getSize());
            videoPost.setLocation(location);
            videoPost.setUserId(userId);

            // Sačuvaj u bazu (još uvek temp fajlovi postoje)
            VideoPost savedPost = videoPostRepository.save(videoPost);

            final String tv = tempVideoName;
            final String tt = tempThumbName;
            final String fv = finalVideoFilename;
            final String ft = finalThumbFilename;
            // Registruj transaction synchronization: na commit premestiti temp fajlove u finalne, na rollback obrisati temp fajlove
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorageService.moveTempThumbnailToFinal(tt, ft);
                        fileStorageService.moveTempVideoToFinal(tv, fv);
                    } catch (IOException e) {
                        // Log error (ne možemo rollback-ovati ovde jer transakcija je već commit-ovana)
                        logger.error("Greška pri premještanju fajlova nakon commita:", e);
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        try {
                            if (tv != null) fileStorageService.deleteTempVideo(tv);
                            if (tt != null) fileStorageService.deleteTempThumbnail(tt);
                        } catch (IOException e) {
                            logger.error("Greška pri brisanju temp fajlova nakon rollback-a:", e);
                        }
                    }
                }
            });

            return convertToDTO(savedPost, true);

        } catch (IOException e) {
            // cleanup temp files if any exist
            try { if (tempVideoName != null) fileStorageService.deleteTempVideo(tempVideoName); } catch (IOException ignored) {}
            try { if (tempThumbName != null) fileStorageService.deleteTempThumbnail(tempThumbName); } catch (IOException ignored) {}
            throw e;
        } catch (RuntimeException e) {
            // cleanup and rethrow
            try { if (tempVideoName != null) fileStorageService.deleteTempVideo(tempVideoName); } catch (IOException ignored) {}
            try { if (tempThumbName != null) fileStorageService.deleteTempThumbnail(tempThumbName); } catch (IOException ignored) {}
            throw e;
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