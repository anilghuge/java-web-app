package com.banking.service;

import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.CustomerDtos;
import com.banking.enums.KycStatus;

public interface CustomerService {

    CustomerDtos.CustomerResponse create(CustomerDtos.CreateCustomerRequest request);

    PageResponse<CustomerDtos.CustomerResponse> list(String query, KycStatus kycStatus, String city,
                                                     int page, int size, String sortBy, String sortDir);

    CustomerDtos.CustomerResponse get(Long id);

    CustomerDtos.CustomerResponse update(Long id, CustomerDtos.UpdateCustomerRequest request);

    void delete(Long id);

    PageResponse<CustomerDtos.CustomerSummary> search(String query, int page, int size);

    CustomerDtos.CustomerResponse me();

    CustomerDtos.CustomerResponse updateMe(CustomerDtos.UpdateCustomerRequest request);

    CustomerDtos.CustomerResponse submitKyc(CustomerDtos.KycSubmissionRequest request);

    CustomerDtos.CustomerResponse decideKyc(Long customerId, CustomerDtos.KycDecisionRequest request);
}
