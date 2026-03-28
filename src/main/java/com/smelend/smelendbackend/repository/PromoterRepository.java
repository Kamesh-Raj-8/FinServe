package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Promoter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromoterRepository extends JpaRepository<Promoter, Long> {
    List<Promoter> findBySme_SmeId(Long smeId);
}