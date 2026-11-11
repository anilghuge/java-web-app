package com.banking.service.impl;

import com.banking.dto.BeneficiaryDtos;
import com.banking.entity.Beneficiary;
import com.banking.entity.Customer;
import com.banking.exception.ApiException;
import com.banking.mapper.BeneficiaryMapper;
import com.banking.repository.AccountRepository;
import com.banking.repository.BeneficiaryRepository;
import com.banking.service.AuditService;
import com.banking.service.AuthenticatedUserService;
import com.banking.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryDtos.BeneficiaryResponse> list() {
        Customer customer = authenticatedUserService.customer();
        return beneficiaryRepository.findByCustomerIdAndDeletedFalseOrderByNameAsc(customer.getId())
                .stream().map(BeneficiaryMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryDtos.BeneficiaryResponse get(Long id) {
        Customer customer = authenticatedUserService.customer();
        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndCustomerIdAndDeletedFalse(id, customer.getId())
                .orElseThrow(() -> ApiException.notFound("BENEFICIARY_NOT_FOUND", "Beneficiary not found"));
        return BeneficiaryMapper.toResponse(beneficiary);
    }

    @Override
    @Transactional
    public BeneficiaryDtos.BeneficiaryResponse create(BeneficiaryDtos.BeneficiaryRequest request) {
        Customer customer = authenticatedUserService.customer();
        validateBeneficiaryAccount(customer, request.accountNumber());
        if (beneficiaryRepository.existsByCustomerIdAndAccountNumberAndDeletedFalse(
                customer.getId(), request.accountNumber())) {
            throw ApiException.conflict("BENEFICIARY_EXISTS", "This beneficiary account is already saved");
        }
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setCustomer(customer);
        apply(beneficiary, request);
        beneficiaryRepository.save(beneficiary);
        auditService.log("BENEFICIARY_CREATED", "Beneficiary", beneficiary.getId(),
                "Beneficiary " + beneficiary.getName() + " added");
        return BeneficiaryMapper.toResponse(beneficiary);
    }

    @Override
    @Transactional
    public BeneficiaryDtos.BeneficiaryResponse update(Long id, BeneficiaryDtos.BeneficiaryRequest request) {
        Customer customer = authenticatedUserService.customer();
        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndCustomerIdAndDeletedFalse(id, customer.getId())
                .orElseThrow(() -> ApiException.notFound("BENEFICIARY_NOT_FOUND", "Beneficiary not found"));
        validateBeneficiaryAccount(customer, request.accountNumber());
        boolean duplicate = beneficiaryRepository
                .existsByCustomerIdAndAccountNumberAndDeletedFalse(customer.getId(), request.accountNumber())
                && !beneficiary.getAccountNumber().equals(request.accountNumber());
        if (duplicate) {
            throw ApiException.conflict("BENEFICIARY_EXISTS", "This beneficiary account is already saved");
        }
        apply(beneficiary, request);
        beneficiaryRepository.save(beneficiary);
        auditService.log("BENEFICIARY_UPDATED", "Beneficiary", beneficiary.getId(),
                "Beneficiary " + beneficiary.getName() + " updated");
        return BeneficiaryMapper.toResponse(beneficiary);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = authenticatedUserService.customer();
        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndCustomerIdAndDeletedFalse(id, customer.getId())
                .orElseThrow(() -> ApiException.notFound("BENEFICIARY_NOT_FOUND", "Beneficiary not found"));
        beneficiary.setDeleted(true);
        beneficiary.setActive(false);
        beneficiaryRepository.save(beneficiary);
        auditService.log("BENEFICIARY_DELETED", "Beneficiary", beneficiary.getId(),
                "Beneficiary " + beneficiary.getName() + " removed");
    }

    private void apply(Beneficiary beneficiary, BeneficiaryDtos.BeneficiaryRequest request) {
        beneficiary.setName(request.name().trim());
        beneficiary.setAccountNumber(request.accountNumber().trim());
        beneficiary.setBankName(request.bankName().trim());
        beneficiary.setBranchName(request.branchName());
        beneficiary.setIfscCode(request.ifscCode().trim().toUpperCase());
        beneficiary.setNickname(request.nickname());
        beneficiary.setDailyLimit(request.dailyLimit() == null ? new BigDecimal("200000") : request.dailyLimit());
        beneficiary.setActive(true);
    }

    private void validateBeneficiaryAccount(Customer customer, String accountNumber) {
        accountRepository.findByAccountNumberAndDeletedFalse(accountNumber).ifPresent(account -> {
            if (account.getCustomer().getId().equals(customer.getId())) {
                throw ApiException.badRequest("SELF_BENEFICIARY_NOT_ALLOWED",
                        "You cannot add your own account as a beneficiary");
            }
        });
    }
}
