package com.example.English.Center.Data.controller.file;

import com.example.English.Center.Data.entity.ClassDocument;
import com.example.English.Center.Data.service.ClassDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/class-documents")
public class ClassDocumentController {

    @Autowired
    private ClassDocumentService classDocumentService;

    @PostMapping("/upload/{classId}")
    public ResponseEntity<ClassDocument> uploadDocument(@PathVariable Long classId, @RequestParam("file") MultipartFile file) {
        try {
            ClassDocument document = classDocumentService.uploadDocument(classId, file);
            return ResponseEntity.status(201).body(document);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<ClassDocument>> getDocumentsByClassId(@PathVariable Long classId) {
        List<ClassDocument> documents = classDocumentService.getDocumentsByClassId(classId);
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        classDocumentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        try {
            Resource resource = classDocumentService.loadAsResource(id);
            Optional<ClassDocument> opt = classDocumentService.getDocumentById(id);
            String filename = opt.map(ClassDocument::getFileName).orElse("file");

            Path path = Paths.get(resource.getURI());
            String contentType = null;
            try {
                contentType = Files.probeContentType(path);
            } catch (IOException ignored) {
            }
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            long contentLength = -1;
            try {
                contentLength = resource.contentLength();
            } catch (IOException ignored) {
            }

            // RFC 5987 filename* to support UTF-8 names + fallback filename
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            String contentDisposition = "attachment; filename=\"" + filename.replace('"', '\'') + "\"; filename*=UTF-8''" + encoded;

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

            if (contentLength >= 0) {
                builder.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            }

            return builder.body(resource);
        } catch (IllegalArgumentException | IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
