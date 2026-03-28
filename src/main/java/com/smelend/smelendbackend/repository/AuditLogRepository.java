package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByRefTypeAndRefIdOrderByCreatedDateDesc(String refType, Long refId);

    List<AuditLog> findByActor_UserIdOrderByCreatedDateDesc(Long userId);
}