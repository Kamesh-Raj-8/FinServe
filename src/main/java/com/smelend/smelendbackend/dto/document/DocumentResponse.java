package com.smelend.smelendbackend.dto.document;

import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.entity.enums.UploadStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentResponse {

    private Long documentId;
    private Long applicationId;

    private DocType docType;
    private String fileUri;

    private UploadStatus uploadStatus;
    private LocalDateTime uploadedDate;
}