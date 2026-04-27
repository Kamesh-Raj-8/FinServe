package com.smelend.smelendbackend.service.charge;

import com.smelend.smelendbackend.dto.charge.*;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChargeService {

    private final ChargeRepository      chargeRepo;
    private final LoanAccountRepository loanRepo;
    private final CurrentUserService    currentUserService;

    public ChargeService(ChargeRepository chargeRepo,
                         LoanAccountRepository loanRepo,
                         CurrentUserService currentUserService) {
        this.chargeRepo         = chargeRepo;
        this.loanRepo           = loanRepo;
        this.currentUserService = currentUserService;
    }

    // ── Manual charge posting (Ops / Collections) ─────────────────────

    public ChargeResponse post(ChargeRequest req) {
        // COLLECTIONS may only post PENAL charges — standard fee types are
        // the exclusive domain of OPERATIONS to prevent misuse.
        if (currentUserService.hasRole("COLLECTIONS")
                && req.getChargeType() != ChargeType.PENAL) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "COLLECTIONS role may only post PENAL charges. "
                    + "Attempted type: " + req.getChargeType()
                    + ". Standard fee charges must be raised by OPERATIONS.");
        }

        LoanAccount loan = loanRepo.findById(req.getLoanAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));
        Charge c = Charge.builder()
                .loanAccount(loan)
                .chargeType(req.getChargeType())
                .amount(req.getAmount())
                .description(req.getDescription())
                .chargeDate(req.getChargeDate() != null ? req.getChargeDate() : LocalDate.now())
                .status(ChargeStatus.OUTSTANDING)
                .createdAt(LocalDateTime.now())
                .build();
        return toDto(chargeRepo.save(c));
    }

    /** Auto-post a processing fee charge at disbursement. */
    public ChargeResponse postProcessingFee(LoanAccount loan, BigDecimal feeAmount) {
        Charge c = Charge.builder()
                .loanAccount(loan)
                .chargeType(ChargeType.PROCESSING)
                .amount(feeAmount)
                .description("Processing fee deducted at disbursement")
                .chargeDate(LocalDate.now())
                .status(ChargeStatus.OUTSTANDING)
                .createdAt(LocalDateTime.now())
                .build();
        return toDto(chargeRepo.save(c));
    }

    /**
     * Auto-post penal charges for overdue installments.
     * Rate = product.delinquencyFinePerDay × DPD.
     */
    public ChargeResponse postPenalCharge(LoanAccount loan, int dpd, BigDecimal finePerDay) {
        BigDecimal amount = finePerDay.multiply(BigDecimal.valueOf(dpd))
                .setScale(2, RoundingMode.HALF_UP);
        Charge c = Charge.builder()
                .loanAccount(loan)
                .chargeType(ChargeType.PENAL)
                .amount(amount)
                .description("Penal charge: " + dpd + " DPD × ₹" + finePerDay + "/day")
                .chargeDate(LocalDate.now())
                .status(ChargeStatus.OUTSTANDING)
                .createdAt(LocalDateTime.now())
                .build();
        return toDto(chargeRepo.save(c));
    }

    public List<ChargeResponse> listByLoanAccount(Long loanAccountId) {
        return chargeRepo.findByLoanAccount_LoanAccountId(loanAccountId).stream().map(this::toDto).toList();
    }

    public ChargeResponse waive(Long chargeId) {
        Charge c = chargeRepo.findById(chargeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Charge not found"));
        c.setStatus(ChargeStatus.WAIVED);
        return toDto(chargeRepo.save(c));
    }

    public BigDecimal totalOutstanding(Long loanAccountId) {
        return chargeRepo.findByLoanAccount_LoanAccountIdAndStatus(loanAccountId, ChargeStatus.OUTSTANDING)
                .stream().map(Charge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ChargeResponse toDto(Charge c) {
        return ChargeResponse.builder()
                .chargeId(c.getChargeId())
                .loanAccountId(c.getLoanAccount().getLoanAccountId())
                .chargeType(c.getChargeType().name())
                .amount(c.getAmount())
                .description(c.getDescription())
                .chargeDate(c.getChargeDate())
                .status(c.getStatus().name())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
