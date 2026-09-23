package com.collabnote.auth.dto;
/** Internal mail delivery data, never serialized by a controller. */
public record AccountTokenDelivery(String email, String token, String purpose) {
    @Override public String toString() { return "AccountTokenDelivery[redacted]"; }
}
