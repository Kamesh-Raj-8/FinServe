package com.smelend.smelendbackend.repository;
import com.smelend.smelendbackend.entity.FeeConfig;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FeeConfigRepository extends JpaRepository<FeeConfig, Long> {
    List<FeeConfig> findByProduct_ProductId(Long productId);
    List<FeeConfig> findByProduct_ProductIdAndStatus(Long productId, StatusFlag status);
}
