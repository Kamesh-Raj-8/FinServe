package com.smelend.smelendbackend.service.eligibility;

import com.smelend.smelendbackend.dto.eligibility.*;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EligibilityService {

    private final EligibilityPolicyRepository policyRepo;
    private final LoanProductRepository       productRepo;
    private final LoanApplicationRepository   appRepo;

    public EligibilityService(EligibilityPolicyRepository policyRepo,
                               LoanProductRepository productRepo,
                               LoanApplicationRepository appRepo) {
        this.policyRepo  = policyRepo;
        this.productRepo = productRepo;
        this.appRepo     = appRepo;
    }

    public EligibilityPolicyResponse create(EligibilityPolicyRequest req) {
        LoanProduct product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        EligibilityPolicy p = EligibilityPolicy.builder()
                .product(product)
                .ruleName(req.getRuleName())
                .ruleExpression(req.getRuleExpression())
                .maxAmountCap(req.getMaxAmountCap())
                .minCreditScore(req.getMinCreditScore())
                .minBusinessVintageMonths(req.getMinBusinessVintageMonths())
                .maxExistingLoans(req.getMaxExistingLoans())
                .minDscr(req.getMinDscr())
                .status(StatusFlag.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        return toDto(policyRepo.save(p));
    }

    public List<EligibilityPolicyResponse> listByProduct(Long productId) {
        return policyRepo.findByProduct_ProductId(productId).stream().map(this::toDto).toList();
    }

    public List<EligibilityPolicyResponse> listAll() {
        return policyRepo.findAll().stream().map(this::toDto).toList();
    }

    public void deactivate(Long policyId) {
        EligibilityPolicy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));
        p.setStatus(StatusFlag.INACTIVE);
        policyRepo.save(p);
    }

    public EligibilityCheckResult checkApplication(Long applicationId) {
        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        List<EligibilityPolicy> policies = policyRepo.findByProduct_ProductIdAndStatus(
                app.getProduct().getProductId(), StatusFlag.ACTIVE);

        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        LoanProduct prod = app.getProduct();
        BigDecimal amount = app.getRequestedAmount();
        int tenor = app.getTenorMonths();

        for (EligibilityPolicy pol : policies) {
            boolean pass = true;
            StringBuilder reason = new StringBuilder(pol.getRuleName() + ": ");

            if (pol.getMaxAmountCap() != null && amount.compareTo(pol.getMaxAmountCap()) > 0) {
                pass = false;
                reason.append("Amount ").append(amount).append(" > cap ").append(pol.getMaxAmountCap());
            }

            if (amount.compareTo(prod.getMinAmount()) < 0 || amount.compareTo(prod.getMaxAmount()) > 0) {
                pass = false;
                reason.append("Amount outside product range [").append(prod.getMinAmount())
                      .append(", ").append(prod.getMaxAmount()).append("]");
            }

            if (tenor < prod.getMinTenorMonths() || tenor > prod.getMaxTenorMonths()) {
                pass = false;
                reason.append("Tenor ").append(tenor).append(" outside product range");
            }

            if (pass) passed.add(pol.getRuleName());
            else      failed.add(reason.toString());
        }

        if (policies.isEmpty()) {
            if (amount.compareTo(prod.getMinAmount()) < 0 || amount.compareTo(prod.getMaxAmount()) > 0)
                failed.add("Amount outside product range");
            else passed.add("Product amount range check");

            if (tenor < prod.getMinTenorMonths() || tenor > prod.getMaxTenorMonths())
                failed.add("Tenor outside product range");
            else passed.add("Product tenor range check");
        }

        boolean eligible = failed.isEmpty();
        return EligibilityCheckResult.builder()
                .eligible(eligible)
                .passedRules(passed)
                .failedRules(failed)
                .summary(eligible
                        ? "All " + passed.size() + " eligibility checks passed."
                        : failed.size() + " check(s) failed: " + String.join("; ", failed))
                .build();
    }

    private EligibilityPolicyResponse toDto(EligibilityPolicy p) {
        return EligibilityPolicyResponse.builder()
                .policyId(p.getPolicyId())
                .productId(p.getProduct().getProductId())
                .productName(p.getProduct().getProductName())
                .ruleName(p.getRuleName())
                .ruleExpression(p.getRuleExpression())
                .maxAmountCap(p.getMaxAmountCap())
                .minCreditScore(p.getMinCreditScore())
                .minBusinessVintageMonths(p.getMinBusinessVintageMonths())
                .maxExistingLoans(p.getMaxExistingLoans())
                .minDscr(p.getMinDscr())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
