package com.smelend.smelendbackend.repository;
import com.smelend.smelendbackend.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DecisionRepository extends JpaRepository<Decision, Long> {
    Optional<Decision> findByApplication_ApplicationId(Long applicationId);
}
