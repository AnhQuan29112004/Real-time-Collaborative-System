package com.collabnote.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INVALID_CREDENTIALS(401, "error.auth.credentials"),
    EMAIL_NOT_VERIFIED(403, "error.auth.email_unverified"),
    INVALID_REFRESH_TOKEN(401, "error.auth.refresh_invalid"),
    INVALID_ACCOUNT_TOKEN(400, "error.auth.account_token_invalid"),
    AUTH_MAIL_UNAVAILABLE(503, "error.auth.mail_unavailable"),
    GOOGLE_LOGIN_UNAVAILABLE(503, "error.auth.google_unavailable"),
    ACCOUNT_LINK_REQUIRED(409, "error.auth.link_required"),
    LOCAL_PASSWORD_UNAVAILABLE(409, "error.auth.local_password_unavailable"),
    TOO_MANY_REQUESTS(429, "error.auth.rate_limited"),
    INVALID_REQUEST(400, "error.request.invalid"),
    USER_NOT_FOUND(404, "error.user.not_found"),
    EMAIL_DUPLICATED(409, "error.email.duplicated"),
    DOCUMENT_NOT_FOUND(404, "error.document.not_found"),
    UNAUTHORIZED(401, "error.auth.unauthorized"),
    FORBIDDEN(403, "error.auth.forbidden"),
    RESOURCE_NOT_FOUND(404, "error.resource.not_found"),
    CONFLICT(409, "error.resource.conflict"),
    REQUEST_REJECTED(400, "error.request.rejected"),
    INTERNAL_SERVER_ERROR(500, "error.server.internal");

    private final int httpStatus;
    private final String messageKey;

    ErrorCode(int httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}
