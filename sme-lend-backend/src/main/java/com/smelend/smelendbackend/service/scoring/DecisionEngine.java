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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Scoring + Auto-Decisioning Engine.
 *
 * Pipeline (runs once when an application is submitted via scoreAndDecide):
 *
 *   Step 1 — Income adequacy check (ScoreBand)
 *             Primary promoter annualised income vs. product.minIncomeAmount.
 *             EXCELLENT : annualisedIncome >= minIncomeAmount  (income-eligible)
 *             POOR      : anything else, including missing data (conservative default)
 *
 *   Step 2 — Weighted scoreValue (300–900, deterministic):
 *             40 % leverage  — requestedAmount / product.maxAmount
 *             30 % tenor     — tenorMonths / product.maxTenorMonths
 *             30 % income    — annualisedIncome / minIncomeAmount  (capped at 1.0)
 *             Higher income + lower leverage/tenor = higher score.
 *             Hard ceiling: POOR-band applications are capped at 699 to preserve
 *             the invariant that UnderwritingService's 700-floor always blocks them.
 *
 *   Step 3 — Eligibility rules (product amount cap, tenor range, custom policies)
 *
 *   Step 4 — Routing:
 *             EXCELLENT band + score >= 750 + eligibility passes → AUTO_APPROVE
 *             Any eligibility failure                             → AUTO_DECLINE
 *             All other cases (POOR band, score 700–749)         → ROUTE_TO_UW
 *
 * Note: UnderwritingService independently hard-blocks UW approval when score < 700.
 *       The POOR-band cap at 699 ensures those two constraints are always consistent.
 */
@Service
public class DecisionEngine {

    private final ScorecardRepository       scorecardRepo;
    private final DecisionRepository        decisionRepo;
    private final LoanApplicationRepository appRepo;
    private final EligibilityService        eligibilityService;
    private final PromoterRepository        promoterRepo;

    public DecisionEngine(ScorecardRepository scorecardRepo,
                          DecisionRepository decisionRepo,
                          LoanApplicationRepository appRepo,
                          EligibilityService eligibilityService,
                          PromoterRepository promoterRepo) {
        this.scorecardRepo      = scorecardRepo;
        this.decisionRepo       = decisionRepo;
        this.appRepo            = appRepo;
        this.eligibilityService = eligibilityService;
        this.promoterRepo       = promoterRepo;
    }

    // ── Public API ────────────────────────────────────────────────────

    @Transactional
    public DecisionResponse scoreAndDecide(Long applicationId) {
        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        // Idempotent: if a decision already exists, return it without re-running the engine
        return decisionRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toDecisionDto)
                .orElseGet(() -> runEngine(app));
    }

    public ScorecardResponse getScorecardByApplication(Long applicationId) {
        return scorecardRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toScoreDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Scorecard not found for application #" + applicationId));
    }

    public DecisionResponse getDecisionByApplication(Long applicationId) {
        return decisionRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toDecisionDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Decision not found for application #" + applicationId));
    }

    // ── Engine ────────────────────────────────────────────────────────

    private DecisionResponse runEngine(LoanApplication app) {

        // Step 1 + 2: Score (income adequacy + weighted formula)
        Scorecard sc = buildScorecard(app);
        scorecardRepo.save(sc);

        // Step 3: Eligibility
        var eligibility = eligibilityService.checkApplication(app.getApplicationId());

        // Step 4: Route
        DecisionPath    path;
        ApplicationStatus newStatus;
        String          reason;

        if (!eligibility.isEligible()) {
            path      = DecisionPath.AUTO_DECLINE;
            newStatus = ApplicationStatus.UW_REJECTED;
            reason    = "Eligibility failed: " + eligibility.getSummary();

        } else if (sc.getScoreBand() == ScoreBand.EXCELLENT && sc.getScoreValue() >= 750) {
            path      = DecisionPath.AUTO_APPROVE;
            newStatus = ApplicationStatus.AUTO_APPROVED;
            reason    = "Income adequate, score " + sc.getScoreValue()
                        + " (EXCELLENT, ≥750) — auto-approved.";

        } else {
            path      = DecisionPath.ROUTE_TO_UW;
            newStatus = ApplicationStatus.ROUTED_TO_UW;
            reason    = buildUwReason(sc);
        }

        app.setStatus(newStatus);
        appRepo.save(app);

        Decision decision = Decision.builder()
                .application(app)
                .path(path)
                .reason(reason)
                .triggeredRules(String.join("; ", eligibility.getFailedRules()))
                .decidedAt(LocalDateTime.now())
                .build();

        return toDecisionDto(decisionRepo.save(decision));
    }

    // ── Scorecard builder ─────────────────────────────────────────────

    private Scorecard buildScorecard(LoanApplication app) {
        LoanProduct prod = app.getProduct();

        // ── Income adequacy (ScoreBand) ──────────────────────────────
        Promoter  primary         = findPrimaryPromoter(app);
        BigDecimal annualisedIncome = annualise(primary);
        BigDecimal minIncome        = prod.getMinIncomeAmount();

        boolean incomeAdequate = minIncome != null
                && minIncome.compareTo(BigDecimal.ZERO) > 0
                && annualisedIncome.compareTo(minIncome) >= 0;

        ScoreBand band = incomeAdequate ? ScoreBand.EXCELLENT : ScoreBand.POOR;

        // ── Weighted score (300–900) ─────────────────────────────────
        double leverage    = safeDivide(app.getRequestedAmount(), prod.getMaxAmount());
        double tenorRatio  = safeDivide(app.getTenorMonths(), prod.getMaxTenorMonths());
        double incomeRatio = (minIncome != null && minIncome.compareTo(BigDecimal.ZERO) > 0)
                ? Math.min(1.0, annualisedIncome.doubleValue() / minIncome.doubleValue())
                : 0.0;

        // Formula: 900 base, subtract leverage/tenor risk, add income benefit, apply offset
        //   leverage  40% weight — max penalty -240
        //   tenor     30% weight — max penalty -180
        //   income    30% weight — max benefit +180
        //   offset    -180       — anchors neutral mid-risk case to ~600
        double raw   = 900 - (leverage * 240) - (tenorRatio * 180) + (incomeRatio * 180) - 180;
        int    score = Math.max(300, Math.min(900, (int) Math.round(raw)));

        // Hard cap: POOR-band applications cannot exceed 699.
        // This guarantees the UnderwritingService 700-floor always blocks them,
        // regardless of how favourable the leverage/tenor values are.
        if (band == ScoreBand.POOR) {
            score = Math.min(score, 699);
        }

        String inputsJson = String.format(
                "{\"requestedAmount\":%s,\"tenorMonths\":%d,\"productId\":%d,"
                + "\"leverage\":%.3f,\"tenorRatio\":%.3f,"
                + "\"annualisedIncome\":%s,\"minIncomeAmount\":%s,"
                + "\"incomeRatio\":%.3f,\"band\":\"%s\"}",
                app.getRequestedAmount().toPlainString(),
                app.getTenorMonths(),
                prod.getProductId(),
                leverage, tenorRatio,
                annualisedIncome.toPlainString(),
                minIncome != null ? minIncome.toPlainString() : "null",
                incomeRatio,
                band.name());

        return Scorecard.builder()
                .application(app)
                .modelVersion("v2.0-income-weighted")
                .inputsJson(inputsJson)
                .scoreValue(score)
                .scoreBand(band)
                .scoredAt(LocalDateTime.now())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Promoter findPrimaryPromoter(LoanApplication app) {
        List<Promoter> promoters = promoterRepo.findBySme_SmeId(app.getSme().getSmeId());
        if (promoters.isEmpty()) return null;
        return promoters.stream()
                .max(Comparator.comparing(Promoter::getOwnershipPct))
                .orElse(promoters.get(0));
    }

    private BigDecimal annualise(Promoter promoter) {
        if (promoter == null || promoter.getMonthlyIncome() == null) return BigDecimal.ZERO;
        return promoter.getMonthlyIncome().multiply(BigDecimal.valueOf(12));
    }

    private double safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP).doubleValue();
    }

    private double safeDivide(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private String buildUwReason(Scorecard sc) {
        if (sc.getScoreBand() == ScoreBand.POOR) {
            return "Income inadequate (POOR band, score " + sc.getScoreValue()
                   + ") — routed to underwriter. Note: UW approval blocked for score < 700.";
        }
        return "Income adequate (EXCELLENT band) but score " + sc.getScoreValue()
               + " is below 750 threshold — routed to underwriter for manual review.";
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
