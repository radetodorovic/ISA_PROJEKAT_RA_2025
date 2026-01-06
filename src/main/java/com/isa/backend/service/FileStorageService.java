package com.isa.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload.dir}")
    private String videoUploadDir;

    @Value("${file.thumbnail.dir}")
    private String thumbnailUploadDir;

    /**
     * Čuva video fajl na server
     */
    public String saveVideoFile(MultipartFile file) throws IOException {
        // Kreiraj folder ako ne postoji
        Path uploadPath = Paths.get(videoUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generiši jedinstveno ime fajla
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Sačuvaj fajl
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

        // Generiši jedinstveno ime fajla
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Sačuvaj fajl
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFilename;
    }

    /**
     * Briše video fajl sa servera
     */
    public void deleteVideoFile(String filename) throws IOException {
        Path filePath = Paths.get(videoUploadDir).resolve(filename);
        Files.deleteIfExists(filePath);
    }

    /**
     * Briše thumbnail fajl sa servera
     */
    public void deleteThumbnailFile(String filename) throws IOException {
        Path filePath = Paths.get(thumbnailUploadDir).resolve(filename);
        Files.deleteIfExists(filePath);
    }
}