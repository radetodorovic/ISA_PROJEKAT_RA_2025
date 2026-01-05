package com.isa.backend.controller;

import com.isa.backend.dto.CommentDTO;
import com.isa.backend.dto.VideoPostDTO;
import com.isa.backend.model.User;
import com.isa.backend.model.VideoPost;
import com.isa.backend.service.CommentService;
import com.isa.backend.service.UserService;
import com.isa.backend.service.VideoPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*") // Za frontend
public class VideoPostController {

    @Autowired
    private VideoPostService videoPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    /**
     * 🎬 Endpoint za kreiranje video objave
     * POST /api/videos/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("tags") Set<String> tags,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "location", required = false) String location,
            Principal principal
    ) {
        try {
            // Require authentication
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Morate biti prijavljeni da biste postavili video.");
            }

            // Resolve userId from principal (we store email as principal)
            User user = userService.findByEmail(principal.getName());
            Long userId = user.getId();

            // Validacija
            if (video.isEmpty() || thumbnail.isEmpty()) {
                return ResponseEntity.badRequest().body("Video i thumbnail su obavezni!");
            }

            String contentType = video.getContentType();
            if (contentType == null || !contentType.equals("video/mp4")) {
                return ResponseEntity.badRequest().body("Video mora biti u MP4 formatu!");
            }

            // Kreiranje video objave
            VideoPostDTO createdPost = videoPostService.createVideoPost(
                    title, description, tags, thumbnail, video, location, userId
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload nije uspeo: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Greška: " + e.getMessage());
        }
    }

    /**
     * Endpoint za lajk (zahteva autentifikaciju)
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeVideo(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Morate se prijaviti da biste lajkovali.");
        }
        // For now, increment like count in service (not implemented fully)
        try {
            videoPostService.incrementLikeCount(id);
            return ResponseEntity.ok("Lajk registrovan");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Endpoint za komentar (zahteva autentifikaciju)
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> commentVideo(@PathVariable Long id, @RequestParam("text") String text, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Morate se prijaviti da biste komentarisali.");
        }

        try {
            User user = userService.findByEmail(principal.getName());
            CommentDTO saved = commentService.addComment(id, user.getId(), text);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        try {
            List<CommentDTO> comments = commentService.getCommentsForVideo(id);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 📋 Vraća sve video objave
     * GET /api/videos
     */
    @GetMapping
    public ResponseEntity<List<VideoPostDTO>> getAllVideos(Principal principal) {
        List<VideoPostDTO> videos = videoPostService.getAllVideoPosts(principal != null);
        return ResponseEntity.ok(videos);
    }

    /**
     * 🎥 Vraća jednu video objavu po ID-u
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVideoById(@PathVariable Long id, Principal principal) {
        try {
            VideoPostDTO video = videoPostService.getVideoPostById(id, principal != null);
            return ResponseEntity.ok(video);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 🖼️ Vraća thumbnail sliku
     */
    @GetMapping("/thumbnail/{filename:.+}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/thumbnails").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vraća komentare za video na osnovu filename-a koji se koristi u stream URL-u
     * GET /api/videos/stream/{filename}/comments
     */
    @GetMapping("/stream/{filename:.+}/comments")
    public ResponseEntity<?> getCommentsByFilename(@PathVariable String filename) {
        try {
            VideoPost vp = videoPostService.getVideoPostByVideoPath(filename);
            List<CommentDTO> comments = commentService.getCommentsForVideo(vp.getId());
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 🎬 Stream-uje video fajl
     */
    @GetMapping("/stream/{filename:.+}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String filename, HttpServletRequest request) {
        try {
            // Increment view count only for initial requests (no Range header or Range starting at 0)
            String range = request.getHeader("Range");
            if (range == null || range.startsWith("bytes=0-")) {
                // filename here is the stored unique filename (videoPath)
                videoPostService.incrementViewCountByPath(filename);
            }

            Path filePath = Paths.get("uploads/videos").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("video/mp4"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

