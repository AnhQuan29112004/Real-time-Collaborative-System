package com.collabnote.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "error.user.not_found"),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT.value(), "error.email.duplicated"),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "error.document.not_found"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "error.auth.unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN.value(), "error.auth.forbidden"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "error.server.internal");

    private final int httpStatus;
    private final String messageKey;

    ErrorCode(int httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}
