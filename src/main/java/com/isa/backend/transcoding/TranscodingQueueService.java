package com.isa.backend.transcoding;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class TranscodingQueueService {
    private static final Logger logger = LoggerFactory.getLogger(TranscodingQueueService.class);

    private final BlockingQueue<TranscodingJob> queue = new LinkedBlockingQueue<>();
    private final TranscodingService transcodingService;
    private ExecutorService executor;

    @Value("${app.transcode.consumer-count:2}")
    private int consumerCount;

    @Value("${app.transcode.default-profiles:360p,720p}")
    private String defaultProfilesCsv;

    @Value("${app.transcode.output.dir:uploads/transcoded}")
    private String outputDir;

    public TranscodingQueueService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    @PostConstruct
    public void startConsumers() {
        executor = Executors.newFixedThreadPool(consumerCount);
        for (int i = 0; i < consumerCount; i++) {
            int consumerId = i + 1;
            executor.submit(() -> consumeLoop(consumerId));
        }
        logger.info("Transcoding queue started with {} consumers.", consumerCount);
    }

    @PreDestroy
    public void stopConsumers() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void enqueue(String inputPath, List<String> requestedProfiles) {
        List<String> profiles = resolveProfiles(requestedProfiles);
        TranscodingJob job = new TranscodingJob(
                UUID.randomUUID().toString(),
                inputPath,
                outputDir,
                profiles,
                Instant.now()
        );
        queue.offer(job);
        logger.info("Queued transcoding job {} for {}", job.getId(), inputPath);
    }

    private List<String> resolveProfiles(List<String> requestedProfiles) {
        if (requestedProfiles != null && !requestedProfiles.isEmpty()) {
            return requestedProfiles;
        }
        String[] parts = defaultProfilesCsv.split(",");
        List<String> defaults = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                defaults.add(trimmed);
            }
        }
        return defaults.isEmpty() ? Arrays.asList("360p", "720p") : defaults;
    }

    private void consumeLoop(int consumerId) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                TranscodingJob job = queue.take();
                logger.info("Consumer {} picked job {}", consumerId, job.getId());
                transcodingService.processJob(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Consumer {} failed processing a job.", consumerId, e);
            }
        }
    }
}
