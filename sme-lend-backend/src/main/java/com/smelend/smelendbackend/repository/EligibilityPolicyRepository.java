package com.smelend.smelendbackend.repository;
import com.smelend.smelendbackend.entity.EligibilityPolicy;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EligibilityPolicyRepository extends JpaRepository<EligibilityPolicy, Long> {
    List<EligibilityPolicy> findByProduct_ProductId(Long productId);
    List<EligibilityPolicy> findByProduct_ProductIdAndStatus(Long productId, StatusFlag status);
}
