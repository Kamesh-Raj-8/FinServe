package com.smelend.smelendbackend.service.application;

import com.smelend.smelendbackend.dto.document.AddDocumentRequest;
import com.smelend.smelendbackend.dto.document.DocumentResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Document;
import com.smelend.smelendbackend.entity.LoanApplication;
import com.smelend.smelendbackend.entity.enums.DocType;
import com.smelend.smelendbackend.entity.enums.UploadStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.DocumentRepository;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private DocumentRepository docRepo;
    private LoanApplicationRepository appRepo;
    private CurrentUserService currentUserService;
    private DocumentService service;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        docRepo = mock(DocumentRepository.class);
        appRepo = mock(LoanApplicationRepository.class);
        currentUserService = mock(CurrentUserService.class);

        service = new DocumentService(docRepo, appRepo, currentUserService);

        mockUser = AppUser.builder().userId(1L).email("user@test.com").build();
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    void add_success() {
        LoanApplication app = LoanApplication.builder()
                .applicationId(100L)
                .createdBy(mockUser)
                .build();

        AddDocumentRequest req = AddDocumentRequest.builder()
                .docType(DocType.PAN)
                .fileUri("s3://bucket/file.pdf")
                .build();

        when(appRepo.findById(100L)).thenReturn(Optional.of(app));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);
        when(docRepo.save(any())).thenAnswer(invocation -> {
            Document d = invocation.getArgument(0);
            d.setDocumentId(200L);
            return d;
        });

        DocumentResponse response = service.add(100L, req);

        assertNotNull(response);
        assertEquals(200L, response.getDocumentId());
        assertEquals(DocType.PAN, response.getDocType());
        assertEquals(UploadStatus.UPLOADED, response.getUploadStatus());
    }

    @Test
    void add_forbiddenWhenNotOwnerOrAdmin() {
        LoanApplication app = LoanApplication.builder()
                .applicationId(100L)
                .createdBy(AppUser.builder().userId(2L).build()) // different owner
                .build();

        AddDocumentRequest req = AddDocumentRequest.builder()
                .docType(DocType.AADHAAR)
                .fileUri("s3://bucket/file.pdf")
                .build();

        when(appRepo.findById(100L)).thenReturn(Optional.of(app));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        assertThrows(ApiException.class, () -> service.add(100L, req));
    }

    @Test
    void list_success() {
        LoanApplication app = LoanApplication.builder()
                .applicationId(100L)
                .createdBy(mockUser)
                .build();

        Document doc = Document.builder()
                .documentId(200L)
                .application(app)
                .docType(DocType.GST)
                .fileUri("s3://bucket/gst.pdf")
                .uploadStatus(UploadStatus.UPLOADED)
                .uploadedDate(LocalDateTime.now())
                .build();

        when(appRepo.findById(100L)).thenReturn(Optional.of(app));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);
        when(docRepo.findByApplication_ApplicationId(100L)).thenReturn(List.of(doc));

        List<DocumentResponse> responses = service.list(100L);

        assertEquals(1, responses.size());
        assertEquals(DocType.GST, responses.get(0).getDocType());
    }

    @Test
    void list_forbiddenWhenNotOwnerOrAdmin() {
        LoanApplication app = LoanApplication.builder()
                .applicationId(100L)
                .createdBy(AppUser.builder().userId(2L).build()) // different owner
                .build();

        when(appRepo.findById(100L)).thenReturn(Optional.of(app));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        assertThrows(ApiException.class, () -> service.list(100L));
    }
}
