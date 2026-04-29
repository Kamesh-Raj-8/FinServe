package com.smelend.smelendbackend.controller;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.product.LoanProductResponse;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.repository.LoanProductRepository;
import com.smelend.smelendbackend.mapper.LoanProductMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/loan-products")
@PreAuthorize("isAuthenticated()")
public class LoanProductPublicController {

    private final LoanProductRepository productRepo;
    private final LoanProductMapper productMapper;

    public LoanProductPublicController(LoanProductRepository productRepo,
                                       LoanProductMapper productMapper) {
        this.productRepo = productRepo;
        this.productMapper = productMapper;
    }

    @GetMapping
    public ApiResponse<List<LoanProductResponse>> listActive() {
        List<LoanProductResponse> products = productRepo.findAll().stream()
                .filter(p -> p.getStatus() == StatusFlag.ACTIVE)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok("Active loan products fetched", products);
    }
}
