package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.AccountTokenDelivery;
import com.collabnote.auth.service.AuthMailService;
import com.collabnote.common.config.AuthProperties;
import com.collabnote.common.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthMailServiceImpl implements AuthMailService {
    private final JavaMailSender sender;
    private final AuthProperties properties;
    public void requireConfigured() {
        if (!properties.mailEnabled()) throw new BusinessException(ErrorCode.AUTH_MAIL_UNAVAILABLE);
    }
    public void send(AccountTokenDelivery delivery) {
        requireConfigured();
        boolean verify = delivery.purpose().equals("VERIFY_EMAIL");
        String url = verify ? properties.verificationUrl() : properties.resetUrl();
        // URL fragment keeps the bearer token out of server access logs and Referer headers.
        String link = url + "#token=" + delivery.token();
        var mail = new SimpleMailMessage();
        mail.setFrom(properties.mailFrom()); mail.setTo(delivery.email());
        mail.setSubject(verify ? "Verify your CollabNote email" : "Reset your CollabNote password");
        mail.setText("Open this link to " + (verify ? "verify your email" : "reset your password")
                + ":\n" + link + "\nIf you did not request this, ignore this email.");
        try { sender.send(mail); }
        catch (MailException ex) { throw new BusinessException(ErrorCode.AUTH_MAIL_UNAVAILABLE); }
    }
}
