package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.BusinessType;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long smeId;

    @Column(nullable = false, length = 150)
    private String legalName;

    @Column(length = 150)
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BusinessType businessType;

    @Column(nullable = false, length = 50)
    private String industry;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 20)
    private String gstNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFlag status;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;
}