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

    /** Legacy link (null if uploaded via multipart) */
    private String fileUri;

    /** Original filename of the uploaded PDF */
    private String fileName;

    /** MIME type e.g. application/pdf */
    private String contentType;

    /** Download URL: /applications/{appId}/documents/{docId}/download */
    private String downloadUrl;

    private UploadStatus uploadStatus;
    private LocalDateTime uploadedDate;
    /** Set if this document was replaced in-place (re-uploaded) */
    private LocalDateTime lastReplacedDate;
    /** True = an existing record was replaced; False = newly created */
    private boolean replaced;
}