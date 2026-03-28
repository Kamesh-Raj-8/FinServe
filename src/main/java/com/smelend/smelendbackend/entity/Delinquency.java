package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.BucketType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "delinquency",
        uniqueConstraints = @UniqueConstraint(columnNames = {"loan_account_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Delinquency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long delinquencyId;

    @OneToOne(optional = false)
    @JoinColumn(name = "loan_account_id")
    private LoanAccount loanAccount;

    @Column(nullable = false)
    private Integer dpd; // days past due

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BucketType bucket;

    private LocalDate asOfDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;
}