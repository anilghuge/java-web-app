package com.banking.mapper;

import com.banking.dto.AccountDtos;
import com.banking.entity.Account;
import com.banking.entity.AccountType;
import com.banking.entity.Branch;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountDtos.AccountResponse toResponse(Account a) {
        return new AccountDtos.AccountResponse(
                a.getId(),
                a.getAccountNumber(),
                a.getCustomer().getId(),
                a.getCustomer().getUser().getFullName(),
                a.getCustomer().getCustomerNumber(),
                a.getAccountType().getCode(),
                a.getAccountType().getName(),
                a.getBranch().getCode(),
                a.getBranch().getName(),
                a.getBranch().getIfscCode(),
                a.getBalance(),
                a.getAvailableBalance(),
                a.getCurrency(),
                a.getStatus(),
                a.getNickname(),
                a.getInterestRate(),
                a.getOpenedAt());
    }

    public static AccountDtos.AccountSummary toSummary(Account a) {
        return new AccountDtos.AccountSummary(
                a.getId(),
                a.getAccountNumber(),
                a.getAccountType().getName(),
                a.getBalance(),
                a.getCurrency(),
                a.getStatus(),
                a.getNickname());
    }

    public static AccountDtos.AccountTypeResponse toTypeResponse(AccountType t) {
        return new AccountDtos.AccountTypeResponse(
                t.getId(), t.getCode(), t.getName(), t.getDescription(),
                t.getInterestRate(), t.getMinimumBalance(), t.getDailyWithdrawalLimit());
    }

    public static AccountDtos.BranchOption toBranchOption(Branch b) {
        return new AccountDtos.BranchOption(
                b.getId(), b.getCode(), b.getName(), b.getCity(), b.getIfscCode());
    }
}
