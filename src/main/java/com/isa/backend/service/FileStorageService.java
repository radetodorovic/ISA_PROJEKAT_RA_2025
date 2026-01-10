package com.isa.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class FileStorageService {

    @Value("${file.upload.dir}")
    private String videoUploadDir;

    @Value("${file.thumbnail.dir}")
    private String thumbnailUploadDir;

    // Temp subfolders
    private static final String VIDEO_TEMP_SUBDIR = "temp";
    private static final String THUMB_TEMP_SUBDIR = "temp";

    // Executor for timed file saves
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    /**
     * Čuva video fajl na server
     */
    public String saveVideoFile(MultipartFile file) throws IOException {
        // Kreiraj folder ako ne postoji
        Path uploadPath = Paths.get(videoUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = getExtensionOrDefault(originalFilename, "");
        String uniqueFilename = UUID.randomUUID() + fileExtension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFilename;
    }

    /**
     * Čuva thumbnail (sliku) na server
     */
    public String saveThumbnailFile(MultipartFile file) throws IOException {
        // Kreiraj folder ako ne postoji
        Path uploadPath = Paths.get(thumbnailUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = getExtensionOrDefault(originalFilename, "");
        String uniqueFilename = UUID.randomUUID() + fileExtension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFilename;
    }

    private String getExtensionOrDefault(String originalFilename, String defaultExt) {
        if (originalFilename == null) return defaultExt;
        int idx = originalFilename.lastIndexOf('.');
        if (idx == -1) return defaultExt;
        return originalFilename.substring(idx);
    }

    // New: save thumbnail into temp folder (used before transaction commit)
    public String saveThumbnailFileToTempWithFinalName(MultipartFile file, String finalFilename) throws IOException {
        Path tempDir = Paths.get(thumbnailUploadDir).resolve(THUMB_TEMP_SUBDIR);
        if (!Files.exists(tempDir)) Files.createDirectories(tempDir);
        String tempName = finalFilename + ".part";
        Path tempPath = tempDir.resolve(tempName);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempName; // return temp filename (with .part)
    }


    // New: save video into temp folder with timeout (ms)
    public String saveVideoFileToTempWithFinalName(MultipartFile file, String finalFilename, long timeoutMs) throws IOException {
        Path tempDir = Paths.get(videoUploadDir).resolve(VIDEO_TEMP_SUBDIR);
        if (!Files.exists(tempDir)) Files.createDirectories(tempDir);
        String tempName = finalFilename + ".part";
        Path tempPath = tempDir.resolve(tempName);

        Callable<String> writeTask = () -> {
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempName;
        };

        Future<String> future = ioExecutor.submit(writeTask);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            // cancel and cleanup temp file if exists
            future.cancel(true);
            try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
            throw new IOException("Upload timeout");
        } catch (ExecutionException ee) {
            try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
            Throwable cause = ee.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw new IOException("IO error during upload: " + (cause != null ? cause.getMessage() : ee.getMessage()), ee);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Upload interrupted", ie);
        }
    }

    // New: move temp video to final location (atomic if possible)
    public void moveTempVideoToFinal(String tempName, String finalFilename) throws IOException {
        Path tempPath = Paths.get(videoUploadDir).resolve(VIDEO_TEMP_SUBDIR).resolve(tempName);
        Path finalDir = Paths.get(videoUploadDir);
        if (!Files.exists(finalDir)) Files.createDirectories(finalDir);
        Path finalPath = finalDir.resolve(finalFilename);
        Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    public void moveTempThumbnailToFinal(String tempName, String finalFilename) throws IOException {
        Path tempPath = Paths.get(thumbnailUploadDir).resolve(THUMB_TEMP_SUBDIR).resolve(tempName);
        Path finalDir = Paths.get(thumbnailUploadDir);
        if (!Files.exists(finalDir)) Files.createDirectories(finalDir);
        Path finalPath = finalDir.resolve(finalFilename);
        Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    // New: delete temp files

    public void deleteTempVideo(String tempName) throws IOException {
        Path tempPath = Paths.get(videoUploadDir).resolve(VIDEO_TEMP_SUBDIR).resolve(tempName);
        Files.deleteIfExists(tempPath);
    }

    public void deleteTempThumbnail(String tempName) throws IOException {
        Path tempPath = Paths.get(thumbnailUploadDir).resolve(THUMB_TEMP_SUBDIR).resolve(tempName);
        Files.deleteIfExists(tempPath);
    }

    // New: delete final files (kept existing behavior but ensures paths)
    public void deleteVideoFile(String filename) throws IOException {
        Path filePath = Paths.get(videoUploadDir).resolve(filename);
        Files.deleteIfExists(filePath);
    }

    public void deleteThumbnailFile(String filename) throws IOException {
        Path filePath = Paths.get(thumbnailUploadDir).resolve(filename);
        Files.deleteIfExists(filePath);
    }

    // New: check mp4 signature (ftyp) by reading first bytes
    public boolean isMp4(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 12) return false;
            // look for 'ftyp' within first 12 bytes
            String s = new String(header, 4, 4);
            return "ftyp".equals(s);
        } catch (IOException e) {
            return false;
        }
    }

    // New: cached thumbnail bytes
    @Cacheable(value = "thumbnails", key = "#filename")
    public byte[] getThumbnailBytes(String filename) throws IOException {
        Path path = Paths.get(thumbnailUploadDir).resolve(filename);
        return Files.readAllBytes(path);
    }
}