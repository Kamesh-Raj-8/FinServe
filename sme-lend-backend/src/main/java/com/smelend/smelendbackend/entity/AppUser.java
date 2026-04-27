package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.StatusFlag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 15)
    private String phone;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(length = 20)
    private String bankAccountNo;

    @Column(length = 20)
    private String ifsc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFlag status;
}
