package com.isa.backend.controller;

import com.isa.backend.dto.PopularVideoDTO;
import com.isa.backend.dto.VideoPostDTO;
import com.isa.backend.model.PopularVideo;
import com.isa.backend.model.User;
import com.isa.backend.repository.PopularVideoRepository;
import com.isa.backend.service.PopularPipelineService;
import com.isa.backend.service.UserService;
import com.isa.backend.service.VideoPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/popular")
@CrossOrigin(origins = "*")
public class PopularController {

    @Autowired
    private PopularVideoRepository popularVideoRepository;

    @Autowired
    private VideoPostService videoPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private PopularPipelineService popularPipelineService;

    @GetMapping
    public ResponseEntity<?> getPopularVideos(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Morate biti prijavljeni da biste videli popularne videe.");
        }

        User viewer = userService.findByEmail(principal.getName());
        Long viewerUserId = viewer.getId();

        List<PopularVideo> items = popularVideoRepository.findLatest();
        List<PopularVideoDTO> response = new ArrayList<>();
        for (PopularVideo item : items) {
            VideoPostDTO videoDto;
            try {
                videoDto = videoPostService.getVideoPostById(item.getVideoId(), viewerUserId);
            } catch (RuntimeException ex) {
                continue;
            }
            PopularVideoDTO dto = new PopularVideoDTO();
            dto.setVideo(videoDto);
            dto.setScore(item.getScore());
            dto.setRank(item.getRank());
            dto.setRunAt(item.getRunAt());
            response.add(dto);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/run")
    public ResponseEntity<?> runPopularPipeline(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Morate biti prijavljeni da biste pokrenuli popular pipeline.");
        }
        popularPipelineService.runPipeline(java.time.LocalDate.now());
        return ResponseEntity.ok("Popular pipeline pokrenut.");
    }
}
