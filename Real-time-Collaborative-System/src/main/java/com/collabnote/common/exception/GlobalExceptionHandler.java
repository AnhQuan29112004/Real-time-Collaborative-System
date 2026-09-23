package com.collabnote.common.exception;

import com.collabnote.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ErrorResponseFactory errors;

    public GlobalExceptionHandler(ErrorResponseFactory errors) {
        this.errors = errors;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException ex) {
        return response(ex.getErrorCode());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException ex) {
        return response(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> unauthorized(AuthenticationException ex) {
        return response(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(OptimisticLockingFailureException ex) {
        return response(ErrorCode.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
        // Do not expose exception messages or request values to the client/log by default.
        LOG.error("Unhandled request failure: {}", ex.getClass().getName());
        return response(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode code = switch (status.value()) {
            case 400 -> ErrorCode.INVALID_REQUEST;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_SERVER_ERROR : ErrorCode.REQUEST_REJECTED;
        };
        return new ResponseEntity<>(errors.create(status.value(), code, request.getLocale()), headers, status);
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode code) {
        return ResponseEntity.status(code.getHttpStatus())
                .body(errors.create(code, LocaleContextHolder.getLocale()));
    }
}
