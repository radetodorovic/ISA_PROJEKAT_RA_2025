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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/trending")
@CrossOrigin(origins = "*")
public class TrendingController {

    private static final Logger log = LoggerFactory.getLogger(TrendingController.class);
    private static final String GLOBAL_LOCATION = "global";

    @Autowired
    private TrendingVideoRepository trendingVideoRepository;

    @Autowired
    private VideoPostService videoPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private TrendingPipelineService trendingPipelineService;

    @Value("${app.trending.radius-default-meters:2000}")
    private int defaultRadiusMeters;

    @GetMapping
    public ResponseEntity<?> getTrending(
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "lat", required = false) Double latitude,
            @RequestParam(value = "lng", required = false) Double longitude,
            @RequestParam(value = "radiusMeters", required = false) Integer radiusMeters,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Morate biti prijavljeni da biste videli trending.");
        }

        if ((latitude == null) != (longitude == null)) {
            return ResponseEntity.badRequest()
                    .body("Latitude and longitude must be provided together.");
        }

        boolean useGeo = latitude != null && longitude != null;

        double effectiveRadius = radiusMeters != null ? radiusMeters : defaultRadiusMeters;
        if (useGeo && effectiveRadius <= 0) {
            return ResponseEntity.badRequest()
                    .body("radiusMeters must be a positive number.");
        }

        String normalized = null;
        if (location != null && !location.trim().isEmpty()) {
            normalized = trendingPipelineService.normalizeLocation(location);
        } else {
            User user = userService.findByEmail(principal.getName());
            normalized = trendingPipelineService.normalizeLocation(user.getAddress());
        }

        if (normalized == null) {
            normalized = GLOBAL_LOCATION;
        }

        List<TrendingVideo> items;
        if (useGeo) {
            try {
                items = trendingVideoRepository.findLatestByLocationWithinRadius(
                        GLOBAL_LOCATION, latitude, longitude, (int) effectiveRadius
                );
            } catch (Exception ex) {
                items = trendingVideoRepository.findLatestByLocation(GLOBAL_LOCATION);
            }
        } else {
            items = trendingVideoRepository.findLatestByLocation(normalized);
            if (items.isEmpty() && !GLOBAL_LOCATION.equals(normalized)) {
                items = trendingVideoRepository.findLatestByLocation(GLOBAL_LOCATION);
            }
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
        long startTime = System.currentTimeMillis();
        trendingPipelineService.runPipeline(java.time.LocalDate.now());
        long endTime = System.currentTimeMillis();
        log.info("Trending pipeline executed in {}ms", endTime - startTime);
        return ResponseEntity.ok("Trending pipeline pokrenut.");
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTrendingStats() {
        long startTime = System.currentTimeMillis();

        // Trending logika - brojimo sve trending videe
        List<TrendingVideo> allTrending = trendingVideoRepository.findLatestByLocation(GLOBAL_LOCATION);

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        Map<String, Object> stats = new HashMap<>();
        stats.put("responseTimeMs", responseTime);
        stats.put("timestamp", LocalDateTime.now());
        stats.put("totalTrendingVideos", allTrending.size());

        return ResponseEntity.ok(stats);
    }

}
