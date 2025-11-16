package com.example.English.Center.Data.repository;

import com.example.English.Center.Data.entity.ClassDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassDocumentRepository extends JpaRepository<ClassDocument, Long> {
    List<ClassDocument> findByClassId(Long classId);
}
