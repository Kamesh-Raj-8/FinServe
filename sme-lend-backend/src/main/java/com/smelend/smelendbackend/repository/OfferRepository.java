package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Offer;
import com.smelend.smelendbackend.entity.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByApplication_ApplicationId(Long applicationId);

    List<Offer> findByCreatedBy_UserId(Long userId);

    List<Offer> findByOfferStatus(OfferStatus status);

    /** Fetch all offers for applications that are currently in OFFER_ACCEPTED status */
    @Query("SELECT o FROM Offer o WHERE o.application.status = 'OFFER_ACCEPTED' ORDER BY o.application.applicationId DESC")
    List<Offer> findAllForAcceptedApplications();
}
