package com.smelend.smelendbackend.dto.document;

import com.smelend.smelendbackend.entity.enums.DocType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
@Data
@Builder
@Getter @Setter
public class AddDocumentRequest {

    @NotNull
    private DocType docType;

    @NotBlank
    private String fileUri; // Phase-1 metadata only (no real upload)
}