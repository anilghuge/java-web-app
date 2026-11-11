package com.banking.service;

import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.LoanDtos;
import com.banking.enums.LoanStatus;

public interface LoanService {

    LoanDtos.LoanResponse apply(LoanDtos.LoanApplicationRequest request);

    PageResponse<LoanDtos.LoanResponse> list(LoanStatus status, int page, int size, String sortBy, String sortDir);

    LoanDtos.LoanResponse get(Long id);

    LoanDtos.LoanResponse approve(Long id, LoanDtos.LoanDecisionRequest request);

    LoanDtos.LoanResponse reject(Long id, LoanDtos.LoanRejectionRequest request);

    LoanDtos.LoanScheduleResponse schedule(Long id);

    LoanDtos.LoanPaymentResponse pay(Long id, LoanDtos.LoanPaymentRequest request);

    LoanDtos.EmiCalculationResponse calculateEmi(LoanDtos.EmiCalculationRequest request);
}
