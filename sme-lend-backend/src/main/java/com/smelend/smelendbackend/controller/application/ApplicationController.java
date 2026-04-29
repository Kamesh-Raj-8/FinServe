package com.smelend.smelendbackend.controller.application;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.application.CreateApplicationRequest;
import com.smelend.smelendbackend.dto.document.AddDocumentRequest;
import com.smelend.smelendbackend.dto.document.DocumentResponse;
import com.smelend.smelendbackend.entity.Document;
import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.service.application.ApplicationService;
import com.smelend.smelendbackend.service.application.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final DocumentService    documentService;

    public ApplicationController(ApplicationService applicationService,
                              DocumentService documentService) {
        this.applicationService = applicationService;
        this.documentService     = documentService;
    }



    @PostMapping
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<ApplicationResponse> create(@Valid @RequestBody CreateApplicationRequest req) {
        return ApiResponse.ok("Application created", applicationService.create(req));
    }

    @PatchMapping("/{applicationId}/submit")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<ApplicationResponse> submit(@PathVariable Long applicationId) {
        return ApiResponse.ok("Application submitted and routed to underwriter",
                applicationService.submit(applicationId));
    }

    @PostMapping("/{applicationId}/documents")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<DocumentResponse> addDoc(@PathVariable Long applicationId,
                                                @Valid @RequestBody AddDocumentRequest req) {
        return ApiResponse.ok("Document added", documentService.add(applicationId, req));
    }

    @PostMapping(value = "/{applicationId}/documents/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<DocumentResponse> uploadDoc(
            @PathVariable Long applicationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") DocType docType) {
        return ApiResponse.ok("Document uploaded",
                documentService.uploadFile(applicationId, file, docType));
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN')")
    public ApiResponse<List<ApplicationResponse>> listMine() {
        return ApiResponse.ok("Applications fetched", applicationService.listMine());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<List<ApplicationResponse>> listAll() {
        return ApiResponse.ok("All applications fetched", applicationService.listAll());
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<ApplicationResponse> get(@PathVariable Long applicationId) {
        return ApiResponse.ok("Application fetched", applicationService.get(applicationId));
    }

    @GetMapping("/{applicationId}/documents")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<List<DocumentResponse>> listDocs(@PathVariable Long applicationId) {
        return ApiResponse.ok("Documents fetched", documentService.list(applicationId));
    }

    @GetMapping("/{applicationId}/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ResponseEntity<byte[]> downloadDoc(@PathVariable Long applicationId,
                                               @PathVariable Long documentId) {
        Document meta  = documentService.getDocumentMeta(applicationId, documentId);
        byte[]   bytes = documentService.downloadFile(applicationId, documentId);
        String   ct    = meta.getContentType() != null ? meta.getContentType() : "application/octet-stream";
        String   fname = meta.getFileName()    != null ? meta.getFileName()    : "document";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fname + "\"")
                .contentType(MediaType.parseMediaType(ct))
                .body(bytes);
    }

}
