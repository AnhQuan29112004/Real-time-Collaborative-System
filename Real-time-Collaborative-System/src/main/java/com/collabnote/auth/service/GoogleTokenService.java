package com.collabnote.auth.service;
import com.collabnote.auth.dto.GoogleIdentity;
public interface GoogleTokenService { GoogleIdentity verify(String rawToken); }
