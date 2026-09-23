package com.collabnote.auth.service;
import com.collabnote.auth.dto.AccountTokenDelivery;
import java.util.Optional;
public interface AccountTokenService {
    Optional<AccountTokenDelivery> issue(String email, String purpose);
    void verifyEmail(String token);
    void resetPassword(String token, String newPassword);
}
