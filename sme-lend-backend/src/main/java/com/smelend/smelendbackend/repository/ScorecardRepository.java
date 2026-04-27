package com.smelend.smelendbackend.repository;
import com.smelend.smelendbackend.entity.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ScorecardRepository extends JpaRepository<Scorecard, Long> {
    Optional<Scorecard> findByApplication_ApplicationId(Long applicationId);
}
