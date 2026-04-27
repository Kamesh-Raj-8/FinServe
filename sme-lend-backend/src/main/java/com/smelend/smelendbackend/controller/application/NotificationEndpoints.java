package com.smelend.smelendbackend.controller.application;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.notification.NotificationResponse;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.notification.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notification endpoints — placed in the application module because
 * they serve every authenticated role (applicant, agent, UW, ops, etc.).
 * No new folder created; this lives alongside ApplicationController.
 */
@RestController
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationEndpoints {

    private final NotificationService notificationService;
    private final CurrentUserService  currentUserService;

    public NotificationEndpoints(NotificationService notificationService,
                                  CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService  = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list() {
        Long uid = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok("Notifications fetched", notificationService.getForUser(uid));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        Long uid = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok("Count fetched", notificationService.countUnread(uid));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(currentUserService.getCurrentUser().getUserId());
        return ApiResponse.ok("Marked all read");
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markOneRead(id, currentUserService.getCurrentUser().getUserId());
        return ApiResponse.ok("Marked read");
    }
}
