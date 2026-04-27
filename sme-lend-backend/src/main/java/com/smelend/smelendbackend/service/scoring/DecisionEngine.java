package com.smelend.smelendbackend.service.scoring;

import com.smelend.smelendbackend.dto.scoring.DecisionResponse;
import com.smelend.smelendbackend.dto.scoring.ScorecardResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.eligibility.EligibilityService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scoring + Auto-Decisioning Engine.
 *
 * Called when an application is submitted.
 * 1. Compute risk scorecard (deterministic weighted model, Phase-1 proxy).
 * 2. Run eligibility checks.
 * 3. Produce a Decision record that routes the application:
 *    - EXCELLENT score (≥800) + all rules pass → AUTO_APPROVE
 *      (HIGH, MEDIUM, LOW all go to ROUTE_TO_UW for human review)
 *    - Any eligibility failure → AUTO_DECLINE
 *    - Medium/Low score → ROUTE_TO_UW for human review
 */
@Service
public class DecisionEngine {

    private final ScorecardRepository    scorecardRepo;
    private final DecisionRepository     decisionRepo;
    private final LoanApplicationRepository appRepo;
    private final EligibilityService     eligibilityService;

    public DecisionEngine(ScorecardRepository scorecardRepo,
                           DecisionRepository decisionRepo,
                           LoanApplicationRepository appRepo,
                           EligibilityService eligibilityService) {
        this.scorecardRepo      = scorecardRepo;
        this.decisionRepo       = decisionRepo;
        this.appRepo            = appRepo;
        this.eligibilityService = eligibilityService;
    }

    // ── Score + Decide (called at submission) ─────────────────────────

    @Transactional
    public DecisionResponse scoreAndDecide(Long applicationId) {
        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        // Idempotent
        return decisionRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toDecisionDto)
                .orElseGet(() -> runEngine(app));
    }

    private DecisionResponse runEngine(LoanApplication app) {
        Long appId = app.getApplicationId();

        // ── Step 1: Compute scorecard (idempotent — update if exists) ──
        Scorecard sc = computeScorecard(app);
        scorecardRepo.findByApplication_ApplicationId(app.getApplicationId())
                .ifPresentOrElse(existing -> {
                    existing.setScoreValue(sc.getScoreValue());
                    existing.setScoreBand(sc.getScoreBand());
                    existing.setInputsJson(sc.getInputsJson());
                    existing.setScoredAt(sc.getScoredAt());
                    scorecardRepo.save(existing);
                    sc.setScoreId(existing.getScoreId());
                }, () -> scorecardRepo.save(sc));

        // ── Step 2: Eligibility check ─────────────────────────────────
        var eligibility = eligibilityService.checkApplication(appId);
        List<String> triggeredRules = new ArrayList<>(eligibility.getFailedRules());

        // ── Step 3: Route decision ─────────────────────────────────────
        DecisionPath path;
        String reason;

        if (!eligibility.isEligible()) {
            path   = DecisionPath.AUTO_DECLINE;
            reason = "Eligibility check failed: " + eligibility.getSummary();
        } else if (sc.getScoreValue() >= 750) {
            // Threshold: HIGH/EXCELLENT (≥750) + full eligibility → auto-approve.
            // Bypasses manual UW queue; offer is generated automatically in ApplicationService.
            path   = DecisionPath.AUTO_APPROVE;
            reason = "Score " + sc.getScoreValue() + " (" + sc.getScoreBand()
                     + " ≥750) + full eligibility — auto-approved, offer will be generated.";
            app.setStatus(ApplicationStatus.AUTO_APPROVED);
            appRepo.save(app);
        } else {
            // LOW (<600) and MEDIUM (600-749) → human underwriter review required.
            path   = DecisionPath.ROUTE_TO_UW;
            reason = "Score " + sc.getScoreValue() + " (" + sc.getScoreBand() + ") — routed to underwriter.";
        }

        Decision decision = Decision.builder()
                .application(app)
                .path(path)
                .reason(reason)
                .triggeredRules(String.join("; ", triggeredRules))
                .decidedAt(LocalDateTime.now())
                .build();

        return toDecisionDto(decisionRepo.save(decision));
    }

    /**
     * Deterministic scoring model (Phase-1 proxy).
     * Production: replace with ML model call / bureau integration.
     *
     * Score = 300–900 weighted:
     *   40% leverage ratio (requestedAmount / maxAmount)
     *   30% tenor ratio (tenorMonths / maxTenor) — shorter = less risk
     *   30% entity seed (stable per applicationId)
     */
    Scorecard computeScorecard(LoanApplication app) {
        LoanProduct prod = app.getProduct();
        double leverage  = app.getRequestedAmount().doubleValue() / prod.getMaxAmount().doubleValue();
        double tenorRatio = (double) app.getTenorMonths() / prod.getMaxTenorMonths();
        double seed      = Math.abs(Math.sin(app.getApplicationId() * 6364136.0)) * 0.3;

        int raw = (int) (900 - (leverage * 240) - (tenorRatio * 180) + (seed * 60));
        int score = Math.max(300, Math.min(900, raw));

        ScoreBand band;
        if      (score >= 800) band = ScoreBand.EXCELLENT;
        else if (score >= 700) band = ScoreBand.HIGH;
        else if (score >= 600) band = ScoreBand.MEDIUM;
        else                   band = ScoreBand.LOW;

        String inputs = String.format(
            "{\"requestedAmount\":%s,\"tenorMonths\":%d,\"productId\":%d,\"leverage\":%.2f,\"tenorRatio\":%.2f}",
            app.getRequestedAmount().toPlainString(), app.getTenorMonths(),
            prod.getProductId(), leverage, tenorRatio);

        return Scorecard.builder()
                .application(app)
                .modelVersion("v1.0-phase1")
                .inputsJson(inputs)
                .scoreValue(score)
                .scoreBand(band)
                .scoredAt(LocalDateTime.now())
                .build();
    }

    // ── Query ─────────────────────────────────────────────────────────

    public ScorecardResponse getScorecardByApplication(Long applicationId) {
        return scorecardRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toScoreDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scorecard not found for application #" + applicationId));
    }

    public DecisionResponse getDecisionByApplication(Long applicationId) {
        return decisionRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toDecisionDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Decision not found for application #" + applicationId));
    }

    // ── Mappers ───────────────────────────────────────────────────────

    private ScorecardResponse toScoreDto(Scorecard s) {
        return ScorecardResponse.builder()
                .scoreId(s.getScoreId())
                .applicationId(s.getApplication().getApplicationId())
                .modelVersion(s.getModelVersion())
                .inputsJson(s.getInputsJson())
                .scoreValue(s.getScoreValue())
                .scoreBand(s.getScoreBand().name())
                .scoredAt(s.getScoredAt())
                .build();
    }

    private DecisionResponse toDecisionDto(Decision d) {
        return DecisionResponse.builder()
                .decisionId(d.getDecisionId())
                .applicationId(d.getApplication().getApplicationId())
                .path(d.getPath().name())
                .reason(d.getReason())
                .triggeredRules(d.getTriggeredRules())
                .decidedAt(d.getDecidedAt())
                .build();
    }
}
