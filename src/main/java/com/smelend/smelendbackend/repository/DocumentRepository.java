package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByApplication_ApplicationId(Long applicationId);
}