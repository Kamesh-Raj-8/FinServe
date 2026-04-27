package com.smelend.smelendbackend.controller.onboarding;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.onboarding.promoter.AddPromoterRequest;
import com.smelend.smelendbackend.dto.onboarding.promoter.PromoterResponse;
import com.smelend.smelendbackend.dto.document.PromoterDocumentResponse;
import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.service.application.DocumentService;
import com.smelend.smelendbackend.service.onboarding.PromoterService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/onboarding")
public class PromoterController {

    private final PromoterService  promoterService;
    private final DocumentService  documentService;

    public PromoterController(PromoterService promoterService,
                               DocumentService documentService) {
        this.promoterService = promoterService;
        this.documentService = documentService;
    }

    // ── WRITE: APPLICANT / AGENT only ─────────────────────────────────

    @PostMapping("/smes/{smeId}/promoters")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<PromoterResponse> addPromoter(
            @PathVariable Long smeId,
            @Valid @RequestBody AddPromoterRequest req) {
        return ApiResponse.ok("Promoter added", promoterService.addPromoter(smeId, req));
    }

    // ── READ: Admin/UW can monitor ────────────────────────────────────

    @GetMapping("/smes/{smeId}/promoters")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<List<PromoterResponse>> listPromoters(@PathVariable Long smeId) {
        return ApiResponse.ok("Promoters fetched", promoterService.listBySme(smeId));
    }

    @GetMapping("/promoters/{promoterId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<PromoterResponse> getPromoter(@PathVariable Long promoterId) {
        return ApiResponse.ok("Promoter fetched", promoterService.get(promoterId));
    }

    // ── KYC DOCUMENTS — linked to Promoter (one per DocType) ─────────

    /** Upload or replace a KYC document for a promoter */
    @PostMapping("/promoters/{promoterId}/documents/{docType}/upload")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN')")
    public ApiResponse<PromoterDocumentResponse> uploadKycDoc(
            @PathVariable Long promoterId,
            @PathVariable DocType docType,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("KYC document uploaded",
                documentService.uploadPromoterDoc(promoterId, file, docType));
    }

    /** List all KYC documents for a promoter */
    @GetMapping("/promoters/{promoterId}/documents")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<java.util.List<PromoterDocumentResponse>> listKycDocs(
            @PathVariable Long promoterId) {
        return ApiResponse.ok("KYC documents fetched",
                documentService.listPromoterDocs(promoterId));
    }

    /** Download a specific KYC document */
    @GetMapping("/promoters/{promoterId}/documents/{docType}/download")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ResponseEntity<byte[]> downloadKycDoc(@PathVariable Long promoterId,
                                                  @PathVariable DocType docType) {
        byte[] bytes = documentService.downloadPromoterDoc(promoterId, docType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + docType.name() + ".pdf\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

}
