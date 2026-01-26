package com.isa.backend.service;

import com.isa.backend.model.TrendingVideo;
import com.isa.backend.model.VideoCommentEvent;
import com.isa.backend.model.VideoLikeEvent;
import com.isa.backend.model.VideoPost;
import com.isa.backend.model.VideoViewEvent;
import com.isa.backend.repository.TrendingVideoRepository;
import com.isa.backend.repository.VideoCommentEventRepository;
import com.isa.backend.repository.VideoLikeEventRepository;
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
public class TrendingPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(TrendingPipelineService.class);
    private static final String GLOBAL_LOCATION = "global";

    @Value("${app.trending.window-days:7}")
    private int windowDays;

    @Value("${app.trending.weight.view:1}")
    private int viewWeight;

    @Value("${app.trending.weight.like:3}")
    private int likeWeight;

    @Value("${app.trending.weight.comment:5}")
    private int commentWeight;

    @Autowired
    private VideoViewEventRepository videoViewEventRepository;

    @Autowired
    private VideoLikeEventRepository videoLikeEventRepository;

    @Autowired
    private VideoCommentEventRepository videoCommentEventRepository;

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired
    private TrendingVideoRepository trendingVideoRepository;

    @Scheduled(cron = "${app.trending.cron:0 0 2 * * *}")
    @Transactional
    public void runDailyPipeline() {
        runPipeline(LocalDate.now());
    }

    @Transactional
    public void runPipeline(LocalDate runDate) {
        LocalDate startDate = runDate.minusDays(windowDays - 1);
        LocalDate endDate = runDate;
        if (endDate.isBefore(startDate)) {
            logger.warn("Trending pipeline skipped due to invalid date window.");
            return;
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<VideoViewEvent> viewEvents = videoViewEventRepository.findByViewedAtBetween(start, end);
        List<VideoLikeEvent> likeEvents = videoLikeEventRepository.findByCreatedAtBetween(start, end);
        List<VideoCommentEvent> commentEvents = videoCommentEventRepository.findByCreatedAtBetween(start, end);

        if (viewEvents.isEmpty() && likeEvents.isEmpty() && commentEvents.isEmpty()) {
            logger.info("Trending pipeline: no events in the last {} days.", windowDays);
            return;
        }

        Map<Long, Map<LocalDate, Long>> viewCounts = countByVideoAndDay(viewEvents, runDate);
        Map<Long, Map<LocalDate, Long>> likeCounts = countByVideoAndDay(likeEvents, runDate);
        Map<Long, Map<LocalDate, Long>> commentCounts = countByVideoAndDay(commentEvents, runDate);

        Map<Long, Long> scoresByVideo = new HashMap<>();
        applyWeightedScores(scoresByVideo, viewCounts, runDate, viewWeight);
        applyWeightedScores(scoresByVideo, likeCounts, runDate, likeWeight);
        applyWeightedScores(scoresByVideo, commentCounts, runDate, commentWeight);

        if (scoresByVideo.isEmpty()) {
            logger.info("Trending pipeline: no videos reached a positive score.");
            return;
        }

        Set<Long> videoIds = scoresByVideo.keySet();
        Map<Long, VideoPost> videoMap = videoPostRepository.findAllById(videoIds)
                .stream()
                .collect(Collectors.toMap(VideoPost::getId, vp -> vp));

        Map<String, List<VideoScore>> scoresByLocation = new HashMap<>();
        for (Map.Entry<Long, Long> entry : scoresByVideo.entrySet()) {
            VideoPost video = videoMap.get(entry.getKey());
            if (video == null) {
                continue;
            }
            String normalized = normalizeLocation(video.getLocation());
            String locationKey = normalized != null ? normalized : GLOBAL_LOCATION;

            scoresByLocation
                    .computeIfAbsent(locationKey, k -> new ArrayList<>())
                    .add(new VideoScore(entry.getKey(), entry.getValue()));

            scoresByLocation
                    .computeIfAbsent(GLOBAL_LOCATION, k -> new ArrayList<>())
                    .add(new VideoScore(entry.getKey(), entry.getValue()));
        }

        LocalDateTime runAt = LocalDateTime.now();
        List<TrendingVideo> toSave = new ArrayList<>();
        for (Map.Entry<String, List<VideoScore>> entry : scoresByLocation.entrySet()) {
            List<VideoScore> top = entry.getValue().stream()
                    .sorted((a, b) -> {
                        int byScore = Long.compare(b.score, a.score);
                        return byScore != 0 ? byScore : Long.compare(a.videoId, b.videoId);
                    })
                    .limit(3)
                    .toList();

            int rank = 1;
            for (VideoScore vs : top) {
                TrendingVideo tv = new TrendingVideo();
                tv.setRunAt(runAt);
                tv.setLocation(entry.getKey());
                tv.setRank(rank++);
                tv.setVideoId(vs.videoId);
                tv.setScore(vs.score);
                toSave.add(tv);
            }
        }

        if (!toSave.isEmpty()) {
            trendingVideoRepository.saveAll(toSave);
            logger.info("Trending pipeline saved {} entries.", toSave.size());
        }
    }

    public String normalizeLocation(String location) {
        if (location == null) return null;
        String trimmed = location.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.toLowerCase();
    }

    private Map<Long, Map<LocalDate, Long>> countByVideoAndDay(List<?> events, LocalDate runDate) {
        Map<Long, Map<LocalDate, Long>> counts = new HashMap<>();
        for (Object raw : events) {
            Long videoId;
            LocalDate day;
            if (raw instanceof VideoViewEvent v) {
                videoId = v.getVideoId();
                day = v.getViewedAt().toLocalDate();
            } else if (raw instanceof VideoLikeEvent l) {
                videoId = l.getVideoId();
                day = l.getCreatedAt().toLocalDate();
            } else if (raw instanceof VideoCommentEvent c) {
                videoId = c.getVideoId();
                day = c.getCreatedAt().toLocalDate();
            } else {
                continue;
            }
            long daysAgo = ChronoUnit.DAYS.between(day, runDate);
            if (daysAgo < 0 || daysAgo > (windowDays - 1)) {
                continue;
            }
            counts
                    .computeIfAbsent(videoId, id -> new HashMap<>())
                    .merge(day, 1L, Long::sum);
        }
        return counts;
    }

    private void applyWeightedScores(
            Map<Long, Long> scoresByVideo,
            Map<Long, Map<LocalDate, Long>> countsByVideo,
            LocalDate runDate,
            int baseWeight
    ) {
        for (Map.Entry<Long, Map<LocalDate, Long>> entry : countsByVideo.entrySet()) {
            long score = 0L;
            for (Map.Entry<LocalDate, Long> perDay : entry.getValue().entrySet()) {
                long daysAgo = ChronoUnit.DAYS.between(perDay.getKey(), runDate);
                if (daysAgo >= 0 && daysAgo <= (windowDays - 1)) {
                    int weight = (windowDays - (int) daysAgo);
                    score += perDay.getValue() * (long) weight * baseWeight;
                }
            }
            if (score > 0) {
                scoresByVideo.merge(entry.getKey(), score, Long::sum);
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
