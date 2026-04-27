package com.smelend.smelendbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "in_app_notification", indexes = {
    @Index(name = "idx_notif_user", columnList = "user_id"),
    @Index(name = "idx_notif_read", columnList = "is_read")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Short title shown in the notification bell dropdown */
    @Column(nullable = false, length = 100)
    private String title;

    /** Full message body */
    @Column(nullable = false, length = 500)
    private String message;

    /** Category: ONBOARDING | UNDERWRITING | OFFER | DISBURSEMENT | SERVICING | COLLECTIONS */
    @Column(length = 30)
    private String category;

    /** Related entity type for deep-linking: APPLICATION | KYC | LOAN_ACCOUNT | OFFER */
    @Column(length = 30)
    private String entityType;

    @Column
    private Long entityId;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
