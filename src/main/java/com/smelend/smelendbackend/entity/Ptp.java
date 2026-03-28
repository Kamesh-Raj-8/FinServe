package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.PtpStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ptp")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Ptp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ptpId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_account_id")
    private LoanAccount loanAccount;

    @Column(nullable = false)
    private LocalDate promiseDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal promisedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PtpStatus status;

    private String notes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdBy;

    @Column(nullable = false)
    private LocalDateTime createdDate;
}