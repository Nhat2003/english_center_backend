package com.example.English.Center.Data.service;

import com.example.English.Center.Data.entity.ClassDocument;
import com.example.English.Center.Data.repository.ClassDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClassDocumentService {

    @Autowired
    private ClassDocumentRepository classDocumentRepository;

    // Base directory for class documents. Files will be stored under {uploadClassDocDir}/{classId}/...
    @Value("${upload.class-documents-dir:uploads/class-documents}")
    private String uploadClassDocDir;

    public ClassDocument uploadDocument(Long classId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safeOriginal = original.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        String fileName = UUID.randomUUID().toString() + "-" + safeOriginal;

        Path baseUploadPath = Paths.get(uploadClassDocDir).toAbsolutePath().normalize();
        // create base and class-specific directory
        Path classDir = baseUploadPath.resolve(String.valueOf(classId));
        Files.createDirectories(classDir);

        Path target = classDir.resolve(fileName);
        file.transferTo(target.toFile());

        ClassDocument document = new ClassDocument();
        document.setClassId(classId);
        document.setFileName(original);
        // store absolute file system path so loadAsResource can find it
        document.setFileUrl(target.toString());

        return classDocumentRepository.save(document);
    }

    public List<ClassDocument> getDocumentsByClassId(Long classId) {
        return classDocumentRepository.findByClassId(classId);
    }

    public Optional<ClassDocument> getDocumentById(Long id) {
        return classDocumentRepository.findById(id);
    }

    public void deleteDocument(Long id) {
        Optional<ClassDocument> document = classDocumentRepository.findById(id);
        document.ifPresent(doc -> {
            if (doc.getFileUrl() != null) {
                File file = new File(doc.getFileUrl());
                if (file.exists()) {
                    file.delete();
                }
            }
            classDocumentRepository.deleteById(id);
        });
    }

    public ClassDocument updateDocument(Long id, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        Optional<ClassDocument> existingDocument = classDocumentRepository.findById(id);
        if (existingDocument.isPresent()) {
            ClassDocument document = existingDocument.get();

            if (document.getFileUrl() != null) {
                File oldFile = new File(document.getFileUrl());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String safeOriginal = original.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
            String fileName = UUID.randomUUID().toString() + "-" + safeOriginal;

            Path baseUploadPath = Paths.get(uploadClassDocDir).toAbsolutePath().normalize();
            Path classDir = baseUploadPath.resolve(String.valueOf(document.getClassId()));
            Files.createDirectories(classDir);

            Path target = classDir.resolve(fileName);
            file.transferTo(target.toFile());

            document.setFileName(original);
            document.setFileUrl(target.toString());

            return classDocumentRepository.save(document);
        } else {
            throw new IllegalArgumentException("Document not found");
        }
    }

    public Resource loadAsResource(Long id) throws IOException {
        Optional<ClassDocument> opt = classDocumentRepository.findById(id);
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("Document not found");
        }
        Path path = Paths.get(opt.get().getFileUrl());
        if (!Files.exists(path)) {
            throw new IOException("File not found on disk");
        }
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new IOException("Failed to load file", e);
        }
    }
}
