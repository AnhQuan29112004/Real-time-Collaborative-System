package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.*;
import com.collabnote.auth.dto.request.*;
import com.collabnote.auth.service.*;
import com.collabnote.common.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final CredentialService credentials;
    private final AccountTokenService tokens;
    private final AuthMailService mail;

    public void register(RegisterRequest request) {
        mail.requireConfigured();
        try { credentials.register(request); }
        catch (DataIntegrityViolationException ex) { throw new BusinessException(ErrorCode.EMAIL_DUPLICATED); }
        requestVerification(request.email());
    }
    public IssuedTokens login(LoginRequest request) { return credentials.login(request); }
    public void requestVerification(String email) { send(email, "VERIFY_EMAIL"); }
    public void forgotPassword(String email) { send(email, "RESET_PASSWORD"); }
    public void verifyEmail(String token) { tokens.verifyEmail(token); }
    public void resetPassword(ResetPasswordRequest request) { tokens.resetPassword(request.token(), request.newPassword()); }
    public void changePassword(Long userId, ChangePasswordRequest request) { credentials.changePassword(userId, request); }

    private void send(String email, String purpose) {
        mail.requireConfigured();
        // Token issuance commits before SMTP. Delivery failures can be retried via resend after cooldown.
        tokens.issue(email, purpose).ifPresent(delivery -> {
            try { mail.send(delivery); }
            catch (BusinessException ex) {
                if (ex.getErrorCode() != ErrorCode.AUTH_MAIL_UNAVAILABLE) throw ex;
                // Keep public resend/reset responses independent of whether an email exists.
                log.warn("Auth email delivery failed; purpose={}", purpose);
            }
        });
    }
}
