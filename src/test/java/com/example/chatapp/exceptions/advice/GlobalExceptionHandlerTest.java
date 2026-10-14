package com.example.chatapp.exceptions.advice;

import com.example.chatapp.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/chatapp/chatrooms");
    }

    @Test
    @DisplayName("handleNotFoundException returns 404 with error status and message")
    void handleNotFoundException_returns404() {
        NotFoundException ex = new NotFoundException("Chat room 'general' does not exist.");

        ResponseEntity<ApiExceptionResponse> response = exceptionHandler.handleNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("error");
        assertThat(response.getBody().getMessage()).isEqualTo("Chat room 'general' does not exist.");
    }

    @Test
    @DisplayName("handleConflictException returns 409 with error status and message")
    void handleConflictException_returns409() {
        ConflictException ex = new ConflictException("Chat room 'general' already exists.");

        ResponseEntity<ApiExceptionResponse> response = exceptionHandler.handleConflictException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("error");
        assertThat(response.getBody().getMessage()).isEqualTo("Chat room 'general' already exists.");
    }

    @Test
    @DisplayName("handleBadRequestException returns 400 with error status and message")
    void handleBadRequestException_returns400() {
        BadRequestException ex = new BadRequestException("Invalid parameter limit");

        ResponseEntity<ApiExceptionResponse> response = exceptionHandler.handleBadRequestException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("error");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid parameter limit");
    }

    @Test
    @DisplayName("handleValidationException returns 400 with field errors")
    void handleValidationException_returns400WithErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("createRoomRequest", "roomName", "Room name cannot be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiExceptionResponse> response = exceptionHandler.handleValidationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("error");
        assertThat(response.getBody().getMessage()).isEqualTo("Room name cannot be blank");
        assertThat(response.getBody().getErrors()).contains("Room name cannot be blank");
    }

    @Test
    @DisplayName("handleGenericException returns 500 for unexpected errors")
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Redis connection refused");

        ResponseEntity<ApiExceptionResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("error");
        assertThat(response.getBody().getMessage()).contains("Redis connection refused");
    }
}
