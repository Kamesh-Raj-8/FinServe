package com.smelend.smelendbackend.controller.application;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.document.AddDocumentRequest;
import com.smelend.smelendbackend.dto.document.DocumentResponse;
import com.smelend.smelendbackend.service.application.DocumentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications/{applicationId}/documents")
@PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
public class ApplicationDocumentController {

    private final DocumentService documentService;

    public ApplicationDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ApiResponse<DocumentResponse> add(
            @PathVariable Long applicationId,
            @Valid @RequestBody AddDocumentRequest req
    ) {
        return ApiResponse.ok("Document added", documentService.add(applicationId, req));
    }

    @GetMapping
    public ApiResponse<List<DocumentResponse>> list(@PathVariable Long applicationId) {
        return ApiResponse.ok("Documents fetched", documentService.list(applicationId));
    }
}