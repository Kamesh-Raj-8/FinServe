package com.smelend.smelendbackend.dto.notification;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {
    private Long notificationId;
    private String title;
    private String message;
    private String category;
    private String entityType;
    private Long entityId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
