package com.banking.mapper;

import com.banking.dto.BeneficiaryDtos;
import com.banking.entity.Beneficiary;

public final class BeneficiaryMapper {

    private BeneficiaryMapper() {
    }

    public static BeneficiaryDtos.BeneficiaryResponse toResponse(Beneficiary b) {
        return new BeneficiaryDtos.BeneficiaryResponse(
                b.getId(),
                b.getName(),
                b.getAccountNumber(),
                b.getBankName(),
                b.getBranchName(),
                b.getIfscCode(),
                b.getNickname(),
                b.getDailyLimit(),
                b.isActive(),
                b.getCreatedAt());
    }
}
