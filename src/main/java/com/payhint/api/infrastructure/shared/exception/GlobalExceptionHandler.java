package com.payhint.api.infrastructure.shared.exception;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.payhint.api.application.shared.exception.AlreadyExistsException;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.application.shared.exception.PermissionDeniedException;
import com.payhint.api.domain.shared.exception.DomainException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(NotFoundException.class)
        public ProblemDetail handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
                logger.warn("Resource not found: {}", ex.getMessage());
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
                problemDetail.setTitle("Resource Not Found");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }

        @ExceptionHandler(AlreadyExistsException.class)
        public ProblemDetail handleAlreadyExistsException(AlreadyExistsException ex, HttpServletRequest request) {
                logger.warn("Resource already exists: {}", ex.getMessage());
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
                problemDetail.setTitle("Resource Already Exists");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }

        @ExceptionHandler(PermissionDeniedException.class)
        public ProblemDetail handlePermissionDeniedException(PermissionDeniedException ex, HttpServletRequest request) {
                logger.warn("Permission denied: {}", ex.getMessage());
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
                problemDetail.setTitle("Permission Denied");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }

        @ExceptionHandler(DomainException.class)
        public ProblemDetail handleDomainException(DomainException ex, HttpServletRequest request) {
                logger.warn("Domain exception: {}", ex.getMessage());
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
                problemDetail.setTitle("Business Rule Violation");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
                String errors = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                logger.warn("Validation failed: {}", errors);
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                                "Validation failed");
                problemDetail.setTitle("Invalid Input");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                problemDetail.setProperty("errors", errors);
                return problemDetail;
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
                logger.warn("Authentication failed: Invalid credentials");
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                                "Invalid email or password");
                problemDetail.setTitle("Authentication Failed");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                logger.warn("Malformed JSON request: {}", ex.getMessage());
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                                "Malformed JSON request");
                problemDetail.setTitle("Invalid Request");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
                logger.warn("Invalid argument: {}", ex.getMessage());
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                                "Invalid argument");
                problemDetail.setTitle("Invalid Input");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }

        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ProblemDetail handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex,
                        HttpServletRequest request) {
                logger.warn("Optimistic locking failure: {}", ex.getMessage());
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                                "Conflict occurred due to concurrent modification");
                problemDetail.setTitle("Conflict");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }

        @ExceptionHandler(Exception.class)
        public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
                logger.error("Unexpected error occurred", ex);
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred");
                problemDetail.setTitle("Internal Server Error");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now());
                return problemDetail;
        }
}