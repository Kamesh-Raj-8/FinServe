package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByApplication_ApplicationId(Long applicationId);

    List<Offer> findByCreatedBy_UserId(Long userId);
}