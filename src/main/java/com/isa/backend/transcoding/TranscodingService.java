package com.isa.backend.transcoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class TranscodingService {
    private static final Logger logger = LoggerFactory.getLogger(TranscodingService.class);

    private final Map<String, TranscodingProfile> profiles = Map.of(
            "360p", new TranscodingProfile("360p", "640:-2", "800k", "96k"),
            "720p", new TranscodingProfile("720p", "1280:-2", "2500k", "128k"),
            "1080p", new TranscodingProfile("1080p", "1920:-2", "4500k", "192k")
    );

    @Value("${app.transcode.enabled:true}")
    private boolean enabled;

    @Value("${app.transcode.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    public void processJob(TranscodingJob job) {
        if (!enabled) {
            logger.info("Transcoding disabled. Skipping job {}", job.getId());
            return;
        }

        Path input = Paths.get(job.getInputPath());
        if (!Files.exists(input)) {
            logger.warn("Transcoding input not found: {}", input.toAbsolutePath());
            return;
        }

        Path outputDir = Paths.get(job.getOutputDir());
        try {
            Files.createDirectories(outputDir);
        } catch (Exception e) {
            logger.error("Failed to create output dir: {}", outputDir.toAbsolutePath(), e);
            return;
        }

        for (String profileName : job.getProfiles()) {
            TranscodingProfile profile = profiles.get(profileName);
            if (profile == null) {
                logger.warn("Unknown transcode profile '{}', skipping.", profileName);
                continue;
            }

            String outputName = buildOutputName(input.getFileName().toString(), profile.getName());
            Path outputPath = outputDir.resolve(outputName);

            List<String> command = List.of(
                    ffmpegPath,
                    "-y",
                    "-i", input.toAbsolutePath().toString(),
                    "-vf", "scale=" + profile.getScale(),
                    "-c:v", "libx264",
                    "-b:v", profile.getVideoBitrate(),
                    "-c:a", "aac",
                    "-b:a", profile.getAudioBitrate(),
                    outputPath.toAbsolutePath().toString()
            );

            runFfmpeg(command, job.getId(), profile.getName());
        }
    }

    private String buildOutputName(String inputFilename, String suffix) {
        int dot = inputFilename.lastIndexOf('.');
        String base = dot > 0 ? inputFilename.substring(0, dot) : inputFilename;
        return base + "_" + suffix + ".mp4";
    }

    private void runFfmpeg(List<String> command, String jobId, String profileName) {
        try {
            logger.info("Transcoding job {} -> profile {}", jobId, profileName);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Consume output to prevent buffer blocking.
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Transcoding job {} profile {} exited with code {}", jobId, profileName, exitCode);
            } else {
                logger.info("Transcoding job {} profile {} completed.", jobId, profileName);
            }
        } catch (Exception e) {
            logger.error("Transcoding job {} profile {} failed.", jobId, profileName, e);
        }
    }
}
