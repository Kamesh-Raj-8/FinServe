package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.entity.enums.UploadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_application_doctype",
           columnNames = {"application_id", "doc_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocType docType;

    /**
     * Stores either:
     * - A user-supplied URL string (legacy mode), OR
     * - Absolute filesystem path where the uploaded PDF is saved (file-upload mode)
     * Increased to 1024 to accommodate full OS path lengths.
     */
    @Column(length = 1024)
    private String fileUri;

    /** Original filename as uploaded, e.g. "aadhaar.pdf" */
    @Column(length = 255)
    private String fileName;

    /** MIME type, e.g. "application/pdf" */
    @Column(length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus uploadStatus;

    @Column(nullable = false)
    private LocalDateTime uploadedDate;

    @Column
    private LocalDateTime lastReplacedDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploaded_by_user_id")
    private AppUser uploadedBy;

    @PrePersist
    void prePersist() { if (uploadedDate == null) uploadedDate = java.time.LocalDateTime.now(); }
}
