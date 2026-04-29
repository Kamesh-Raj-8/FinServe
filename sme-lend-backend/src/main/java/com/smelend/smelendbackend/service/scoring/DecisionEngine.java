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

    @Transactional
    public DecisionResponse scoreAndDecide(Long applicationId) {
        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        return decisionRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toDecisionDto)
                .orElseGet(() -> runEngine(app));
    }

    @Transactional(readOnly = true)
    public ScorecardResponse getScorecardByApplication(Long applicationId) {
        return scorecardRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toScoreDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Scorecard not found for application #" + applicationId));
    }

    @Transactional(readOnly = true)
    public DecisionResponse getDecisionByApplication(Long applicationId) {
        return decisionRepo.findByApplication_ApplicationId(applicationId)
                .map(this::toDecisionDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Decision not found for application #" + applicationId));
    }

    private DecisionResponse runEngine(LoanApplication app) {
        Scorecard sc = buildScorecard(app);
        scorecardRepo.save(sc);
        var eligibility = eligibilityService.checkApplication(app.getApplicationId());
        DecisionPath      path;
        ApplicationStatus newStatus;
        String            reason;

        int threshold = resolveThreshold(app.getProduct());

        if (!eligibility.isEligible()) {
            path      = DecisionPath.AUTO_DECLINE;
            newStatus = ApplicationStatus.UW_REJECTED;
            reason    = "Eligibility failed: " + eligibility.getSummary();

        } else if (sc.getScoreBand() == ScoreBand.EXCELLENT) {
            path      = DecisionPath.AUTO_APPROVE;
            newStatus = ApplicationStatus.AUTO_APPROVED;
            reason    = "Score " + sc.getScoreValue() + " (EXCELLENT, ≥ " + (750)
                        + ") — auto-approved.";

        } else {
            path      = DecisionPath.ROUTE_TO_UW;
            newStatus = ApplicationStatus.ROUTED_TO_UW;
            reason    = buildUwReason(sc, threshold);
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

    private Scorecard buildScorecard(LoanApplication app) {
        LoanProduct prod     = app.getProduct();
        int         threshold = resolveThreshold(prod);
        Promoter   primary         = findPrimaryPromoter(app);
        BigDecimal annualisedIncome = annualise(primary);
        BigDecimal minIncome        = prod.getMinIncomeAmount();

        double leverage    = safeDivide(app.getRequestedAmount(), prod.getMaxAmount());
        double tenorRatio  = safeDivide(app.getTenorMonths(), prod.getMaxTenorMonths());
        double incomeRatio = (minIncome != null && minIncome.compareTo(BigDecimal.ZERO) > 0)
                ? Math.min(1.0, annualisedIncome.doubleValue() / minIncome.doubleValue())
                : 0.0;
        double raw   = 900 - (leverage * 240) - (tenorRatio * 180) + (incomeRatio * 180) - 180;
        int    score = Math.max(300, Math.min(900, (int) Math.round(raw)));

        ScoreBand band;
        if (score < threshold) {
            band = ScoreBand.POOR;
        } else if (score < 750) {
            band = ScoreBand.FAIR;
        } else {
            band = ScoreBand.EXCELLENT;
        }

        String inputsJson = String.format(
                "{\"requestedAmount\":%s,\"tenorMonths\":%d,\"productId\":%d,"
                + "\"leverage\":%.3f,\"tenorRatio\":%.3f,"
                + "\"annualisedIncome\":%s,\"minIncomeAmount\":%s,"
                + "\"incomeRatio\":%.3f,\"thresholdScore\":%d,\"band\":\"%s\"}",
                app.getRequestedAmount().toPlainString(),
                app.getTenorMonths(),
                prod.getProductId(),
                leverage, tenorRatio,
                annualisedIncome.toPlainString(),
                minIncome != null ? minIncome.toPlainString() : "null",
                incomeRatio,
                threshold,
                band.name());

        return Scorecard.builder()
                .application(app)
                .modelVersion("v2.1-threshold-dynamic")
                .inputsJson(inputsJson)
                .scoreValue(score)
                .scoreBand(band)
                .scoredAt(LocalDateTime.now())
                .build();
    }

    int resolveThreshold(LoanProduct product) {
        BigDecimal t = product.getCreditThreshold();
        return (t != null && t.compareTo(BigDecimal.ZERO) > 0) ? t.intValue() : 700;
    }

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

    private String buildUwReason(Scorecard sc, int threshold) {
        if (sc.getScoreBand() == ScoreBand.POOR) {
            return "Score " + sc.getScoreValue() + " is below the product threshold (" + threshold
                   + ") — routed to underwriter. Note: UW approval is blocked for POOR-band applications.";
        }
        return "Score " + sc.getScoreValue() + " is within the manual review range ["
               + threshold + "–" + (749) + "] (FAIR) — routed to underwriter for manual review.";
    }

    private ScorecardResponse toScoreDto(Scorecard s) {
        int threshold = resolveThreshold(s.getApplication().getProduct());
        return ScorecardResponse.builder()
                .scoreId(s.getScoreId())
                .applicationId(s.getApplication().getApplicationId())
                .modelVersion(s.getModelVersion())
                .inputsJson(s.getInputsJson())
                .scoreValue(s.getScoreValue())
                .scoreBand(s.getScoreBand().name())
                .scoredAt(s.getScoredAt())
                .thresholdScore(threshold)
                .isApproveDisabled(s.getScoreBand() == ScoreBand.POOR)
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
