package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.UwReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UwReviewRepository extends JpaRepository<UwReview, Long> {
    Optional<UwReview> findByApplication_ApplicationId(Long applicationId);
}