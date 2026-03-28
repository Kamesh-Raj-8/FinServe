package com.smelend.smelendbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "portfolio_report")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false, length = 100)
    private String scope;

    @Lob
    @Column(nullable = false)
    private String metricsJson;

    @Column(nullable = false)
    private LocalDate generatedDate;
}
