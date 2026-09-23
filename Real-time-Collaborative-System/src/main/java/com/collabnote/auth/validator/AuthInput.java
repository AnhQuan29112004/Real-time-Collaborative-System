package com.collabnote.auth.validator;
import com.collabnote.common.exception.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
public final class AuthInput {
    private AuthInput() { }
    public static String email(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    public static void password(String value) {
        if (value.length() < 12 || value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
}
