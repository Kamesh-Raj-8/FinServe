package com.smelend.smelendbackend.service.fee;
 
import com.smelend.smelendbackend.dto.fee.*;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
 
@Service
public class FeeService {
 
    private final FeeConfigRepository feeRepo;
    private final LoanProductRepository productRepo;
 
    public FeeService(FeeConfigRepository feeRepo, LoanProductRepository productRepo) {
        this.feeRepo     = feeRepo;
        this.productRepo = productRepo;
    }

 
    public FeeConfigResponse create(FeeConfigRequest req) {
        LoanProduct product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        FeeConfig fee = FeeConfig.builder()
                .product(product)
                .feeType(req.getFeeType())
                .feeMode(req.getFeeMode())
                .value(req.getValue())
                .effectiveFrom(req.getEffectiveFrom())
                .effectiveTo(req.getEffectiveTo())
                .status(StatusFlag.ACTIVE)
                .build();
        return toDto(feeRepo.save(fee));
    }
 
    public List<FeeConfigResponse> listByProduct(Long productId) {
        return feeRepo.findByProduct_ProductId(productId).stream().map(this::toDto).toList();
    }
 
    public List<FeeConfigResponse> listAll() {
        return feeRepo.findAll().stream().map(this::toDto).toList();
    }
 
    public void deactivate(Long feeId) {
        FeeConfig f = feeRepo.findById(feeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fee config not found"));
        f.setStatus(StatusFlag.INACTIVE);
        feeRepo.save(f);
    }

    public List<AppliedFeeDto> calculateFees(Long productId, BigDecimal sanctionedAmount) {
        LocalDate today = LocalDate.now();

        List<FeeConfig> configs = feeRepo.findByProduct_ProductIdAndStatus(productId, StatusFlag.ACTIVE)
                .stream()
                .filter(f -> f.getFeeType() == FeeType.PENAL || (
                           (f.getEffectiveFrom() == null || !today.isBefore(f.getEffectiveFrom()))
&& (f.getEffectiveTo()   == null || !today.isAfter(f.getEffectiveTo()))
                ))
                .toList();
 
        List<AppliedFeeDto> applied = new ArrayList<>();
        for (FeeConfig f : configs) {
            BigDecimal amount = f.getFeeMode() == FeeMode.FLAT
                    ? f.getValue()
                    : sanctionedAmount.multiply(f.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
 
            applied.add(AppliedFeeDto.builder()
                    .feeType(f.getFeeType().name())
                    .feeMode(f.getFeeMode().name())
                    .configuredValue(f.getValue())
                    .calculatedAmount(amount)
                    .build());
        }
        return applied;
    }
 
    public BigDecimal totalFees(Long productId, BigDecimal sanctionedAmount) {
        return calculateFees(productId, sanctionedAmount).stream()
                .map(AppliedFeeDto::getCalculatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
 
    private FeeConfigResponse toDto(FeeConfig f) {
        return FeeConfigResponse.builder()
                .feeId(f.getFeeId())
                .productId(f.getProduct().getProductId())
                .productName(f.getProduct().getProductName())
                .feeType(f.getFeeType())
                .feeMode(f.getFeeMode())
                .value(f.getValue())
                .effectiveFrom(f.getEffectiveFrom())
                .effectiveTo(f.getEffectiveTo())
                .status(f.getStatus().name())
                .build();
    }
}