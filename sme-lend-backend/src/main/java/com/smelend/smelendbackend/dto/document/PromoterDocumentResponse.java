package com.smelend.smelendbackend.dto.document;

import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.entity.enums.UploadStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromoterDocumentResponse {
    private Long   docId;
    private Long   promoterId;
    private DocType docType;
    private String  fileName;
    private String  contentType;
    private String  downloadUrl;     // /onboarding/promoters/{id}/documents/{docType}/download
    private UploadStatus uploadStatus;
    private LocalDateTime uploadedDate;
    private LocalDateTime lastReplacedDate;
    private boolean replaced;        // true if this call replaced an existing doc
}
