package com.payhint.api.infrastructure.web.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.payhint.api.application.crm.dto.response.error.ErrorResponse;
import com.payhint.api.application.shared.exceptions.AlreadyExistException;
import com.payhint.api.application.shared.exceptions.NotFoundException;
import com.payhint.api.application.shared.exceptions.PermissionDeniedException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(NotFoundException ex, HttpServletRequest request) {
                logger.warn("Resource not found: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value()).error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                .message(ex.getMessage()).path(request.getRequestURI()).build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                        HttpServletRequest request) {
                logger.warn("Bad credentials: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase()).message("Invalid email or password")
                                .path(request.getRequestURI()).build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(AlreadyExistException.class)
        public ResponseEntity<ErrorResponse> handleAlreadyExist(AlreadyExistException ex, HttpServletRequest request) {
                logger.warn("Resource already exists: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value()).error(HttpStatus.CONFLICT.getReasonPhrase())
                                .message(ex.getMessage()).path(request.getRequestURI()).build();

                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex,
                        HttpServletRequest request) {
                logger.warn("Authentication failed: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase()).message("Authentication failed")
                                .path(request.getRequestURI()).build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                                .map(field -> field.getField() + " " + field.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                logger.warn("Validation error: {}", errorMessage);

                ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value()).error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message(errorMessage).path(request.getRequestURI()).build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                        HttpServletRequest request) {
                logger.warn("Illegal argument: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value()).error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message(ex.getMessage()).path(request.getRequestURI()).build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(PermissionDeniedException.class)
        public ResponseEntity<ErrorResponse> handlePermissionDenied(PermissionDeniedException ex,
                        HttpServletRequest request) {
                logger.warn("Permission denied: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
                                .status(HttpStatus.FORBIDDEN.value()).error(HttpStatus.FORBIDDEN.getReasonPhrase())
                                .message(ex.getMessage()).path(request.getRequestURI()).build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
                logger.error("Unexpected error occurred", ex);

                ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .message("An unexpected error occurred").path(request.getRequestURI()).build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
