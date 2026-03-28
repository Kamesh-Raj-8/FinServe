package com.smelend.smelendbackend.service.application;

import com.smelend.smelendbackend.dto.document.AddDocumentRequest;
import com.smelend.smelendbackend.dto.document.DocumentResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Document;
import com.smelend.smelendbackend.entity.LoanApplication;
import com.smelend.smelendbackend.entity.enums.UploadStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.DocumentRepository;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository docRepo;
    private final LoanApplicationRepository appRepo;
    private final CurrentUserService currentUserService;

    public DocumentService(DocumentRepository docRepo, LoanApplicationRepository appRepo, CurrentUserService currentUserService) {
        this.docRepo = docRepo;
        this.appRepo = appRepo;
        this.currentUserService = currentUserService;
    }

    public DocumentResponse add(Long applicationId, AddDocumentRequest req) {
        AppUser me = currentUserService.getCurrentUser();

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        boolean owner = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot add document to this application");
        }

        Document saved = docRepo.save(Document.builder()
                .application(app)
                .docType(req.getDocType())
                .fileUri(req.getFileUri())
                .uploadStatus(UploadStatus.UPLOADED)
                .uploadedDate(LocalDateTime.now())
                .build());

        return toDto(saved);
    }

    public List<DocumentResponse> list(Long applicationId) {
        AppUser me = currentUserService.getCurrentUser();

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        boolean owner = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view documents for this application");
        }

        return docRepo.findByApplication_ApplicationId(applicationId).stream().map(this::toDto).toList();
    }

    private DocumentResponse toDto(Document d) {
        return DocumentResponse.builder()
                .documentId(d.getDocumentId())
                .applicationId(d.getApplication() != null ? d.getApplication().getApplicationId() : null)
                .docType(d.getDocType())
                .fileUri(d.getFileUri())
                .uploadStatus(d.getUploadStatus())
                .uploadedDate(d.getUploadedDate())
                .build();
    }
}