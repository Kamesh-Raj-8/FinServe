package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Sme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmeRepository extends JpaRepository<Sme, Long> {
    List<Sme> findByCreatedBy_UserId(Long userId);
}