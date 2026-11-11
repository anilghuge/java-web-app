package com.banking.service;

import com.banking.dto.BeneficiaryDtos;

import java.util.List;

public interface BeneficiaryService {

    List<BeneficiaryDtos.BeneficiaryResponse> list();

    BeneficiaryDtos.BeneficiaryResponse get(Long id);

    BeneficiaryDtos.BeneficiaryResponse create(BeneficiaryDtos.BeneficiaryRequest request);

    BeneficiaryDtos.BeneficiaryResponse update(Long id, BeneficiaryDtos.BeneficiaryRequest request);

    void delete(Long id);
}
