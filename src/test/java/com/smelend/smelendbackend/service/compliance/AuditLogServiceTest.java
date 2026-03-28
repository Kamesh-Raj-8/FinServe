package com.smelend.smelendbackend.service.compliance;

import com.smelend.smelendbackend.dto.compliance.AuditLogResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.AuditLog;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditRepo;

    @InjectMocks
    private AuditLogService auditLogService;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = AppUser.builder()
                .userId(1L)
                .email("actor@example.com")
                .build();
    }

    @Test
    void log_savesAuditLog() {
        when(auditRepo.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log(mockUser, AuditAction.LOGIN, "APPLICATION", 123L, "User logged in");

        verify(auditRepo, times(1)).save(any(AuditLog.class));
    }

    @Test
    void listAll_returnsMappedResponses() {
        AuditLog log1 = AuditLog.builder()
                .auditId(1L)
                .actor(mockUser)
                .action(AuditAction.LOGIN)
                .refType("APPLICATION")
                .refId(123L)
                .message("User logged in")
                .createdDate(LocalDateTime.now())
                .build();

        AuditLog log2 = AuditLog.builder()
                .auditId(2L)
                .actor(mockUser)
                .action(AuditAction.APPLICATION_CREATED)
                .refType("APPLICATION")
                .refId(456L)
                .message("Application created")
                .createdDate(LocalDateTime.now())
                .build();

        when(auditRepo.findAll()).thenReturn(List.of(log1, log2));

        List<AuditLogResponse> responses = auditLogService.listAll();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getAuditId());
        assertEquals("actor@example.com", responses.get(0).getActorEmail());
    }

    @Test
    void listByRef_returnsMappedResponses() {
        AuditLog log = AuditLog.builder()
                .auditId(3L)
                .actor(mockUser)
                .action(AuditAction.PTP_CREATED)
                .refType("LOAN_ACCOUNT")
                .refId(999L)
                .message("PTP created")
                .createdDate(LocalDateTime.now())
                .build();

        when(auditRepo.findByRefTypeAndRefIdOrderByCreatedDateDesc("LOAN_ACCOUNT", 999L))
                .thenReturn(List.of(log));

        List<AuditLogResponse> responses = auditLogService.listByRef("LOAN_ACCOUNT", 999L);

        assertEquals(1, responses.size());
        assertEquals(999L, responses.get(0).getRefId());
        assertEquals(AuditAction.PTP_CREATED, responses.get(0).getAction());
    }

    @Test
    void listByActor_returnsMappedResponses() {
        AuditLog log = AuditLog.builder()
                .auditId(4L)
                .actor(mockUser)
                .action(AuditAction.PTP_STATUS_UPDATED)
                .refType("PTP")
                .refId(888L)
                .message("PTP status updated")
                .createdDate(LocalDateTime.now())
                .build();

        when(auditRepo.findByActor_UserIdOrderByCreatedDateDesc(1L))
                .thenReturn(List.of(log));

        List<AuditLogResponse> responses = auditLogService.listByActor(1L);

        assertEquals(1, responses.size());
        assertEquals("actor@example.com", responses.get(0).getActorEmail());
        assertEquals(AuditAction.PTP_STATUS_UPDATED, responses.get(0).getAction());
    }
}
