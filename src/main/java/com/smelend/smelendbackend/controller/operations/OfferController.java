package com.smelend.smelendbackend.controller.operations;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.operations.offer.CreateOfferRequest;
import com.smelend.smelendbackend.dto.operations.offer.OfferResponse;
import com.smelend.smelendbackend.service.operations.OfferService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    // OPERATIONS creates offer
    @PostMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('OPERATIONS')")
    public ApiResponse<OfferResponse> createOffer(
            @PathVariable Long applicationId,
            @Valid @RequestBody CreateOfferRequest req
    ) {
        return ApiResponse.ok("Offer created", offerService.createOffer(applicationId, req));
    }

    // Applicant/Agent accepts offer
    @PatchMapping("/{offerId}/accept")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<OfferResponse> accept(@PathVariable Long offerId) {
        return ApiResponse.ok("Offer accepted", offerService.acceptOffer(offerId));
    }

    // Applicant/Agent rejects offer
    @PatchMapping("/{offerId}/reject")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<OfferResponse> reject(@PathVariable Long offerId) {
        return ApiResponse.ok("Offer rejected", offerService.rejectOffer(offerId));
    }

    // View offer
    @GetMapping("/{offerId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','OPERATIONS')")
    public ApiResponse<OfferResponse> get(@PathVariable Long offerId) {
        return ApiResponse.ok("Offer fetched", offerService.get(offerId));
    }

    // List offers (Ops/Admin = all, Applicant/Agent = only mine)
    @GetMapping
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','OPERATIONS')")
    public ApiResponse<List<OfferResponse>> list() {
        return ApiResponse.ok("Offers fetched", offerService.listMineOrAll());
    }
}