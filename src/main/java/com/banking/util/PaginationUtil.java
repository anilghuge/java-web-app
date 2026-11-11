package com.banking.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PaginationUtil {

    private static final Set<String> SAFE_SORT_FIELDS = Set.of(
            "id", "createdAt", "updatedAt", "amount", "balance", "openedAt", "appliedAt",
            "dueDate", "customerNumber", "accountNumber", "reference", "status", "name",
            "joiningDate", "loanNumber", "email", "firstName", "lastName", "action");

    private PaginationUtil() {
    }

    public static Pageable pageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String field = (sortBy == null || !SAFE_SORT_FIELDS.contains(sortBy)) ? "createdAt" : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}
