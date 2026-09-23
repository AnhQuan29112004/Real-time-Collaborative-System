package com.collabnote.auth.service;
import com.collabnote.auth.dto.AccountTokenDelivery;
public interface AuthMailService {
    void requireConfigured();
    void send(AccountTokenDelivery delivery);
}
