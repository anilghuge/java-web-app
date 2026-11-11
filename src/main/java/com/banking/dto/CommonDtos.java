package com.banking.dto;

import java.time.Instant;
import java.util.List;

/** Standard envelope used by every REST endpoint. */
public final class CommonDtos {

    private CommonDtos() {
    }

    public record ApiResponse<T>(
            boolean success,
            String message,
            T data,
            Instant timestamp,
            String errorCode) {

        public static <T> ApiResponse<T> ok(String message, T data) {
            return new ApiResponse<>(true, message, data, Instant.now(), null);
        }

        public static <T> ApiResponse<T> ok(T data) {
            return ok("Request completed successfully", data);
        }

        public static <T> ApiResponse<T> error(String message, String errorCode) {
            return new ApiResponse<>(false, message, null, Instant.now(), errorCode);
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last) {
    }

    public record MessageResponse(String message) {
    }

    public record NameValue(String label, java.math.BigDecimal value) {
    }
}
