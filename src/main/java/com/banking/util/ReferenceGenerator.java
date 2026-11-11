package com.banking.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ReferenceGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private ReferenceGenerator() {
    }

    public static String transactionReference() {
        return "TXN" + DAY.format(Instant.now()) + randomDigits(10);
    }

    public static String transactionGroup() {
        return "GRP" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    public static String accountNumber() {
        return "10" + randomDigits(10);
    }

    public static String customerNumber() {
        return "CUS" + randomDigits(9);
    }

    public static String loanNumber() {
        return "LN" + DAY.format(Instant.now()) + randomDigits(6);
    }

    public static String employeeCode() {
        return "EMP" + randomDigits(6);
    }

    public static String cardReference() {
        return "CARD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    public static String lastFour() {
        return randomDigits(4);
    }

    public static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String refreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
