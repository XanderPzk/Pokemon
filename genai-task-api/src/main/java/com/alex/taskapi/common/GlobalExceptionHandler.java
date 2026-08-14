package com.alex.taskapi.common;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
    private static final String RESOURCE_CONFLICT_MESSAGE = "Resource conflict";

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleDomainValidation(ValidationException exception) {
        List<FieldErrorResponse> fieldErrors =
                exception.getField() != null
                        ? List.of(
                                new FieldErrorResponse(
                                        exception.getField(), exception.getMessage()))
                        : List.of();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        createWithFieldErrors(
                                HttpStatus.BAD_REQUEST,
                                "BAD_REQUEST",
                                exception.getMessage(),
                                fieldErrors));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(create(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(create(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(
            MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(GlobalExceptionHandler::toFieldErrorResponse)
                        .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        createWithFieldErrors(
                                HttpStatus.BAD_REQUEST,
                                "BAD_REQUEST",
                                "Validation failed",
                                fieldErrors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException exception) {
        List<FieldErrorResponse> fieldErrors =
                exception.getAllValidationResults().stream()
                        .flatMap(
                                result ->
                                        result.getResolvableErrors().stream()
                                                .map(
                                                        error ->
                                                                new FieldErrorResponse(
                                                                        result.getMethodParameter()
                                                                                .getParameterName(),
                                                                        error.getDefaultMessage())))
                        .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        createWithFieldErrors(
                                HttpStatus.BAD_REQUEST,
                                "BAD_REQUEST",
                                "Validation failed",
                                fieldErrors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        String message =
                "Invalid value for parameter '"
                        + exception.getName()
                        + "': "
                        + exception.getValue();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(create(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception) {
        String message = "Malformed JSON request";
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            message = exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(create(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(create(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(
                        create(
                                HttpStatus.METHOD_NOT_ALLOWED,
                                "METHOD_NOT_ALLOWED",
                                "HTTP method not supported"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(
                        create(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                                "UNSUPPORTED_MEDIA_TYPE",
                                "Content type not supported"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception) {
        String message = "Required parameter '" + exception.getParameterName() + "' is not present";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(create(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundEndpoint() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(create(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(create(HttpStatus.CONFLICT, "CONFLICT", RESOURCE_CONFLICT_MESSAGE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        create(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "INTERNAL_SERVER_ERROR",
                                UNEXPECTED_ERROR_MESSAGE));
    }

    private static ErrorResponse create(HttpStatus status, String error, String message) {
        return new ErrorResponse(status.value(), error, message, Instant.now());
    }

    private static ErrorResponse createWithFieldErrors(
            HttpStatus status, String error, String message, List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(status.value(), error, message, Instant.now(), fieldErrors);
    }

    private static FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
