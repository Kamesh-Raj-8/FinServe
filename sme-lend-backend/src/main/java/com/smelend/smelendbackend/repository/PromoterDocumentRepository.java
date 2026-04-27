package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.PromoterDocument;
import com.smelend.smelendbackend.entity.enums.DocType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromoterDocumentRepository extends JpaRepository<PromoterDocument, Long> {

    List<PromoterDocument> findByPromoter_PromoterIdOrderByDocType(Long promoterId);

    Optional<PromoterDocument> findByPromoter_PromoterIdAndDocType(Long promoterId, DocType docType);
}
