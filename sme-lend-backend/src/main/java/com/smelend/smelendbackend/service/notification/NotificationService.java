package com.smelend.smelendbackend.service.notification;

import com.smelend.smelendbackend.dto.notification.NotificationResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.InAppNotification;
import com.smelend.smelendbackend.entity.enums.RoleName;
import com.smelend.smelendbackend.repository.AppUserRepository;
import com.smelend.smelendbackend.repository.InAppNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * In-app notification service — Database-as-Queue model.
 *
 * Delivery strategy:
 *   deliverToUser(email, ...)  → writes ONE record for a specific user
 *   deliverToRole(role, ...)   → writes N records, one per active user with that role
 *
 * No email, SMS, or WebSocket. Frontend polls /notifications/unread-count every 30 s
 * and fetches the full list only when the user opens the notification panel.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final InAppNotificationRepository notifRepo;
    private final AppUserRepository           userRepo;

    public NotificationService(InAppNotificationRepository notifRepo,
                                AppUserRepository userRepo) {
        this.notifRepo = notifRepo;
        this.userRepo  = userRepo;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BUSINESS EVENT TRIGGERS
    //  Each method defines: WHAT happened, WHO needs to know.
    // ═══════════════════════════════════════════════════════════════

    // ── Onboarding ────────────────────────────────────────────────

    public void notifyRegistrationSuccess(String email, String fullName) {
        deliverToUser(email,
                "Welcome to FinServe! 🎉",
                "Hello " + fullName + ", your account is ready. Start your loan journey today.",
                "ONBOARDING", null, null);
    }

    // ── KYC ───────────────────────────────────────────────────────

    public void notifyKycCreated(String email, String fullName, Long kycId) {
        // Applicant: KYC submitted
        deliverToUser(email,
                "KYC Submitted",
                "Your KYC record #" + kycId + " has been submitted. An agent will verify it shortly.",
                "ONBOARDING", "KYC", kycId);
        // Agents: new KYC waiting in queue
        deliverToRole(RoleName.AGENT,
                "New KYC Pending Verification",
                "KYC #" + kycId + " for " + fullName + " is awaiting verification.",
                "ONBOARDING", "KYC", kycId);
    }

    public void notifyKycVerified(String email, String fullName, Long kycId) {
        // Applicant: KYC approved
        deliverToUser(email,
                "KYC Verified ✓",
                "Your KYC #" + kycId + " has been verified. You may now submit your loan application.",
                "ONBOARDING", "KYC", kycId);
    }

    public void notifyKycRejected(String email, String fullName, Long kycId) {
        deliverToUser(email,
                "KYC Rejected",
                "Your KYC #" + kycId + " was rejected. Please re-submit with corrected documents.",
                "ONBOARDING", "KYC", kycId);
    }

    // ── Application ───────────────────────────────────────────────

    public void notifyLoanApplicationCreated(String email, String fullName, Long appId) {
        deliverToUser(email,
                "Application #" + appId + " Created",
                "Your loan application #" + appId + " has been created and is pending KYC verification.",
                "ONBOARDING", "APPLICATION", appId);
    }

    public void notifyRoutedToUnderwriter(String email, String fullName, Long appId) {
        // Applicant: application is now in review
        deliverToUser(email,
                "Application Under Review",
                "Application #" + appId + " has been sent to our underwriting team. Response within 2 business days.",
                "UNDERWRITING", "APPLICATION", appId);
        // All Underwriters: new application in their queue
        deliverToRole(RoleName.UNDERWRITER,
                "New Application in UW Queue",
                "Application #" + appId + " from " + fullName + " is ready for underwriting review.",
                "UNDERWRITING", "APPLICATION", appId);
    }

    public void notifyUnderwriterApproved(String email, String fullName, Long appId) {
        // Applicant: great news
        deliverToUser(email,
                "Application Approved! 🎉",
                "Congratulations! Application #" + appId + " has been approved. An offer will be ready shortly.",
                "UNDERWRITING", "APPLICATION", appId);
        // Operations: create an offer
        deliverToRole(RoleName.OPERATIONS,
                "Application Approved — Offer Required",
                "Application #" + appId + " (" + fullName + ") has been UW-approved. Create a loan offer.",
                "UNDERWRITING", "APPLICATION", appId);
    }

    public void notifyUnderwriterRejected(String email, String fullName, Long appId) {
        deliverToUser(email,
                "Application Rejected",
                "We regret to inform you that application #" + appId + " was not approved at this time.",
                "UNDERWRITING", "APPLICATION", appId);
    }

    public void notifyApplicationReturned(String email, String fullName, Long appId, String reason) {
        // Applicant: action required
        deliverToUser(email,
                "Action Required on Application #" + appId,
                "Your application #" + appId + " has been returned for KYC re-verification. Reason: " + reason,
                "UNDERWRITING", "APPLICATION", appId);
        // Agent managing this applicant
        deliverToRole(RoleName.AGENT,
                "Application #" + appId + " Returned — KYC Re-verify Needed",
                "Application #" + appId + " (" + fullName + ") was returned by the underwriter. Assist with KYC re-verification.",
                "UNDERWRITING", "APPLICATION", appId);
    }

    // ── Offer ─────────────────────────────────────────────────────

    public void notifyOfferCreated(String email, String fullName, Long appId, String amount) {
        deliverToUser(email,
                "Loan Offer Ready — ₹" + amount,
                "A loan offer of ₹" + amount + " is ready for application #" + appId + ". Log in to review and accept.",
                "OFFER", "APPLICATION", appId);
    }

    public void notifyOfferAccepted(Long appId, String fullName) {
        // Operations: proceed to disbursement
        deliverToRole(RoleName.OPERATIONS,
                "Offer Accepted — Ready to Disburse",
                fullName + " has accepted the offer for application #" + appId + ". Proceed with disbursement.",
                "OFFER", "APPLICATION", appId);
    }

    public void notifyOfferRejected(Long appId, String fullName) {
        deliverToRole(RoleName.OPERATIONS,
                "Offer Rejected by Applicant",
                fullName + " rejected the offer for application #" + appId + ". Review and revise if needed.",
                "OFFER", "APPLICATION", appId);
    }

    // ── Disbursement ──────────────────────────────────────────────

    public void notifyLoanDisbursed(String email, String fullName, Long loanAccountId, String amount) {
        // Applicant: money is on the way
        deliverToUser(email,
                "Loan Disbursed 🏦",
                "₹" + amount + " has been disbursed to your account. Loan Account: #" + loanAccountId + ".",
                "DISBURSEMENT", "LOAN_ACCOUNT", loanAccountId);
        // Servicing: begin tracking repayments
        deliverToRole(RoleName.SERVICING,
                "New Active Loan — Servicing Required",
                "Loan Account #" + loanAccountId + " for " + fullName + " is now ACTIVE (₹" + amount + " disbursed).",
                "DISBURSEMENT", "LOAN_ACCOUNT", loanAccountId);
    }

    // ── Servicing ─────────────────────────────────────────────────

    public void notifyRepaymentReceived(String email, String fullName, String amount, String refNo) {
        deliverToUser(email,
                "Repayment Received ✓",
                "Your repayment of ₹" + amount + " has been received. Reference: " + refNo + ".",
                "SERVICING", null, null);
    }

    public void notifyEmiDueReminder(String email, String fullName, String amount, String dueDate) {
        deliverToUser(email,
                "EMI Due — ₹" + amount,
                "Your EMI of ₹" + amount + " is due on " + dueDate + ". Please ensure timely payment to avoid penalties.",
                "SERVICING", null, null);
    }

    // ── Collections ───────────────────────────────────────────────

    public void notifyDelinquencyAlert(String email, String fullName, int overdueDays, String amount) {
        // Applicant: overdue warning
        deliverToUser(email,
                "⚠ Overdue EMI — " + overdueDays + " day(s)",
                "Your EMI of ₹" + amount + " is " + overdueDays + " day(s) overdue. A penal charge has been applied.",
                "COLLECTIONS", null, null);
        // Collections team: needs follow-up
        deliverToRole(RoleName.COLLECTIONS,
                "Delinquency Alert — " + overdueDays + " DPD",
                fullName + " has an overdue EMI of ₹" + amount + " (" + overdueDays + " days past due). Follow up required.",
                "COLLECTIONS", null, null);
    }

    public void notifyPtpCreated(String email, String fullName, String promiseDate, String amount) {
        deliverToUser(email,
                "Promise to Pay Recorded",
                "Your PTP of ₹" + amount + " for " + promiseDate + " has been recorded.",
                "COLLECTIONS", null, null);
    }

    // ── Document upload (Agent → Applicant) ───────────────────────

    public void notifyDocumentRequired(String email, String fullName, Long appId) {
        deliverToUser(email,
                "Documents Required for Application #" + appId,
                "Your agent has indicated that documents are required for application #" + appId
                        + ". Please log in to upload the necessary files.",
                "ONBOARDING", "APPLICATION", appId);
    }

    // ═══════════════════════════════════════════════════════════════
    //  REST QUERY METHODS (used by NotificationEndpoints)
    // ═══════════════════════════════════════════════════════════════

    /** Full payload — called only when user opens the notification panel */
    public List<NotificationResponse> getForUser(Long userId) {
        return notifRepo.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream().limit(50).map(this::toDto).collect(Collectors.toList());
    }

    /** Lightweight count — called by the 30-second background poll */
    public long countUnread(Long userId) {
        return notifRepo.countByUser_UserIdAndIsReadFalse(userId);
    }

    public void markAllRead(Long userId) {
        notifRepo.markAllReadForUser(userId);
    }

    public void markOneRead(Long notifId, Long userId) {
        notifRepo.markOneRead(notifId, userId);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CORE DELIVERY — private
    // ═══════════════════════════════════════════════════════════════

    /**
     * User-based: writes exactly ONE notification for the given email address.
     */
    @Async
    public void deliverToUser(String email, String title, String body,
                               String category, String entityType, Long entityId) {
        userRepo.findByEmail(email).ifPresent(user ->
                persist(user, title, body, category, entityType, entityId));
    }

    /**
     * Role-based: writes one notification record per active user in that role.
     * If the role has 10 users → 10 DB rows are created.
     */
    @Async
    public void deliverToRole(RoleName role, String title, String body,
                               String category, String entityType, Long entityId) {
        List<AppUser> targets = userRepo.findActiveByRole(role);
        for (AppUser user : targets) {
            persist(user, title, body, category, entityType, entityId);
        }
        log.info("[NOTIF-ROLE] {} → {} ({} recipients)", title, role, targets.size());
    }

    /** Writes a single UNREAD InAppNotification row to the database */
    private void persist(AppUser user, String title, String body,
                         String category, String entityType, Long entityId) {
        notifRepo.save(InAppNotification.builder()
                .user(user)
                .title(title)
                .message(body)
                .category(category)
                .entityType(entityType)
                .entityId(entityId)
                .isRead(false)
                .build());
        log.debug("[NOTIF] {} → {} ({})", title, user.getEmail(), category);
    }

    private NotificationResponse toDto(InAppNotification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .title(n.getTitle())
                .message(n.getMessage())
                .category(n.getCategory())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
