package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.entity.enums.UploadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocType docType;

    @Column(nullable = false, length = 255)
    private String fileUri;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus uploadStatus;

    @Column(nullable = false)
    private LocalDateTime uploadedDate;
}
