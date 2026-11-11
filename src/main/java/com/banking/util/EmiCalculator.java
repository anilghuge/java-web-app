package com.banking.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class EmiCalculator {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private EmiCalculator() {
    }

    public static BigDecimal monthlyEmi(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        if (months <= 0) {
            throw new IllegalArgumentException("Term must be positive");
        }
        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(1200), 12, RoundingMode.HALF_UP);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal factor = onePlusR.pow(months, MC);
        BigDecimal emi = principal.multiply(monthlyRate).multiply(factor, MC)
                .divide(factor.subtract(BigDecimal.ONE), MC);
        return emi.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal totalPayable(BigDecimal emi, int months) {
        return emi.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal totalInterest(BigDecimal totalPayable, BigDecimal principal) {
        return totalPayable.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }
}
