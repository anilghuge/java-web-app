package com.banking.service;

import com.banking.dto.AccountDtos;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.entity.Account;
import com.banking.enums.AccountStatus;

import java.util.List;

public interface AccountService {

    AccountDtos.AccountResponse open(AccountDtos.OpenAccountRequest request);

    PageResponse<AccountDtos.AccountResponse> list(AccountStatus status, String query, int page, int size,
                                                   String sortBy, String sortDir);

    AccountDtos.AccountResponse get(Long id);

    PageResponse<AccountDtos.AccountResponse> byCustomer(Long customerId, int page, int size);

    AccountDtos.AccountResponse updateStatus(Long id, AccountDtos.UpdateAccountStatusRequest request);

    AccountDtos.AccountBalanceResponse balance(Long id);

    List<AccountDtos.AccountTypeResponse> accountTypes();

    List<AccountDtos.BranchOption> branches();

    Account requireAccessibleAccount(String accountNumber);

    AccountDtos.AccountResponse toResponse(Account account);
}
