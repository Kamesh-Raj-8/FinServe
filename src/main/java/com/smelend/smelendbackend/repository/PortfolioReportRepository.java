package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.PortfolioReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioReportRepository extends JpaRepository<PortfolioReport, Long> {
    
}
