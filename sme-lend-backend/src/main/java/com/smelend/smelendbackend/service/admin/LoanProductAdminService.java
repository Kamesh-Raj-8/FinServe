package com.smelend.smelendbackend.service.admin;

import com.smelend.smelendbackend.dto.product.LoanProductRequest;
import com.smelend.smelendbackend.dto.product.LoanProductResponse;
import com.smelend.smelendbackend.entity.LoanProduct;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.mapper.LoanProductMapper;
import com.smelend.smelendbackend.repository.LoanProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanProductAdminService {

    private final LoanProductRepository productRepo;
    private final LoanProductMapper loanProductMapper;

    public LoanProductAdminService(LoanProductRepository productRepo,
                                   LoanProductMapper loanProductMapper) {
        this.productRepo = productRepo;
        this.loanProductMapper = loanProductMapper;
    }

    public LoanProductResponse create(LoanProductRequest req) {
        validateRanges(req);

        LoanProduct saved = productRepo.save(
                LoanProduct.builder()
                        .productName(req.getProductName())
                        .minAmount(req.getMinAmount())
                        .maxAmount(req.getMaxAmount())
                        .minTenorMonths(req.getMinTenorMonths())
                        .maxTenorMonths(req.getMaxTenorMonths())
                        .baseInterestRate(req.getBaseInterestRate())
                        .creditThreshold(req.getCreditThreshold())
                        .minIncomeAmount(req.getMinIncomeAmount())
                        .maxIncomeAmount(req.getMaxIncomeAmount())
                        .status(StatusFlag.ACTIVE)
                        .build()
        );

        return loanProductMapper.toResponse(saved);
    }

    public LoanProductResponse update(Long productId, LoanProductRequest req) {
        validateRanges(req);

        LoanProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan product not found"));

        product.setProductName(req.getProductName());
        product.setMinAmount(req.getMinAmount());
        product.setMaxAmount(req.getMaxAmount());
        product.setMinTenorMonths(req.getMinTenorMonths());
        product.setMaxTenorMonths(req.getMaxTenorMonths());
        product.setBaseInterestRate(req.getBaseInterestRate());
        product.setCreditThreshold(req.getCreditThreshold());
        product.setMinIncomeAmount(req.getMinIncomeAmount());
        product.setMaxIncomeAmount(req.getMaxIncomeAmount());

        return loanProductMapper.toResponse(productRepo.save(product));
    }

    public LoanProductResponse get(Long productId) {
        LoanProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan product not found"));

        return loanProductMapper.toResponse(product);
    }

    public List<LoanProductResponse> list() {
        return productRepo.findAll().stream()
                .map(loanProductMapper::toResponse)
                .toList();
    }

    public LoanProductResponse setStatus(Long productId, StatusFlag status) {
        LoanProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan product not found"));

        product.setStatus(status);
        return loanProductMapper.toResponse(productRepo.save(product));
    }

    public void delete(Long productId) {
        LoanProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan product not found"));
        product.setStatus(StatusFlag.INACTIVE);
        productRepo.save(product);
    }

    private void validateRanges(LoanProductRequest req) {
        if (req.getMinAmount().compareTo(req.getMaxAmount()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "minAmount cannot be greater than maxAmount");
        }
        if (req.getMinTenorMonths() > req.getMaxTenorMonths()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "minTenorMonths cannot be greater than maxTenorMonths");
        }
    }
}