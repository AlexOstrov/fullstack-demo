package com.example.backend.error;

import java.time.Instant;

/** Единый формат ошибки API (ТЗ 5.3). */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
