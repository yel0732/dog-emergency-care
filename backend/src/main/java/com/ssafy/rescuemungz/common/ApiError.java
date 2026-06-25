package com.ssafy.rescuemungz.common;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        int status,
        String code,
        String message,
        List<FieldError> errors,
        String path,
        LocalDateTime timestamp
) {
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(status, code, message, List.of(), path, LocalDateTime.now());
    }

    public static ApiError validation(String message, List<FieldError> errors, String path) {
        return new ApiError(400, "VALIDATION_FAILED", message, errors, path, LocalDateTime.now());
    }

    public record FieldError(String field, String message) {
    }
}
