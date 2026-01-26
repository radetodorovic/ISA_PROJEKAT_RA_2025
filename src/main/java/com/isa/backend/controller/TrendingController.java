package com.isa.backend.controller;

import com.isa.backend.dto.TrendingVideoDTO;
import com.isa.backend.dto.VideoPostDTO;
import com.isa.backend.model.TrendingVideo;
import com.isa.backend.model.User;
import com.isa.backend.repository.TrendingVideoRepository;
import com.isa.backend.service.TrendingPipelineService;
import com.isa.backend.service.UserService;
import com.isa.backend.service.VideoPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/trending")
@CrossOrigin(origins = "*")
public class TrendingController {

    @Autowired
    private TrendingVideoRepository trendingVideoRepository;

    @Autowired
    private VideoPostService videoPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private TrendingPipelineService trendingPipelineService;

    @GetMapping
    public ResponseEntity<?> getTrending(
            @RequestParam(value = "location", required = false) String location,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Morate biti prijavljeni da biste videli trending.");
        }

        String normalized = null;
        if (location != null && !location.trim().isEmpty()) {
            normalized = trendingPipelineService.normalizeLocation(location);
        } else {
            User user = userService.findByEmail(principal.getName());
            normalized = trendingPipelineService.normalizeLocation(user.getAddress());
        }

        if (normalized == null) {
            normalized = "global";
        }

        List<TrendingVideo> items = trendingVideoRepository.findLatestByLocation(normalized);
        if (items.isEmpty() && !"global".equals(normalized)) {
            items = trendingVideoRepository.findLatestByLocation("global");
        }

        List<TrendingVideoDTO> response = new ArrayList<>();
        for (TrendingVideo item : items) {
            VideoPostDTO videoDto = videoPostService.getVideoPostById(item.getVideoId(), true);
            TrendingVideoDTO dto = new TrendingVideoDTO();
            dto.setVideo(videoDto);
            dto.setScore(item.getScore());
            dto.setRank(item.getRank());
            dto.setLocation(item.getLocation());
            dto.setRunAt(item.getRunAt());
            response.add(dto);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/run")
    public ResponseEntity<?> runTrendingPipeline(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Morate biti prijavljeni da biste pokrenuli trending pipeline.");
        }
        trendingPipelineService.runPipeline(java.time.LocalDate.now());
        return ResponseEntity.ok("Trending pipeline pokrenut.");
    }
}
