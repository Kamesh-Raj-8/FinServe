package com.smelend.smelendbackend.service.application;

import com.smelend.smelendbackend.dto.document.AddDocumentRequest;
import com.smelend.smelendbackend.dto.document.DocumentResponse;
import com.smelend.smelendbackend.dto.document.PromoterDocumentResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.entity.enums.UploadStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository         docRepo;
    private final PromoterDocumentRepository promoterDocRepo;
    private final LoanApplicationRepository  appRepo;
    private final PromoterRepository         promoterRepo;
    private final CurrentUserService         currentUserService;
    private final Path                       uploadRoot;

    public DocumentService(DocumentRepository docRepo,
                           PromoterDocumentRepository promoterDocRepo,
                           LoanApplicationRepository appRepo,
                           PromoterRepository promoterRepo,
                           CurrentUserService currentUserService,
                           @Value("${app.upload.dir:${user.home}/smelend-uploads}") String uploadDir) {
        this.docRepo          = docRepo;
        this.promoterDocRepo  = promoterDocRepo;
        this.appRepo          = appRepo;
        this.promoterRepo     = promoterRepo;
        this.currentUserService = currentUserService;
        this.uploadRoot       = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FINANCIAL DOCUMENTS — linked to LoanApplication (one per DocType)
    // ═══════════════════════════════════════════════════════════════════

    /** URI-based add — upserts: replaces existing docType record if present */
    public DocumentResponse add(Long applicationId, AddDocumentRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        LoanApplication app = requireWriteAccess(applicationId, me);
        return upsertUri(app, req.getDocType(), req.getFileUri(), me);
    }

    /** File upload — upserts: replaces existing docType record if present */
    public DocumentResponse uploadFile(Long applicationId, MultipartFile file, DocType docType) {
        AppUser me = currentUserService.getCurrentUser();
        LoanApplication app = requireWriteAccess(applicationId, me);
        return upsertFile(app, docType, file, me);
    }

    /** List all financial docs for an application */
    public List<DocumentResponse> list(Long applicationId) {
        requireReadAccess(applicationId, currentUserService.getCurrentUser());
        return docRepo.findByApplication_ApplicationId(applicationId)
                .stream().map(d -> toDto(d, false)).toList();
    }

    /** Download bytes for a financial document */
    public byte[] downloadFile(Long applicationId, Long documentId) {
        requireReadAccess(applicationId, currentUserService.getCurrentUser());
        Document doc = docRepo.findById(documentId)
                .filter(d -> d.getApplication().getApplicationId().equals(applicationId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
        return readBytes(doc.getFileUri(), doc.getFileName());
    }

    public Document getDocumentMeta(Long applicationId, Long documentId) {
        requireReadAccess(applicationId, currentUserService.getCurrentUser());
        return docRepo.findById(documentId)
                .filter(d -> d.getApplication().getApplicationId().equals(applicationId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  KYC DOCUMENTS — linked to Promoter (one per DocType, permanent)
    // ═══════════════════════════════════════════════════════════════════

    /** Upload or replace a KYC document for a promoter */
    public PromoterDocumentResponse uploadPromoterDoc(Long promoterId, MultipartFile file, DocType docType) {
        AppUser me = currentUserService.getCurrentUser();
        Promoter promoter = promoterRepo.findById(promoterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Promoter not found"));

        validateKycDocType(docType);
        validateFileType(file);

        Path dir = uploadRoot.resolve("promoter-" + promoterId);
        try { Files.createDirectories(dir); }
        catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create upload dir"); }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        Path target = dir.resolve(UUID.randomUUID() + "_" + originalName);
        try { Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file"); }

        // Upsert: one record per (promoter, docType)
        Optional<PromoterDocument> existing = promoterDocRepo
                .findByPromoter_PromoterIdAndDocType(promoterId, docType);

        boolean isReplace = existing.isPresent();
        PromoterDocument doc = existing.orElse(PromoterDocument.builder()
                .promoter(promoter)
                .docType(docType)
                .build());

        doc.setFileUri(target.toString());
        doc.setFileName(originalName);
        doc.setContentType(file.getContentType());
        doc.setUploadStatus(UploadStatus.UPLOADED);
        doc.setUploadedBy(me);
        if (isReplace) doc.setLastReplacedDate(LocalDateTime.now());

        PromoterDocument saved = promoterDocRepo.save(doc);
        return toPromoterDto(saved, isReplace);
    }

    /** List all KYC docs for a promoter */
    public List<PromoterDocumentResponse> listPromoterDocs(Long promoterId) {
        return promoterDocRepo.findByPromoter_PromoterIdOrderByDocType(promoterId)
                .stream().map(d -> toPromoterDto(d, false)).toList();
    }

    /** Download a promoter KYC document */
    public byte[] downloadPromoterDoc(Long promoterId, DocType docType) {
        PromoterDocument doc = promoterDocRepo
                .findByPromoter_PromoterIdAndDocType(promoterId, docType)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No " + docType + " document found for promoter #" + promoterId));
        return readBytes(doc.getFileUri(), doc.getFileName());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private DocumentResponse upsertFile(LoanApplication app, DocType docType,
                                         MultipartFile file, AppUser me) {
        validateFileType(file);

        Path dir = uploadRoot.resolve("app-" + app.getApplicationId());
        try { Files.createDirectories(dir); }
        catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create upload dir"); }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        Path target = dir.resolve(UUID.randomUUID() + "_" + originalName);
        try { Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file"); }

        Optional<Document> existing = docRepo
                .findByApplication_ApplicationIdAndDocType(app.getApplicationId(), docType);

        boolean isReplace = existing.isPresent();
        Document doc = existing.orElse(Document.builder()
                .application(app)
                .docType(docType)
                .build());

        doc.setFileUri(target.toString());
        doc.setFileName(originalName);
        doc.setContentType(file.getContentType());
        doc.setUploadStatus(UploadStatus.UPLOADED);
        doc.setUploadedBy(me);
        if (isReplace) doc.setLastReplacedDate(LocalDateTime.now());

        return toDto(docRepo.save(doc), isReplace);
    }

    private DocumentResponse upsertUri(LoanApplication app, DocType docType,
                                        String fileUri, AppUser me) {
        Optional<Document> existing = docRepo
                .findByApplication_ApplicationIdAndDocType(app.getApplicationId(), docType);

        boolean isReplace = existing.isPresent();
        Document doc = existing.orElse(Document.builder()
                .application(app)
                .docType(docType)
                .build());

        doc.setFileUri(fileUri);
        doc.setFileName(null);
        doc.setContentType(null);
        doc.setUploadStatus(UploadStatus.UPLOADED);
        doc.setUploadedBy(me);
        if (isReplace) doc.setLastReplacedDate(LocalDateTime.now());

        return toDto(docRepo.save(doc), isReplace);
    }

    private void validateFileType(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        String ct = file.getContentType();
        if (ct == null || (!ct.equals("application/pdf") && !ct.startsWith("image/")))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF and image files are accepted. Received: " + ct);
    }

    private void validateKycDocType(DocType docType) {
        switch (docType) {
            case PAN, AADHAAR, BUSINESS_REG_CERT, GST_CERTIFICATE, PROMOTER_PHOTO, SHOP_LICENSE -> { /* valid */ }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    docType + " is a financial document type. Upload it under the loan application, not the promoter.");
        }
    }

    private byte[] readBytes(String fileUri, String fileName) {
        if (fileUri == null || fileUri.isBlank())
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No file attached to this document");
        if (fileUri.startsWith("http://") || fileUri.startsWith("https://"))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This is an external URL — access it directly");
        Path path = Paths.get(fileUri);
        if (!Files.exists(path))
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found on server: " + fileName);
        try { return Files.readAllBytes(path); }
        catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file: " + e.getMessage()); }
    }

    private LoanApplication requireAppAccess(Long applicationId, AppUser me) {
        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));
        String role = me.getRole().getRoleName().name();
        boolean isAdmin   = role.equals("ADMIN");
        boolean isCreator = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());
        if (!isCreator && !isAdmin && !role.equals("AGENT") && !role.equals("UNDERWRITER")
                && !role.equals("OPERATIONS") && !role.equals("SERVICING"))
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied for this application");
        return app;
    }

    private LoanApplication requireWriteAccess(Long applicationId, AppUser me) {
        LoanApplication app = requireAppAccess(applicationId, me);
        String role    = me.getRole().getRoleName().name();
        boolean isAdmin   = role.equals("ADMIN");
        boolean isCreator = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());
        if (!isCreator && !isAdmin) {
            String creatorRole = app.getCreatedBy() != null ? app.getCreatedBy().getRole().getRoleName().name() : "";
            if (creatorRole.equals("APPLICANT"))
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Permission Denied: Creator Only. This application was created by the Applicant. " +
                        "Only the Applicant or Admin can upload or add documents.");
        }
        return app;
    }

    private void requireReadAccess(Long applicationId, AppUser me) { requireAppAccess(applicationId, me); }

    private DocumentResponse toDto(Document d, boolean replaced) {
        boolean isFileUpload = d.getFileUri() != null
                && !d.getFileUri().startsWith("http://")
                && !d.getFileUri().startsWith("https://")
                && d.getFileName() != null;
        return DocumentResponse.builder()
                .documentId(d.getDocumentId())
                .applicationId(d.getApplication() != null ? d.getApplication().getApplicationId() : null)
                .docType(d.getDocType())
                .fileUri(isFileUpload ? null : d.getFileUri())
                .fileName(d.getFileName())
                .contentType(d.getContentType())
                .downloadUrl(isFileUpload
                        ? "/applications/" + d.getApplication().getApplicationId()
                          + "/documents/" + d.getDocumentId() + "/download"
                        : null)
                .uploadStatus(d.getUploadStatus())
                .uploadedDate(d.getUploadedDate())
                .lastReplacedDate(d.getLastReplacedDate())
                .replaced(replaced)
                .build();
    }

    private PromoterDocumentResponse toPromoterDto(PromoterDocument d, boolean replaced) {
        return PromoterDocumentResponse.builder()
                .docId(d.getDocId())
                .promoterId(d.getPromoter().getPromoterId())
                .docType(d.getDocType())
                .fileName(d.getFileName())
                .contentType(d.getContentType())
                .downloadUrl("/onboarding/promoters/" + d.getPromoter().getPromoterId()
                        + "/documents/" + d.getDocType().name() + "/download")
                .uploadStatus(d.getUploadStatus())
                .uploadedDate(d.getUploadedDate())
                .lastReplacedDate(d.getLastReplacedDate())
                .replaced(replaced)
                .build();
    }
}
