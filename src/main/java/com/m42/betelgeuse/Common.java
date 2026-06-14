package com.m42.betelgeuse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

record ApiResponse<T>(boolean success, T data, ApiError error, String correlationId, Instant timestamp) {
    static <T> ApiResponse<T> ok(T data, String correlationId) {
        return new ApiResponse<>(true, data, null, correlationId, Instant.now());
    }

    static <T> ApiResponse<T> failed(String code, String message, String correlationId) {
        return new ApiResponse<>(false, null, new ApiError(code, message), correlationId, Instant.now());
    }

    record ApiError(String code, String message) {
    }
}

class NotFoundException extends RuntimeException {
    NotFoundException(String message) {
        super(message);
    }
}

@org.springframework.stereotype.Component
class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader("X-Correlation-Id");
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        request.setAttribute("correlationId", id);
        response.setHeader("X-Correlation-Id", id);
        MDC.put("correlationId", id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFound(NotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiResponse<Void>> badRequest(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", ex.getMessage(), request);
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiResponse.failed(code, message, (String) request.getAttribute("correlationId")));
    }
}
