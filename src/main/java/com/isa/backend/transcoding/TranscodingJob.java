package com.isa.backend.transcoding;

import java.time.Instant;
import java.util.List;

public class TranscodingJob {
    private final String id;
    private final String inputPath;
    private final String outputDir;
    private final List<String> profiles;
    private final Instant createdAt;

    public TranscodingJob(String id, String inputPath, String outputDir, List<String> profiles, Instant createdAt) {
        this.id = id;
        this.inputPath = inputPath;
        this.outputDir = outputDir;
        this.profiles = profiles;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
