package com.isa.backend.controller;

import com.isa.backend.dto.VideoDTO;
import com.isa.backend.service.UserService;
import com.isa.backend.service.VideoPostService;
import com.isa.backend.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "http://localhost:4200")
public class VideosController {

    @Autowired
    private VideoPostService videoPostService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<VideoDTO>> listVideos() {
        List<VideoDTO> all = videoPostService.getAllVideoPosts();
        return ResponseEntity.ok(all);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(@RequestParam("title") String title,
                                         @RequestParam("description") String description,
                                         @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
                                         @RequestParam(value = "video", required = false) MultipartFile video,
                                         Principal principal,
                                         HttpServletRequest request) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status",401,"message","Morate biti prijavljeni da biste postavili video."));
            }

            // Resolve userId from principal/email -> attempt to use UserService
            Long userId = null;
            String uploaderEmail = principal.getName();
            try {
                User user = userService.findByEmail(uploaderEmail);
                if (user != null) userId = user.getId();
            } catch (RuntimeException ignored) {
                // leave userId null if not resolvable
            }

            VideoDTO created = videoPostService.createVideoPost(title, description, video, thumbnail, userId, uploaderEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status",400,"message",e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status",500,"message","Upload failed: "+e.getMessage()));
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideoAlias(@RequestParam("title") String title,
                                              @RequestParam("description") String description,
                                              @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
                                              @RequestParam(value = "video", required = false) MultipartFile video,
                                              Principal principal,
                                              HttpServletRequest request) {
        return uploadVideo(title, description, thumbnail, video, principal, request);
    }
}
