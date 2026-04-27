package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    
}
