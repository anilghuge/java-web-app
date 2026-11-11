package com.banking.mapper;

import com.banking.dto.LoanDtos;
import com.banking.entity.Loan;
import com.banking.entity.LoanPayment;

import java.util.concurrent.atomic.AtomicInteger;

public final class LoanMapper {

    private LoanMapper() {
    }

    public static LoanDtos.LoanResponse toResponse(Loan l) {
        long remaining = l.getStatus() == com.banking.enums.LoanStatus.ACTIVE
                ? Math.max(0, l.getTermMonths() - l.getPaidInstallments())
                : (l.getStatus() == com.banking.enums.LoanStatus.CLOSED ? 0 : l.getTermMonths());
        return new LoanDtos.LoanResponse(
                l.getId(),
                l.getLoanNumber(),
                l.getCustomer().getId(),
                l.getCustomer().getUser().getFullName(),
                l.getDisbursementAccount() != null ? l.getDisbursementAccount().getAccountNumber() : null,
                l.getLoanType(),
                l.getPrincipal(),
                l.getAnnualInterestRate(),
                l.getTermMonths(),
                l.getEmi(),
                l.getTotalPayable(),
                l.getOutstandingBalance(),
                l.getStatus(),
                l.getPurpose(),
                l.getAppliedAt(),
                l.getApprovedAt(),
                l.getApprovedBy(),
                l.getRejectionReason(),
                l.getNextDueDate(),
                l.getPaidInstallments(),
                remaining,
                l.getCreatedAt());
    }

    public static java.util.List<LoanDtos.LoanScheduleItem> toSchedule(java.util.List<LoanPayment> payments) {
        AtomicInteger counter = new AtomicInteger(1);
        return payments.stream()
                .map(p -> new LoanDtos.LoanScheduleItem(
                        p.getId(),
                        counter.getAndIncrement(),
                        p.getDueDate(),
                        p.getAmount(),
                        p.getPrincipalComponent(),
                        p.getInterestComponent(),
                        p.getStatus(),
                        p.getPaidAt(),
                        p.getTransactionReference()))
                .toList();
    }
}
