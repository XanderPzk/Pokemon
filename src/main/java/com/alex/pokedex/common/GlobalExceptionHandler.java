package com.alex.pokedex.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
    private static final String RESOURCE_CONFLICT_MESSAGE = "Resource conflict";

    public record FieldError(String field, String message) {}

    public record ErrorResponse(
            int status,
            String error,
            String message,
            Instant timestamp,
            @JsonInclude(JsonInclude.Include.NON_NULL) String correlationId,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> fieldErrors) {

        public ErrorResponse(int status, String error, String message, Instant timestamp) {
            this(status, error, message, timestamp, null, List.of());
        }

        public ErrorResponse(
                int status,
                String error,
                String message,
                Instant timestamp,
                List<FieldError> fieldErrors) {
            this(status, error, message, timestamp, null, fieldErrors);
        }

        public ErrorResponse(
                int status, String error, String message, Instant timestamp, String correlationId) {
            this(status, error, message, timestamp, correlationId, List.of());
        }
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        List<FieldError> fieldErrors =
                exception.getField() != null
                        ? List.of(new FieldError(exception.getField(), exception.getMessage()))
                        : List.of();
        return ResponseEntity.status(exception.getStatus())
                .body(
                        buildErrorResponse(
                                exception.getStatus(),
                                statusError(exception.getStatus()),
                                exception.getMessage(),
                                fieldErrors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        buildErrorResponse(
                                HttpStatus.CONFLICT,
                                "CONFLICT",
                                RESOURCE_CONFLICT_MESSAGE,
                                List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        buildErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "INTERNAL_SERVER_ERROR",
                                UNEXPECTED_ERROR_MESSAGE,
                                List.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        buildErrorResponse(
                                HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", List.of()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        buildErrorResponse(
                                HttpStatus.UNAUTHORIZED,
                                "UNAUTHORIZED",
                                "Authentication required",
                                List.of()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<FieldError> fieldErrors =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(GlobalExceptionHandler::toFieldError)
                        .toList();
        return envelopeResponse(
                headers, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Validation failed", fieldErrors);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<FieldError> fieldErrors =
                exception.getAllValidationResults().stream()
                        .flatMap(
                                result ->
                                        result.getResolvableErrors().stream()
                                                .map(
                                                        error ->
                                                                new FieldError(
                                                                        result.getMethodParameter()
                                                                                .getParameterName(),
                                                                        error.getDefaultMessage())))
                        .toList();
        return envelopeResponse(
                headers, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Validation failed", fieldErrors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = "Malformed JSON request";
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            message = exception.getCause().getMessage();
        }
        return envelopeResponse(headers, HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, List.of());
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
                .body(
                        buildErrorResponse(
                                HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, List.of()));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return envelopeResponse(
                headers,
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "HTTP method not supported",
                List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return envelopeResponse(
                headers,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "Content type not supported",
                List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = "Required parameter '" + exception.getParameterName() + "' is not present";
        return envelopeResponse(headers, HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return envelopeResponse(
                headers, HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return envelopeResponse(
                headers, HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        HttpStatus status = resolveStatus(statusCode);
        String message =
                body instanceof org.springframework.http.ProblemDetail problemDetail
                                && problemDetail.getDetail() != null
                        ? problemDetail.getDetail()
                        : exception.getMessage() != null
                                ? exception.getMessage()
                                : status.getReasonPhrase();
        return envelopeResponse(headers, status, statusError(status), message, List.of());
    }

    private ResponseEntity<Object> envelopeResponse(
            HttpHeaders headers,
            HttpStatus status,
            String error,
            String message,
            List<FieldError> fieldErrors) {
        return new ResponseEntity<>(
                buildErrorResponse(status, error, message, fieldErrors), headers, status);
    }

    private static ErrorResponse buildErrorResponse(
            HttpStatus status, String error, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                status.value(), error, message, Instant.now(), correlationId(), fieldErrors);
    }

    private static String correlationId() {
        return MDC.get(RequestLoggingFilter.MDC_KEY);
    }

    private static FieldError toFieldError(org.springframework.validation.FieldError fieldError) {
        return new FieldError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private static HttpStatus resolveStatus(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String statusError(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case CONFLICT -> "CONFLICT";
            case UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA_TYPE";
            case INTERNAL_SERVER_ERROR -> "INTERNAL_SERVER_ERROR";
            default -> status.name();
        };
    }
}
