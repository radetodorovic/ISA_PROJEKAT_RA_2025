package com.isa.backend.service;

import com.isa.backend.dto.VideoPostDTO;
import com.isa.backend.model.VideoPost;
import com.isa.backend.model.VideoLikeEvent;
import com.isa.backend.model.VideoViewEvent;
import com.isa.backend.repository.VideoPostRepository;
import com.isa.backend.repository.VideoLikeEventRepository;
import com.isa.backend.repository.VideoViewEventRepository;
import com.isa.backend.transcoding.TranscodingQueueService;
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
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoPostService {

    private static final Logger logger = LoggerFactory.getLogger(VideoPostService.class);

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired
    private VideoLikeEventRepository videoLikeEventRepository;

    @Autowired
    private VideoViewEventRepository videoViewEventRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TranscodingQueueService transcodingQueueService;

    @Value("${file.upload.dir:uploads/videos}")
    private String videoUploadDir;

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
            Double latitude,
            Double longitude,
            Long userId,
            List<String> transcodeProfiles,
            LocalDateTime scheduledAt
    ) throws IOException {

        // Validacija
        if (video.getSize() > 200 * 1024 * 1024) { // 200MB
            throw new IllegalArgumentException("Video fajl je prevelik! Maksimalna veličina je 200MB.");
        }

        if (!fileStorageService.isMp4(video)) {
            throw new IllegalArgumentException("Video mora biti pravi MP4 fajl!");
        }

        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Latitude and longitude must be provided together.");
        }
        if (latitude != null) {
            if (latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90.");
            }
            if (longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180.");
            }
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
            videoPost.setScheduledAt(scheduledAt);
            videoPost.setLocation(location);
            videoPost.setLatitude(latitude);
            videoPost.setLongitude(longitude);
            videoPost.setUserId(userId);

            // Sačuvaj u bazu (još uvek temp fajlovi postoje)
            VideoPost savedPost = videoPostRepository.save(videoPost);

            final String tv = tempVideoName;
            final String tt = tempThumbName;
            final String fv = finalVideoFilename;
            final String ft = finalThumbFilename;
            final List<String> profilesCopy = transcodeProfiles == null ? null : new ArrayList<>(transcodeProfiles);
            // Registruj transaction synchronization: na commit premestiti temp fajlove u finalne, na rollback obrisati temp fajlove
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorageService.moveTempThumbnailToFinal(tt, ft);
                        fileStorageService.moveTempVideoToFinal(tv, fv);
                        Path finalPath = Paths.get(videoUploadDir).resolve(fv).normalize();
                        transcodingQueueService.enqueue(finalPath.toAbsolutePath().toString(), profilesCopy);
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
        return getAllVideoPostsForViewer(null);
    }

    public List<VideoPostDTO> getAllVideoPosts(boolean authenticated) {
        return getAllVideoPostsForViewer(null);
    }

    public List<VideoPostDTO> getAllVideoPostsForViewer(Long viewerUserId) {
        LocalDateTime now = LocalDateTime.now();
        return videoPostRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(vp -> isAvailableForViewer(vp, viewerUserId, now))
                .map(vp -> convertToDTO(vp, viewerUserId != null))
                .collect(Collectors.toList());
    }

    /**
     * Vraća video objavu po ID-u
     */
    public VideoPostDTO getVideoPostById(Long id) {
        return getVideoPostById(id, null);
    }

    public VideoPostDTO getVideoPostById(Long id, Long viewerUserId) {
        VideoPost videoPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena!"));
        if (!isAvailableForViewer(videoPost, viewerUserId, LocalDateTime.now())) {
            throw new RuntimeException("Video objava nije dostupna pre zakazanog vremena!");
        }
        return convertToDTO(videoPost, viewerUserId != null);
    }

    /**
     * Povećava broj pregleda za data videoPath (koristi se u stream endpoint-u)
     * Thread-safe implementacija koristi atomski UPDATE na nivou baze podataka
     */
    @Transactional
    public void incrementViewCountByPath(String videoPath) {
        VideoPost videoPost = videoPostRepository.findByVideoPath(videoPath)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena za dati filename: " + videoPath));
        int updated = videoPostRepository.incrementViewCountById(videoPost.getId());
        if (updated == 0) {
            logger.warn("Video sa videoPath '{}' nije pronađen ili view count nije ažuriran.", videoPath);
            return;
        }
        videoViewEventRepository.save(new VideoViewEvent(videoPost.getId()));
    }

    /**
     * Povećava broj pregleda za dati video ID (thread-safe)
     */
    @Transactional
    public void incrementViewCountById(Long id) {
        int updated = videoPostRepository.incrementViewCountById(id);
        if (updated == 0) {
            throw new RuntimeException("Video objava nije pronađena za dati id: " + id);
        }
        videoViewEventRepository.save(new VideoViewEvent(id));
    }

    /**
     * Vraća VideoPost entitet na osnovu sačuvanog videoPath (koristi se za mapiranje filename -> videoId)
     */
    public VideoPost getVideoPostByVideoPath(String videoPath) {
        return videoPostRepository.findByVideoPath(videoPath)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena za dati filename: " + videoPath));
    }

    public VideoPost getVideoPostByVideoPathForViewer(String videoPath, Long viewerUserId) {
        VideoPost videoPost = videoPostRepository.findByVideoPath(videoPath)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronaÄ‘ena za dati filename: " + videoPath));
        if (!isAvailableForViewer(videoPost, viewerUserId, LocalDateTime.now())) {
            throw new RuntimeException("Video objava nije dostupna pre zakazanog vremena!");
        }
        return videoPost;
    }

    /**
     * Finds DB video posts whose file does not exist on disk.
     * Returns a list of maps: { id, videoPath, expectedPath }
     */
    public List<Map<String, String>> findMissingVideoFiles() {
        List<VideoPost> all = videoPostRepository.findAll();
        List<Map<String, String>> missing = new ArrayList<>();
        for (VideoPost vp : all) {
            try {
                Path expected = Paths.get(videoUploadDir).resolve(vp.getVideoPath()).normalize();
                if (!Files.exists(expected)) {
                    Map<String, String> m = new HashMap<>();
                    m.put("id", vp.getId().toString());
                    m.put("videoPath", vp.getVideoPath());
                    m.put("expectedPath", expected.toAbsolutePath().toString());
                    missing.add(m);
                }
            } catch (Exception ex) {
                // ignore individual errors but continue
            }
        }
        return missing;
    }

    /**
     * Try to reconcile missing video files by matching file sizes in the upload directory.
     * For each VideoPost whose expected file is missing, scan existing files in the upload dir
     * and copy the first file whose size equals the videoSize into the expected filename.
     * Returns a list of maps with reconciliation results.
     */
    public List<Map<String, String>> reconcileMissingVideoFiles() {
        List<VideoPost> all = videoPostRepository.findAll();
        List<Map<String, String>> results = new ArrayList<>();
        Path uploadDir = Paths.get(videoUploadDir);

        for (VideoPost vp : all) {
            Map<String, String> r = new HashMap<>();
            r.put("id", vp.getId().toString());
            r.put("videoPath", vp.getVideoPath());
            try {
                Path expected = uploadDir.resolve(vp.getVideoPath()).normalize();
                if (Files.exists(expected)) {
                    r.put("status", "already_exists");
                    r.put("expectedPath", expected.toAbsolutePath().toString());
                    results.add(r);
                    continue;
                }

                boolean matched = false;
                if (Files.exists(uploadDir) && Files.isDirectory(uploadDir)) {
                    try (java.util.stream.Stream<Path> stream = Files.list(uploadDir)) {
                        for (Path candidate : (Iterable<Path>) stream::iterator) {
                            try {
                                long candidateSize = Files.size(candidate);
                                Long expectedSize = vp.getVideoSize();
                                if (expectedSize != null && candidateSize == expectedSize) {
                                    // copy candidate to expected
                                    Files.copy(candidate, expected, StandardCopyOption.REPLACE_EXISTING);
                                    r.put("status", "copied");
                                    r.put("matchedFile", candidate.getFileName().toString());
                                    r.put("expectedPath", expected.toAbsolutePath().toString());
                                    matched = true;
                                    break;
                                }
                            } catch (Exception ex) {
                                // ignore candidate errors
                            }
                        }
                    }
                }

                if (!matched) {
                    r.put("status", "not_found");
                }
            } catch (Exception ex) {
                r.put("status", "error");
                r.put("error", ex.getMessage() == null ? "unknown" : ex.getMessage());
            }
            results.add(r);
        }

        return results;
    }

    /**
     * Povećava broj lajkova za dati video ID
     */
    @Transactional
    public void incrementLikeCount(Long id) {
        VideoPost videoPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video objava nije pronađena za dati id: " + id));
        videoPost.setLikeCount(videoPost.getLikeCount() + 1);
        videoPostRepository.save(videoPost);
        videoLikeEventRepository.save(new VideoLikeEvent(id));
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
        dto.setScheduledAt(videoPost.getScheduledAt());
        dto.setLocation(videoPost.getLocation());
        dto.setLatitude(videoPost.getLatitude());
        dto.setLongitude(videoPost.getLongitude());
        dto.setUserId(videoPost.getUserId());
        dto.setViewCount(videoPost.getViewCount());
        dto.setLikeCount(videoPost.getLikeCount());
        dto.setCommentCount(videoPost.getCommentCount());

        // If the caller is authenticated, enable like/comment actions on the DTO
        dto.setCanLike(authenticated);
        dto.setCanComment(authenticated);

        return dto;
    }

    private boolean isAvailableForViewer(VideoPost videoPost, Long viewerUserId, LocalDateTime now) {
        LocalDateTime scheduledAt = videoPost.getScheduledAt();
        boolean published = scheduledAt == null || !scheduledAt.isAfter(now);
        if (published) {
            return true;
        }
        return viewerUserId != null && viewerUserId.equals(videoPost.getUserId());
    }
}




