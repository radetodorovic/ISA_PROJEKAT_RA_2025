package com.isa.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

@Service
public class ThumbnailCompressionService {
    private static final Logger log = LoggerFactory.getLogger(ThumbnailCompressionService.class);

    @Value("${file.thumbnail.dir:uploads/thumbnails}")
    private String thumbnailDir;

    @Value("${app.thumbnail.compression.output.dir:uploads/thumbnails/compressed}")
    private String outputDir;

    @Value("${app.thumbnail.compression.enabled:true}")
    private boolean enabled;

    @Value("${app.thumbnail.compression.age-days:30}")
    private int ageDays;

    @Value("${app.thumbnail.compression.jpeg-quality:0.75}")
    private float jpegQuality;

    @Value("${app.thumbnail.compression.png-compression-level:6}")
    private int pngCompressionLevel;

    @Scheduled(cron = "${app.thumbnail.compression.cron:0 0 2 * * *}")
    public void compressOldThumbnails() {
        if (!enabled) {
            return;
        }

        Path sourceDir = Paths.get(thumbnailDir);
        Path targetDir = Paths.get(outputDir);
        try {
            if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
                return;
            }
            Files.createDirectories(targetDir);
        } catch (Exception e) {
            log.warn("Thumbnail compression skipped: {}", e.getMessage());
            return;
        }

        Instant cutoff = Instant.now().minus(ageDays, ChronoUnit.DAYS);

        try (var stream = Files.list(sourceDir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    FileTime lastModified = Files.getLastModifiedTime(path);
                    if (lastModified.toInstant().isAfter(cutoff)) {
                        return;
                    }

                    String filename = path.getFileName().toString();
                    String ext = getExtension(filename);
                    if (ext == null) {
                        return;
                    }

                    String format = getFormatFromExtension(ext);
                    if (format == null) {
                        log.info("Skipping unsupported thumbnail format: {}", filename);
                        return;
                    }

                    Path outputPath = targetDir.resolve(filename);
                    if (Files.exists(outputPath)) {
                        FileTime compressedTime = Files.getLastModifiedTime(outputPath);
                        if (!compressedTime.toInstant().isBefore(lastModified.toInstant())) {
                            return;
                        }
                    }

                    BufferedImage image = ImageIO.read(path.toFile());
                    if (image == null) {
                        log.warn("Failed to read thumbnail: {}", filename);
                        return;
                    }

                    boolean ok = writeCompressed(image, format, outputPath);
                    if (ok) {
                        log.info("Compressed thumbnail: {} -> {}", filename, outputPath);
                    }
                } catch (Exception e) {
                    log.warn("Compression failed for {}: {}", path.getFileName(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Thumbnail compression scan failed: {}", e.getMessage());
        }
    }

    private boolean writeCompressed(BufferedImage image, String format, Path outputPath) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
            if (!writers.hasNext()) {
                return false;
            }
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();

            if ("jpeg".equals(format) && param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                float quality = Math.max(0.1f, Math.min(1.0f, jpegQuality));
                param.setCompressionQuality(quality);
            } else if ("png".equals(format) && param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                float level = Math.max(0, Math.min(9, pngCompressionLevel)) / 9.0f;
                param.setCompressionQuality(level);
            }

            try (ImageOutputStream out = ImageIO.createImageOutputStream(outputPath.toFile())) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(image, null, null), param);
                writer.dispose();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private String getFormatFromExtension(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "jpeg";
            case "png" -> "png";
            default -> null;
        };
    }
}
