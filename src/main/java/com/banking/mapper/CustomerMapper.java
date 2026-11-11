package com.banking.mapper;

import com.banking.dto.CustomerDtos;
import com.banking.entity.Customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerDtos.CustomerResponse toResponse(Customer c, long accountCount, java.math.BigDecimal totalBalance) {
        return new CustomerDtos.CustomerResponse(
                c.getId(),
                c.getCustomerNumber(),
                c.getUser().getId(),
                c.getUser().getFirstName(),
                c.getUser().getLastName(),
                c.getUser().getEmail(),
                c.getUser().getPhone(),
                c.getDateOfBirth(),
                c.getGender(),
                c.getAddress(),
                c.getCity(),
                c.getState(),
                c.getPostalCode(),
                c.getCountry(),
                maskNationalId(c.getNationalId()),
                c.getOccupation(),
                c.getAnnualIncome(),
                c.getKycStatus(),
                c.getKycVerifiedAt(),
                accountCount,
                totalBalance,
                c.getCreatedAt());
    }

    public static CustomerDtos.CustomerSummary toSummary(Customer c) {
        return new CustomerDtos.CustomerSummary(
                c.getId(),
                c.getCustomerNumber(),
                c.getUser().getFullName(),
                c.getUser().getEmail(),
                c.getUser().getPhone(),
                c.getKycStatus(),
                c.getCreatedAt());
    }

    private static String maskNationalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "*".repeat(Math.min(trimmed.length() - 4, 8)) + trimmed.substring(trimmed.length() - 4);
    }
}
