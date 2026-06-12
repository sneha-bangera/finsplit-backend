package com.finsplit.finsplit.service;

import com.finsplit.finsplit.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    public String store(MultipartFile file, String userId) {
        if (file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException(
                    "Only JPEG, PNG and WEBP images are allowed");

        if (file.getSize() > MAX_BYTES)
            throw new IllegalArgumentException("File size must be under 5 MB");

        try {
            Path dir = Paths.get(uploadDir, "receipts", userId);
            Files.createDirectories(dir);

            // Generate unique filename to avoid collisions
            String ext      = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;
            Path   target   = dir.resolve(filename);

            Files.copy(file.getInputStream(), target,
                    StandardCopyOption.REPLACE_EXISTING);

            // Return relative path stored in DB
            return "receipts/" + userId + "/" + filename;

        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage());
            throw new RuntimeException("Could not store file. Please try again.");
        }
    }

    public byte[] load(String relativePath) {
        try {
            Path file = Paths.get(uploadDir).resolve(relativePath);
            if (!Files.exists(file))
                throw new ResourceNotFoundException("Receipt not found");
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file");
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null) return;
        try {
            Path file = Paths.get(uploadDir).resolve(relativePath);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", relativePath, e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}