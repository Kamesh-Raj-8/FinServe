package com.smelend.smelendbackend.dto.eligibility;
import lombok.*;
import java.util.List;

/** Result returned after validating an application against all product policies. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EligibilityCheckResult {
    private boolean eligible;
    private List<String> passedRules;
    private List<String> failedRules;
    private String summary;
}
