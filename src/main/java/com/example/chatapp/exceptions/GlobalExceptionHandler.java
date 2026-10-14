/** Global exception handler intercepting API exceptions and returning consistent error payloads. */
package com.example.chatapp.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle resource not found exceptions (e.g., room does not exist).
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiExceptionResponse> handleNotFoundException(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .status("error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle resource conflict exceptions (e.g., duplicate chat room name).
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiExceptionResponse> handleConflictException(
            ConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource conflict: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .status("error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle bad request exceptions.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiExceptionResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .status("error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle input validation errors (e.g., blank request body fields).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiExceptionResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        String message = errors.isEmpty() ? "Validation failed" : errors.getFirst();
        log.warn("Validation failed: uri={}, errors={}", request.getRequestURI(), errors);

        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .status("error")
                .message(message)
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle static resource or endpoint not found errors gracefully (returns 404).
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiExceptionResponse> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        log.info("Resource not found: uri={}", request.getRequestURI());
        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .status("error")
                .message("Resource not found: " + request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Catch-all handler for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiExceptionResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception: uri={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .status("error")
                .message("An unexpected error occurred: " + ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
