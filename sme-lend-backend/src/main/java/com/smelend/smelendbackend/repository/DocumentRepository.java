package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Document;
import com.smelend.smelendbackend.entity.enums.DocType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByApplication_ApplicationId(Long applicationId);

    /** Returns the existing record for this (application, docType) pair — used for upsert */
    Optional<Document> findByApplication_ApplicationIdAndDocType(Long applicationId, DocType docType);
}
