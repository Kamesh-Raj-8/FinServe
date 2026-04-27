package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.entity.enums.UploadStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * KYC identity documents tied to a specific Promoter — NOT to a loan application.
 *
 * Once uploaded, these documents persist across all loan applications for this promoter.
 * Unique constraint: one record per (promoter_id, doc_type).
 * To change a document, use the replace endpoint — the same row is updated in-place.
 *
 * KYC DocTypes: PAN, AADHAAR, BUSINESS_REG_CERT, GST_CERTIFICATE, PROMOTER_PHOTO, SHOP_LICENSE
 */
@Entity
@Table(name = "promoter_document",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_promoter_doctype",
           columnNames = {"promoter_id", "doc_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromoterDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "promoter_id", nullable = false)
    private Promoter promoter;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 50)
    private DocType docType;

    /** Filesystem path or external URL */
    @Column(length = 1024)
    private String fileUri;

    /** Original filename as uploaded */
    @Column(length = 255)
    private String fileName;

    /** MIME type */
    @Column(length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus uploadStatus;

    /** When this document was first uploaded */
    @Column(nullable = false)
    private LocalDateTime uploadedDate;

    /** When this document was last replaced (null if never re-uploaded) */
    @Column
    private LocalDateTime lastReplacedDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploaded_by_user_id")
    private AppUser uploadedBy;

    @PrePersist
    void prePersist() {
        if (uploadedDate == null) uploadedDate = LocalDateTime.now();
    }
}
