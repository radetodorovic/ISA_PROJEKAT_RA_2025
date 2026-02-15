package com.isa.backend.service;

import com.isa.backend.model.PopularVideo;
import com.isa.backend.model.VideoPost;
import com.isa.backend.model.VideoViewEvent;
import com.isa.backend.repository.PopularVideoRepository;
import com.isa.backend.repository.VideoPostRepository;
import com.isa.backend.repository.VideoViewEventRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PopularPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(PopularPipelineService.class);

    @Value("${app.popular.window-days:7}")
    private int windowDays;

    @Autowired
    private VideoViewEventRepository videoViewEventRepository;

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired
    private PopularVideoRepository popularVideoRepository;

    @Scheduled(cron = "${app.popular.cron:0 30 2 * * *}")
    @Transactional
    public void runDailyPipeline() {
        runPipeline(LocalDate.now());
    }

    @Transactional
    public void runPipeline(LocalDate runDate) {
        LocalDate startDate = runDate.minusDays(windowDays - 1);
        LocalDate endDate = runDate;
        if (endDate.isBefore(startDate)) {
            logger.warn("Popular pipeline skipped due to invalid date window.");
            return;
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<VideoViewEvent> viewEvents = videoViewEventRepository.findByViewedAtBetween(start, end);
        if (viewEvents.isEmpty()) {
            logger.info("Popular pipeline: no view events in the last {} days.", windowDays);
            return;
        }

        Map<Long, Map<LocalDate, Long>> viewCounts = countByVideoAndDay(viewEvents, runDate);
        Map<Long, Long> scoresByVideo = new HashMap<>();
        applyWeightedScores(scoresByVideo, viewCounts, runDate);

        if (scoresByVideo.isEmpty()) {
            logger.info("Popular pipeline: no videos reached a positive score.");
            return;
        }

        Set<Long> videoIds = scoresByVideo.keySet();
        Map<Long, VideoPost> videoMap = videoPostRepository.findAllById(videoIds)
                .stream()
                .collect(Collectors.toMap(VideoPost::getId, vp -> vp));

        List<VideoScore> ranked = scoresByVideo.entrySet().stream()
                .filter(entry -> videoMap.containsKey(entry.getKey()))
                .map(entry -> new VideoScore(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> {
                    int byScore = Long.compare(b.score, a.score);
                    return byScore != 0 ? byScore : Long.compare(a.videoId, b.videoId);
                })
                .limit(3)
                .toList();

        if (ranked.isEmpty()) {
            logger.info("Popular pipeline: no valid videos found for ranking.");
            return;
        }

        LocalDateTime runAt = LocalDateTime.now();
        List<PopularVideo> toSave = new ArrayList<>();
        int rank = 1;
        for (VideoScore vs : ranked) {
            PopularVideo pv = new PopularVideo();
            pv.setRunAt(runAt);
            pv.setRank(rank++);
            pv.setVideoId(vs.videoId);
            pv.setScore(vs.score);
            toSave.add(pv);
        }

        popularVideoRepository.saveAll(toSave);
        logger.info("Popular pipeline saved {} entries.", toSave.size());
    }

    private Map<Long, Map<LocalDate, Long>> countByVideoAndDay(List<VideoViewEvent> events, LocalDate runDate) {
        Map<Long, Map<LocalDate, Long>> counts = new HashMap<>();
        for (VideoViewEvent event : events) {
            LocalDate day = event.getViewedAt().toLocalDate();
            long daysAgo = ChronoUnit.DAYS.between(day, runDate);
            if (daysAgo < 0 || daysAgo > (windowDays - 1)) {
                continue;
            }
            counts
                    .computeIfAbsent(event.getVideoId(), id -> new HashMap<>())
                    .merge(day, 1L, Long::sum);
        }
        return counts;
    }

    private void applyWeightedScores(
            Map<Long, Long> scoresByVideo,
            Map<Long, Map<LocalDate, Long>> countsByVideo,
            LocalDate runDate
    ) {
        for (Map.Entry<Long, Map<LocalDate, Long>> entry : countsByVideo.entrySet()) {
            long score = 0L;
            for (Map.Entry<LocalDate, Long> perDay : entry.getValue().entrySet()) {
                long daysAgo = ChronoUnit.DAYS.between(perDay.getKey(), runDate);
                if (daysAgo >= 0 && daysAgo <= (windowDays - 1)) {
                    int weight = windowDays - (int) daysAgo;
                    score += perDay.getValue() * (long) weight;
                }
            }
            if (score > 0) {
                scoresByVideo.put(entry.getKey(), score);
            }
        }
    }

    private static class VideoScore {
        private final Long videoId;
        private final Long score;

        private VideoScore(Long videoId, Long score) {
            this.videoId = videoId;
            this.score = score;
        }
    }
}
