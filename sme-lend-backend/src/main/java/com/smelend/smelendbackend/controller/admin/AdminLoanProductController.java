package com.smelend.smelendbackend.controller.admin;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.product.LoanProductRequest;
import com.smelend.smelendbackend.dto.product.LoanProductResponse;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.service.admin.LoanProductAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/loan-products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoanProductController {

    private final LoanProductAdminService productService;

    public AdminLoanProductController(LoanProductAdminService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ApiResponse<LoanProductResponse> create(@Valid @RequestBody LoanProductRequest req) {
        return ApiResponse.ok("Loan product created", productService.create(req));
    }

    @PutMapping("/{productId}")
    public ApiResponse<LoanProductResponse> update(
            @PathVariable Long productId,
            @Valid @RequestBody LoanProductRequest req
    ) {
        return ApiResponse.ok("Loan product updated", productService.update(productId, req));
    }

    @GetMapping("/{productId}")
    public ApiResponse<LoanProductResponse> get(@PathVariable Long productId) {
        return ApiResponse.ok("Loan product fetched", productService.get(productId));
    }

    @GetMapping
    public ApiResponse<List<LoanProductResponse>> list() {
        return ApiResponse.ok("Loan products fetched", productService.list());
    }

    @PatchMapping("/{productId}/status")
    public ApiResponse<LoanProductResponse> setStatus(
            @PathVariable Long productId,
            @RequestParam StatusFlag status
    ) {
        return ApiResponse.ok("Loan product status updated", productService.setStatus(productId, status));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> delete(@PathVariable Long productId) {
        productService.delete(productId);
        return ApiResponse.ok("Loan product deleted");
    }
}