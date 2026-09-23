package com.collabnote.common.exception;

import com.collabnote.common.response.ApiResponse;
import com.collabnote.common.web.RequestIdFilter;
import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseFactory {
    private final MessageSource messages;

    public ErrorResponseFactory(MessageSource messages) {
        this.messages = messages;
    }

    public ApiResponse<Void> create(ErrorCode code, Locale locale) {
        return create(code.getHttpStatus(), code, locale);
    }

    public ApiResponse<Void> create(int status, ErrorCode code, Locale locale) {
        return ApiResponse.error(status, code.name(),
                messages.getMessage(code.getMessageKey(), null, locale), MDC.get(RequestIdFilter.MDC_KEY));
    }
}
