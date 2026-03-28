package com.smelend.smelendbackend.mapper;

import com.smelend.smelendbackend.dto.product.LoanProductResponse;
import com.smelend.smelendbackend.entity.LoanProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LoanProductMapper {
    LoanProductResponse toResponse(LoanProduct loanProduct);
}