package com.example.English.Center.Data.service.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${upload.assignment-dir}")
    private String assignmentDir;

    @Value("${upload.submission-dir:uploads/submissions}")
    private String submissionDir;

    public String storeAssignmentFile(MultipartFile file) throws IOException {
        return storeFile(file, assignmentDir, "/uploads/assignments/");
    }

    public String storeSubmissionFile(MultipartFile file) throws IOException {
        return storeFile(file, submissionDir, "/uploads/submissions/");
    }

    private String storeFile(MultipartFile file, String uploadDir, String urlPrefix) throws IOException {
        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IOException("Invalid file name");
        }

        // Build unique filename: timestamp + uuid + extension
        String ext = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalFileName.substring(dot);
        }
        String uniqueName = Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ext;

        Path filePath = uploadPath.resolve(uniqueName);

        // Copy file to the target location (replace if exists)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return relative URL path used by static resource handler
        return urlPrefix + uniqueName;
    }
}
